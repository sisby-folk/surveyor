package folk.sisby.surveyor.chunk;

import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RegionSummary {
    public static final int REGION_POWER = 5;
    public static final int REGION_SIZE = 2 << REGION_POWER;
    public static final String KEY_BIOMES = "biomes";
    public static final String KEY_BLOCKS = "blocks";
    public static final String KEY_BIOME_WATER = "biomeWater";
    public static final String KEY_BIOME_FOLIAGE = "biomeFoliage";
    public static final String KEY_BIOME_GRASS = "biomeGrass";
    public static final String KEY_BLOCK_COLORS = "blockColors";
    public static final String KEY_CHUNKS = "chunks";

    private final Int2ObjectBiMap<Block> blockPalette = Int2ObjectBiMap.create(255);
    private final Int2ObjectBiMap<Biome> biomePalette = Int2ObjectBiMap.create(255);
    ChunkSummary[][] chunks = new ChunkSummary[REGION_SIZE][REGION_SIZE];

    private boolean dirty = false;

    public static <T, O> List<O> mapPalette(IndexedIterable<T> palette, Function<T, O> mapper) {
        List<O> list = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            list.add(mapper.apply(palette.get(i)));
        }
        return list;
    }

    public static int regionRelative(int xz) {
        return xz & (RegionSummary.REGION_SIZE - 1);
    }

    public boolean contains(Chunk chunk) {
        return chunks[regionRelative(chunk.getPos().x)][regionRelative(chunk.getPos().z)] != null;
    }

    public void putChunk(World world, Chunk chunk) {
        chunks[regionRelative(chunk.getPos().x)][regionRelative(chunk.getPos().z)] = new ChunkSummary(world, chunk, DimensionSupport.getSummaryLayers(world));
        dirty = true;
    }

    public RegionSummary readNbt(NbtCompound nbt, DynamicRegistryManager manager) {
        nbt.getList(KEY_BIOMES, NbtElement.STRING_TYPE).stream().map(e -> manager.get(RegistryKeys.BIOME).get(new Identifier(e.asString()))).forEach(biomePalette::add);
        nbt.getList(KEY_BLOCKS, NbtElement.STRING_TYPE).stream().map(e -> manager.get(RegistryKeys.BLOCK).get(new Identifier(e.asString()))).forEach(blockPalette::add);
        NbtCompound chunksCompound = nbt.getCompound(KEY_CHUNKS);
        for (String posKey : chunksCompound.getKeys()) {
            int x = regionRelative(Integer.parseInt(posKey.split(",")[0]));
            int z = regionRelative(Integer.parseInt(posKey.split(",")[1]));
            chunks[x][z] = new ChunkSummary(chunksCompound.getCompound(posKey), biomePalette, blockPalette);
        }
        return this;
    }

    public NbtCompound writeNbt(DynamicRegistryManager manager, NbtCompound nbt, ChunkPos regionPos) {
        nbt.put(KEY_BIOMES, new NbtList(mapPalette(biomePalette, b -> NbtString.of(manager.get(RegistryKeys.BIOME).getId(b).toString())), NbtElement.STRING_TYPE));
        nbt.put(KEY_BLOCKS, new NbtList(mapPalette(blockPalette, b -> NbtString.of(manager.get(RegistryKeys.BLOCK).getId(b).toString())), NbtElement.STRING_TYPE));
        nbt.putIntArray(KEY_BIOME_WATER, mapPalette(biomePalette, Biome::getWaterColor));
        nbt.putIntArray(KEY_BIOME_FOLIAGE, mapPalette(biomePalette, Biome::getFoliageColor));
        nbt.putIntArray(KEY_BIOME_GRASS, mapPalette(biomePalette, b -> b.getGrassColorAt(0, 0)));
        nbt.putIntArray(KEY_BLOCK_COLORS, mapPalette(blockPalette, b -> b.getDefaultMapColor().color));
        NbtCompound chunksCompound = new NbtCompound();
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                if (chunks[x][z] != null) chunksCompound.put("%s,%s".formatted((regionPos.x << REGION_POWER) + x, (regionPos.z << REGION_POWER) + z), chunks[x][z].writeNbt(new NbtCompound(), biomePalette, blockPalette));
            }
        }
        nbt.put(KEY_CHUNKS, chunksCompound);
        return nbt;
    }

    public boolean isDirty() {
        return dirty;
    }
}
