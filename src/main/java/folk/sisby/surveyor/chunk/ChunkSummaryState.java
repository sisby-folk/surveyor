package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorWorld;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChunkSummaryState {
    public static final String KEY_BIOMES = "biomes";
    public static final String KEY_BLOCKS = "blocks";
    public static final String KEY_BIOME_WATER = "biomeWater";
    public static final String KEY_BIOME_FOLIAGE = "biomeFoliage";
    public static final String KEY_BIOME_GRASS = "biomeGrass";
    public static final String KEY_BLOCK_COLORS = "blockColors";
    public static final String KEY_CHUNKS = "chunks";

    private final Map<ChunkPos, ChunkSummary> chunks;
    private final DynamicRegistryManager manager;

    private boolean dirty = false;

    public ChunkSummaryState(Map<ChunkPos, ChunkSummary> chunks, DynamicRegistryManager manager) {
        this.chunks = chunks;
        this.manager = manager;
    }

    public boolean contains(Chunk chunk) {
        return chunks.containsKey(chunk.getPos());
    }

    public void putChunk(World world, Chunk chunk) {
        chunks.put(chunk.getPos(), new ChunkSummary(world, chunk, DimensionSupport.getSummaryLayers(world)));
        dirty = true;
    }

    public static NbtCompound writeNbt(Map<ChunkPos, ChunkSummary> chunks, DynamicRegistryManager manager, NbtCompound nbt) {
        List<Biome> biomePalette = chunks.values().stream().flatMap(summary -> summary.layers.values().stream()).flatMap(Arrays::stream).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::biome).distinct().sorted(Comparator.comparingInt(b -> manager.get(RegistryKeys.BIOME).getRawId(b))).toList();
        List<Block> blockPalette = chunks.values().stream().flatMap(summary -> summary.layers.values().stream()).flatMap(Arrays::stream).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::block).distinct().sorted(Comparator.comparingInt(b -> manager.get(RegistryKeys.BLOCK).getRawId(b))).toList();
        nbt.put(KEY_BIOMES, new NbtList(biomePalette.stream().map(b -> (NbtElement) NbtString.of(manager.get(RegistryKeys.BIOME).getId(b).toString())).toList(), NbtElement.STRING_TYPE));
        nbt.put(KEY_BLOCKS, new NbtList(blockPalette.stream().map(b -> (NbtElement) NbtString.of(manager.get(RegistryKeys.BLOCK).getId(b).toString())).toList(), NbtElement.STRING_TYPE));
        nbt.putIntArray(KEY_BIOME_WATER, biomePalette.stream().mapToInt(Biome::getWaterColor).toArray());
        nbt.putIntArray(KEY_BIOME_FOLIAGE, biomePalette.stream().mapToInt(Biome::getFoliageColor).toArray());
        nbt.putIntArray(KEY_BIOME_GRASS, biomePalette.stream().mapToInt(b -> b.getGrassColorAt(0, 0)).toArray());
        nbt.putIntArray(KEY_BLOCK_COLORS, blockPalette.stream().map(AbstractBlock::getDefaultMapColor).mapToInt(c -> c.color).toArray());
        NbtCompound chunksCompound = new NbtCompound();
        chunks.forEach((pos, summary) -> chunksCompound.put("%s,%s".formatted(pos.x, pos.z), summary.writeNbt(new NbtCompound(), biomePalette, blockPalette)));
        nbt.put(KEY_CHUNKS, chunksCompound);
        return nbt;
    }

    public void save(ServerWorld world) {
        Map<ChunkPos, Map<ChunkPos, ChunkSummary>> regions = new HashMap<>();
        chunks.forEach(((pos, summary) -> regions.computeIfAbsent(new ChunkPos(pos.getRegionX(), pos.getRegionZ()), k -> new HashMap<>()).put(pos, summary)));
        File dataFolder = world.getPersistentStateManager().directory;
        File surveyorFolder = new File(dataFolder, Surveyor.ID);
        surveyorFolder.mkdirs();
        regions.forEach((pos, chunks) -> {
            NbtCompound regionCompound = writeNbt(chunks, manager, new NbtCompound());
            File regionFile = new File(surveyorFolder, "c.%d.%d.dat".formatted(pos.x, pos.z));
            try {
                NbtIo.writeCompressed(regionCompound, regionFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("Error writing region summary file {}.", regionFile.getName(), e);
            }
        });
    }

    public static void readNbt(Map<ChunkPos, ChunkSummary> chunks, NbtCompound nbt, DynamicRegistryManager manager) {
        List<Biome> biomePalette =  nbt.getList(KEY_BIOMES, NbtElement.STRING_TYPE).stream().map(e -> manager.get(RegistryKeys.BIOME).get(new Identifier(e.asString()))).toList();
        List<Block> blockPalette =  nbt.getList(KEY_BLOCKS, NbtElement.STRING_TYPE).stream().map(e -> manager.get(RegistryKeys.BLOCK).get(new Identifier(e.asString()))).toList();
        NbtCompound chunksCompound = nbt.getCompound(KEY_CHUNKS);
        for (String posKey : chunksCompound.getKeys()) {
            int x = Integer.parseInt(posKey.split(",")[0]);
            int z = Integer.parseInt(posKey.split(",")[1]);
            chunks.put(
                new ChunkPos(x, z),
                new ChunkSummary(chunksCompound.getCompound(posKey), biomePalette, blockPalette)
            );
        }
    }

    public static ChunkSummaryState readNbt(Map<ChunkPos, NbtCompound> regions, DynamicRegistryManager manager) {
        Map<ChunkPos, ChunkSummary> chunks = new HashMap<>();
        regions.forEach((pos, nbt) -> readNbt(chunks, nbt, manager));
        return new ChunkSummaryState(chunks, manager);
    }

    public static ChunkSummaryState load(ServerWorld world) {
        File dataFolder = world.getPersistentStateManager().directory;
        File surveyorFolder = new File(dataFolder, Surveyor.ID);
        surveyorFolder.mkdirs();
        File[] chunkFiles = surveyorFolder.listFiles((file, name) -> {
            String[] split = name.split("\\.");
            if (split.length == 4 && split[0].equals("c") && split[3].equals("dat")) {
                try {
                    Integer.parseInt(split[1]);
                    Integer.parseInt(split[2]);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        });
        Map<ChunkPos, NbtCompound> regions = new HashMap<>();
        if (chunkFiles != null) {
            for (File regionFile : chunkFiles) {
                ChunkPos regionPos = new ChunkPos(Integer.parseInt(regionFile.getName().split("\\.")[1]), Integer.parseInt(regionFile.getName().split("\\.")[2]));
                try {
                    regions.put(regionPos, NbtIo.readCompressed(regionFile));
                } catch (IOException e) {
                    Surveyor.LOGGER.error("Error loading region summary file {}.", regionFile.getName(), e);
                }
            }
        }
        return ChunkSummaryState.readNbt(regions, world.getRegistryManager());
    }

    public static void onChunkLoad(ServerWorld world, Chunk chunk) {
        ChunkSummaryState state = ((SurveyorWorld) world).surveyor$getChunkSummaryState();
        if (!state.contains(chunk)) state.putChunk(world, chunk);
    }

    public static void onChunkUnload(ServerWorld world, WorldChunk chunk) {
        ChunkSummaryState state = ((SurveyorWorld) world).surveyor$getChunkSummaryState();
        if (chunk.needsSaving()) state.putChunk(world, chunk);
    }
}
