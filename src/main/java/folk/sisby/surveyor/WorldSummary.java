package folk.sisby.surveyor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.chunk.ChunkSummary;
import folk.sisby.surveyor.chunk.RegionSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.Landmarks;
import folk.sisby.surveyor.packet.c2s.OnLandmarkAddedC2SPacket;
import folk.sisby.surveyor.packet.c2s.OnLandmarkRemovedC2SPacket;
import folk.sisby.surveyor.packet.s2c.OnLandmarkAddedS2CPacket;
import folk.sisby.surveyor.packet.s2c.OnLandmarkRemovedS2CPacket;
import folk.sisby.surveyor.packet.s2c.OnStructureAddedS2CPacket;
import folk.sisby.surveyor.structure.StructurePieceSummary;
import folk.sisby.surveyor.structure.StructureSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.util.ChunkUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WorldSummary {
    public enum Type {
        SERVER,
        CLIENT
    }

    public final Type type;
    protected final Map<ChunkPos, RegionSummary> regions;
    protected final WorldStructureSummary structures;
    protected final Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks;

    protected boolean landmarksDirty = true;

    protected final DynamicRegistryManager manager;

    public static ChunkPos getRegionPos(ChunkPos pos) {
        return new ChunkPos(pos.x >> RegionSummary.REGION_POWER, pos.z >> RegionSummary.REGION_POWER);
    }

    public WorldSummary(Type type, Map<ChunkPos, RegionSummary> regions, WorldStructureSummary structures, Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks, DynamicRegistryManager manager) {
        this.type = type;
        this.regions = regions;
        this.structures = structures;
        this.landmarks = landmarks;
        this.manager = manager;
    }

    public boolean containsChunk(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.containsKey(regionPos) && regions.get(regionPos).contains(pos);
    }

    public boolean containsStructure(World world, StructureStart start) {
        return structures.contains(world, start);
    }

    public boolean containsStructure(ChunkPos pos, RegistryKey<Structure> structure) {
        return structures.contains(pos, structure);
    }

    public Collection<StructureSummary> getStructures() {
        return structures.getStructures();
    }

    public ChunkSummary getChunk(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.get(regionPos).get(pos);
    }

    @SuppressWarnings("unchecked")
    public <T extends Landmark<T>> Landmark<T> getLandmark(LandmarkType<T> type, BlockPos pos) {
        return (Landmark<T>) landmarks.get(type).get(pos);
    }

    @SuppressWarnings("unchecked")
    public <T extends Landmark<T>> Map<BlockPos, Landmark<T>> getLandmarks(LandmarkType<T> type) {
        Map<BlockPos, Landmark<T>> outMap = new HashMap<>();
        if (landmarks.containsKey(type)) landmarks.get(type).forEach((pos, landmark) -> outMap.put(pos, (Landmark<T>) landmark));
        return outMap;
    }

    public Map<RegistryKey<Structure>, Set<ChunkPos>> getStructureKeys() {
        return structures.getStructureKeys();
    }

    public Set<ChunkPos> getChunks() {
        Set<ChunkPos> chunkPosCollection = new HashSet<>();
        regions.forEach((p, r) -> chunkPosCollection.addAll(r.getChunks(p)));
        return chunkPosCollection;
    }

    public Multimap<LandmarkType<?>, BlockPos> getLandmarks() {
        Multimap<LandmarkType<?>, BlockPos> outMap = HashMultimap.create();
        landmarks.forEach((type, map) -> outMap.putAll(type, map.keySet()));
        return outMap;
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
        SurveyorEvents.Invoke.chunkAdded(world, this, chunk.getPos(), getChunk(chunk.getPos()));
    }

    public void putStructureSummary(World world, ChunkPos pos, RegistryKey<Structure> structure, RegistryKey<StructureType<?>> type, Collection<StructurePieceSummary> pieces) {
        structures.putStructureSummary(pos, structure, type, pieces);
        SurveyorEvents.Invoke.structureAdded(world, this, new StructureSummary(pos, structure, type, pieces));
    }

    private void putStructure(World world, StructureStart start) {
        StructureSummary summary = structures.putStructure(world, start);
        if (summary != null) {
            SurveyorEvents.Invoke.structureAdded(world, this, summary);
            if (type == Type.SERVER) new OnStructureAddedS2CPacket(summary.getPos(), summary.getKey(), summary.getType(), summary.getChildren()).send((ServerWorld) world);
        }
    }

    public void putLandmarkNoSync(World world, Landmark<?> landmark) {
        landmarks.computeIfAbsent(landmark.type(), t -> new HashMap<>()).put(landmark.pos(), landmark);
        SurveyorEvents.Invoke.landmarkAdded(world, this, landmark);
    }

    public void putLandmark(World world, Landmark<?> landmark) {
        putLandmarkNoSync(world, landmark);
        if (world instanceof ServerWorld sw) {
            new OnLandmarkAddedS2CPacket(landmark).send(sw);
        } else {
            new OnLandmarkAddedC2SPacket(landmark).send();
        }
    }

    public void putLandmark(PlayerEntity sender, ServerWorld world, Landmark<?> landmark) {
        putLandmarkNoSync(world, landmark);
        OnLandmarkAddedS2CPacket packet = new OnLandmarkAddedS2CPacket(landmark);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.equals(sender)) continue;
            packet.send(player);
        }
    }

    public Landmark<?> removeLandmarkNoSync(World world, LandmarkType<?> type, BlockPos pos) {
        Landmark<?> landmark = landmarks.get(type).remove(pos);
        SurveyorEvents.Invoke.landmarkRemoved(world, this, type, pos);
        return landmark;
    }

    public void removeLandmark(World world, LandmarkType<?> type, BlockPos pos) {
        removeLandmarkNoSync(world, type, pos);
        if (world instanceof ServerWorld sw) {
            new OnLandmarkRemovedS2CPacket(type, pos).send(sw);
        } else {
            new OnLandmarkRemovedC2SPacket(type, pos).send();
        }
    }

    public void removeLandmarksMatching(World world, Class<?> clazz, BlockPos pos) {
        landmarks.forEach((type, map) -> {
            if (map.containsKey(pos) && map.get(pos).getClass().isAssignableFrom(clazz)) {
                removeLandmark(world, map.get(pos).type(), pos);
            }
        });
    }

    public void removeLandmark(ServerPlayerEntity sender, ServerWorld world, LandmarkType<?> type, BlockPos pos) {
        removeLandmarkNoSync(world, type, pos);
        OnLandmarkRemovedS2CPacket packet = new OnLandmarkRemovedS2CPacket(type, pos);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.equals(sender)) continue;
            packet.send(player);
        }
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
        if (landmarksDirty) {
            File landmarksFile = new File(folder, "landmarks.dat");
            try {
                NbtIo.writeCompressed(Landmarks.writeNbt(landmarks, new NbtCompound()), landmarksFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing landmarks file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
        Surveyor.LOGGER.info("[Surveyor] Finished saving data for {}", world.getRegistryKey().getValue());
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
        File structuresFile = new File(folder, "structures.dat");
        if (structuresFile.exists()) {
            try {
                structureNbt = NbtIo.readCompressed(structuresFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error loading structure summary file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
        WorldStructureSummary structures = WorldStructureSummary.readNbt(structureNbt);
        NbtCompound landmarkNbt = new NbtCompound();
        File landmarksFile = new File(folder, "landmarks.dat");
        if (landmarksFile.exists()) {
            try {
                landmarkNbt = NbtIo.readCompressed(landmarksFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error loading landmarks file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
        Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks = Landmarks.fromNbt(landmarkNbt);
        return new WorldSummary(type, regions, structures, landmarks, world.getRegistryManager());
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
