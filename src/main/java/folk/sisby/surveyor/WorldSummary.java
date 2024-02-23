package folk.sisby.surveyor;

import folk.sisby.surveyor.chunk.ChunkSummary;
import folk.sisby.surveyor.chunk.RegionSummary;
import folk.sisby.surveyor.structure.StructureSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.util.ChunkUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WorldSummary {
    public static final String SERVERS_FILE_NAME = "servers.txt";
    public static final String DATA_SUBFOLDER = "data";

    public enum Type {
        SERVER,
        CLIENT
    }
    public final Type type;

    protected final Map<ChunkPos, RegionSummary> regions;
    protected final WorldStructureSummary structures;
    protected final DynamicRegistryManager manager;

    public static ChunkPos getRegionPos(ChunkPos pos) {
        return new ChunkPos(pos.x >> RegionSummary.REGION_POWER, pos.z >> RegionSummary.REGION_POWER);
    }

    public WorldSummary(Type type, Map<ChunkPos, RegionSummary> regions, WorldStructureSummary structures, DynamicRegistryManager manager) {
        this.type = type;
        this.regions = regions;
        this.structures = structures;
        this.manager = manager;
    }

    public boolean containsChunk(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.containsKey(regionPos) && regions.get(regionPos).contains(pos);
    }

    public boolean containsStructure(World world, StructureStart start) {
        return structures.contains(world, start);
    }

    public Collection<StructureSummary> getStructures() {
        return structures.getStructures();
    }

    public ChunkSummary getChunk(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.get(regionPos).get(pos);
    }

    public Collection<ChunkPos> getChunks() {
        Collection<ChunkPos> chunkPosCollection = new ArrayList<>();
        regions.forEach((p, r) -> chunkPosCollection.addAll(r.getChunks(p)));
        return chunkPosCollection;
    }

    public IndexedIterable<Biome> getBiomePalette(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.get(regionPos).getBiomePalette();
    }

    public IndexedIterable<Block> getBlockPalette(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.get(regionPos).getBlockPalette();
    }

    public void putChunk(World world, Chunk chunk) {
        regions.computeIfAbsent(getRegionPos(chunk.getPos()), k -> new RegionSummary(type)).putChunk(world, chunk);
    }

    private void putStructure(World world, StructureStart start) {
        structures.putStructure(world, start);
    }

    public static File getClientDirectory(ClientWorld world) {
        ServerInfo info = MinecraftClient.getInstance().getCurrentServerEntry();
        String saveFolder = String.valueOf(world.getBiomeAccess().seed);
        String dimNamespace = world.getRegistryKey().getValue().getNamespace();
        String dimPath = world.getRegistryKey().getValue().getPath();
        Path savePath = FabricLoader.getInstance().getGameDir().resolve(DATA_SUBFOLDER).resolve(Surveyor.ID).resolve(saveFolder);
        savePath.toFile().mkdirs();
        File serversFile = savePath.resolve(SERVERS_FILE_NAME).toFile();
        try {
            if (!serversFile.exists() || !FileUtils.readFileToString(serversFile, StandardCharsets.UTF_8).contains(info.name + "\n" + info.address)) {
                FileUtils.writeStringToFile(serversFile, info.name + "\n" + info.address + "\n", StandardCharsets.UTF_8, true);
            }
        } catch (IOException e) {
            Surveyor.LOGGER.error("[Surveyor] Error writing servers file for save {}.", savePath, e);
        }
        return savePath.resolve(dimNamespace).resolve(dimPath).toFile();
    }

    public void save(World world, File folder) {
        Surveyor.LOGGER.info("[Surveyor] Saving summaries for {}", world.getRegistryKey().getValue());
        folder.mkdirs();
        regions.forEach((pos, summary) -> {
            if (!summary.isDirty()) return;
            NbtCompound regionCompound = summary.writeNbt(manager, new NbtCompound(), pos);
            File regionFile = new File(folder, "c.%d.%d.dat".formatted(pos.x, pos.z));
            try {
                NbtIo.writeCompressed(regionCompound, regionFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing region summary file {}.", regionFile.getName(), e);
            }
        });
        if (structures.isDirty()) {
            File structureFile = new File(folder, "structures.dat");
            try {
                NbtIo.writeCompressed(structures.writeNbt(new NbtCompound()), structureFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing structure summary file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
        Surveyor.LOGGER.info("[Surveyor] Finished saving data for {}", world.getRegistryKey().getValue());
    }

    public void save(ServerWorld world) {
        if (type != Type.SERVER) return;
        save(world, new File(world.getPersistentStateManager().directory, Surveyor.ID));
    }

    public void save(ClientWorld world) {
        if (type != Type.CLIENT) return;
        save(world, getClientDirectory(world));
    }

    public static WorldSummary load(Type type, World world, File folder) {
        folder.mkdirs();
        File[] chunkFiles = folder.listFiles((file, name) -> {
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
        Map<ChunkPos, RegionSummary> regions;
        regions = new HashMap<>();
        if (chunkFiles != null) {
            for (File regionFile : chunkFiles) {
                ChunkPos regionPos = new ChunkPos(Integer.parseInt(regionFile.getName().split("\\.")[1]), Integer.parseInt(regionFile.getName().split("\\.")[2]));
                NbtCompound regionCompound = null;
                try {
                    regionCompound = NbtIo.readCompressed(regionFile);
                } catch (IOException e) {
                    Surveyor.LOGGER.error("[Surveyor] Error loading region summary file {}.", regionFile.getName(), e);
                }
                if (regionCompound != null) regions.put(regionPos, new RegionSummary(type).readNbt(regionCompound, world.getRegistryManager()));
            }
        }
        NbtCompound structureNbt = new NbtCompound();
        File structureFile = new File(folder, "structures.dat");
        if (structureFile.exists()) {
            try {
                structureNbt = NbtIo.readCompressed(structureFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error loading structure summary file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
        WorldStructureSummary structures = WorldStructureSummary.readNbt(structureNbt);
        return new WorldSummary(type, regions, structures, world.getRegistryManager());
    }

    public static WorldSummary load(ServerWorld world) {
        return load(Type.SERVER, world, new File(world.getPersistentStateManager().directory, Surveyor.ID));
    }

    public static WorldSummary load(ClientWorld world) {
        return load(Type.CLIENT, world, getClientDirectory(world));
    }

    public static void onChunkLoad(World world, Chunk chunk) {
        Type type = world instanceof ServerWorld ? Type.SERVER : Type.CLIENT;
        WorldSummary state = ((SurveyorWorld) world).surveyor$getWorldSummary();
        if (state.type == type && (!state.containsChunk(chunk.getPos()) || !ChunkUtil.airCount(chunk).equals(state.getChunk(chunk.getPos()).getAirCount()))) state.putChunk(world, chunk);
        chunk.getStructureStarts().forEach((structure, start) -> {
            if (!state.containsStructure(world, start)) state.putStructure(world, start);
        });
    }

    public static void onChunkUnload(World world, WorldChunk chunk) {
        Type type = world instanceof ServerWorld ? Type.SERVER : Type.CLIENT;
        WorldSummary state = ((SurveyorWorld) world).surveyor$getWorldSummary();
        if (state.type == type && chunk.needsSaving()) state.putChunk(world, chunk);
    }

    public static void onStructurePlace(World world, StructureStart start) {
        WorldSummary state = ((SurveyorWorld) world).surveyor$getWorldSummary();
        if (!state.containsStructure(world, start)) state.putStructure(world, start);
    }
}
