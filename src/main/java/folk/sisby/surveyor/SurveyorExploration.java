package folk.sisby.surveyor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.terrain.RegionSummary;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface SurveyorExploration {
    static SurveyorExploration of(ServerPlayerEntity player) {
        return ((SurveyorPlayer) player).surveyor$getExploration();
    }

    String KEY_EXPLORED_TERRAIN = "exploredTerrain";
    String KEY_EXPLORED_STRUCTURES = "exploredStructures";

    Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain();

    Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures();

    Set<UUID> sharedPlayers();

    default boolean exploredChunk(RegistryKey<World> worldKey, ChunkPos pos) {
        ChunkPos regionPos = new ChunkPos(pos.getRegionX(), pos.getRegionZ());
        Map<ChunkPos, BitSet> regions = terrain().get(worldKey);
        return Surveyor.CONFIG.shareAllTerrain || regions != null && regions.containsKey(regionPos) && regions.get(regionPos).get(RegionSummary.bitForChunk(pos));
    }

    default boolean exploredStructure(RegistryKey<World> worldKey, RegistryKey<Structure> structure, ChunkPos pos) {
        Map<RegistryKey<Structure>, LongSet> structures = structures().get(worldKey);
        return Surveyor.CONFIG.shareAllStructures || structures != null && structures.containsKey(structure) && structures.get(structure).contains(pos.toLong());
    }

    default boolean exploredLandmark(RegistryKey<World> worldKey, Landmark<?> landmark) {
        return Surveyor.CONFIG.shareAllLandmarks || (landmark.owner() == null ? exploredChunk(worldKey, new ChunkPos(landmark.pos())) : sharedPlayers().contains(landmark.owner()));
    }

    default Map<ChunkPos, BitSet> limitTerrainBitset(RegistryKey<World> worldKey, Map<ChunkPos, BitSet> bitSet) {
        if (Surveyor.CONFIG.shareAllTerrain) return bitSet;
        Map<ChunkPos, BitSet> regions = terrain().get(worldKey);
        if (regions == null) {
            bitSet.clear();
        } else {
            bitSet.forEach((rPos, set) -> {
                if (regions.containsKey(rPos)) {
                    set.and(regions.get(rPos));
                } else {
                    set.clear();
                }
            });
        }
        return bitSet;
    }

    default Multimap<RegistryKey<Structure>, ChunkPos> limitStructureKeySet(RegistryKey<World> worldKey, Multimap<RegistryKey<Structure>, ChunkPos> keySet) {
        if (Surveyor.CONFIG.shareAllStructures) return keySet;
        Map<RegistryKey<Structure>, LongSet> structures = structures().get(worldKey);
        if (structures == null) {
            keySet.clear();
        } else {
            keySet.keySet().removeIf(key -> !structures.containsKey(key));
            keySet.entries().removeIf(e -> !structures.get(e.getKey()).contains(e.getValue().toLong()));
        }
        return keySet;
    }

    default Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> limitLandmarkMap(RegistryKey<World> worldKey, Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> asMap) {
        if (Surveyor.CONFIG.shareAllLandmarks) return asMap;
        Multimap<LandmarkType<?>, BlockPos> toRemove = HashMultimap.create();
        asMap.forEach((type, map) -> map.forEach((pos, landmark) -> {
            if (!exploredLandmark(worldKey, landmark)) toRemove.put(type, pos);
        }));
        toRemove.forEach((type, pos) -> {
            asMap.get(type).remove(pos);
            if (asMap.get(type).isEmpty()) asMap.remove(type);
        });
        return asMap;
    }

    default Multimap<LandmarkType<?>, BlockPos> limitLandmarkKeySet(RegistryKey<World> worldKey, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> keySet) {
        if (Surveyor.CONFIG.shareAllLandmarks) return keySet;
        Multimap<LandmarkType<?>, BlockPos> toRemove = HashMultimap.create();
        keySet.forEach((type, pos) -> {
            if (!exploredLandmark(worldKey, worldLandmarks.get(type, pos))) toRemove.put(type, pos);
        });
        toRemove.forEach(keySet::remove);
        return keySet;
    }

    default void updateClientForMergeRegion(World world, ChunkPos regionPos, BitSet bitSet) {
        Set<ChunkPos> terrainKeys = bitSet.stream().mapToObj(i -> RegionSummary.chunkForBit(regionPos, i)).collect(Collectors.toSet());
        SurveyorClientEvents.Invoke.terrainUpdated(world, terrainKeys);
        Multimap<LandmarkType<?>, BlockPos> landmarkKeys = HashMultimap.create();
        WorldSummary.of(world).landmarks().keySet(this).forEach((type, pos) -> {
            if (terrainKeys.contains(new ChunkPos(pos))) landmarkKeys.put(type, pos);
        });
        SurveyorClientEvents.Invoke.landmarksAdded(world, landmarkKeys);
    }

    default void mergeRegion(RegistryKey<World> worldKey, ChunkPos regionPos, BitSet bitSet) {
        terrain().computeIfAbsent(worldKey, k -> new HashMap<>()).computeIfAbsent(regionPos, p -> new BitSet(RegionSummary.REGION_SIZE)).or(bitSet);
    }

    default void updateClientForAddChunk(World world, ChunkPos chunkPos) {
        SurveyorClientEvents.Invoke.terrainUpdated(world, chunkPos);
        Multimap<LandmarkType<?>, BlockPos> landmarkKeys = HashMultimap.create();
        WorldSummary.of(world).landmarks().keySet(this).forEach((type, pos) -> {
            if (chunkPos.equals(new ChunkPos(pos))) landmarkKeys.put(type, pos);
        });
        SurveyorClientEvents.Invoke.landmarksAdded(world, landmarkKeys);
    }

    default void addChunk(RegistryKey<World> worldKey, ChunkPos pos) {
        terrain().computeIfAbsent(worldKey, k -> new HashMap<>()).computeIfAbsent(new ChunkPos(pos.getRegionX(), pos.getRegionZ()), k -> new BitSet(RegionSummary.BITSET_SIZE)).set(RegionSummary.bitForChunk(pos));
    }

    default void updateClientForAddStructure(World world, RegistryKey<Structure> structureKey, ChunkPos pos) {
        SurveyorClientEvents.Invoke.structuresAdded(world, structureKey, pos);
    }

    default void addStructure(RegistryKey<World> worldKey, RegistryKey<Structure> structureKey, ChunkPos pos) {
        structures().computeIfAbsent(worldKey, k -> new HashMap<>()).computeIfAbsent(structureKey, s -> new LongOpenHashSet()).add(pos.toLong());
    }

    default NbtCompound write(NbtCompound nbt) {
        NbtCompound terrainCompound = new NbtCompound();
        terrain().forEach((worldKey, map) -> {
            LongList regionLongs = new LongArrayList();
            for (Map.Entry<ChunkPos, BitSet> entry : map.entrySet()) {
                regionLongs.add(entry.getKey().toLong());
                if (entry.getValue().cardinality() == RegionSummary.BITSET_SIZE) {
                    regionLongs.add(-1);
                } else {
                    long[] regionBits = entry.getValue().toLongArray();
                    regionLongs.add(regionBits.length);
                    regionLongs.addAll(LongList.of(regionBits));
                }
            }
            terrainCompound.putLongArray(worldKey.getValue().toString(), regionLongs.toLongArray());
        });
        nbt.put(KEY_EXPLORED_TERRAIN, terrainCompound);

        NbtCompound structuresCompound = new NbtCompound();
        structures().forEach((worldKey, map) -> {
            NbtCompound worldStructuresCompound = new NbtCompound();
            for (RegistryKey<Structure> structure : map.keySet()) {
                worldStructuresCompound.putLongArray(structure.getValue().toString(), map.get(structure).toLongArray());
            }
            structuresCompound.put(worldKey.getValue().toString(), worldStructuresCompound);
        });
        nbt.put(KEY_EXPLORED_STRUCTURES, structuresCompound);
        return nbt;
    }

    default void read(NbtCompound nbt) {
        NbtCompound terrainCompound = nbt.getCompound(KEY_EXPLORED_TERRAIN);
        for (String worldKeyString : terrainCompound.getKeys()) {
            long[] regionArray = terrainCompound.getLongArray(worldKeyString);
            Map<ChunkPos, BitSet> regionMap = new HashMap<>();
            for (int i = 0; i + 1 < regionArray.length; i += 2) {
                ChunkPos rPos = new ChunkPos(regionArray[i]);
                int bitLength = (int) regionArray[i + 1];
                if (bitLength == -1) {
                    BitSet set = new BitSet(RegionSummary.BITSET_SIZE);
                    set.set(0, RegionSummary.BITSET_SIZE);
                    regionMap.put(rPos, set);
                } else {
                    long[] bitArray = new long[bitLength];
                    System.arraycopy(regionArray, i + 2, bitArray, 0, bitLength);
                    regionMap.put(rPos, BitSet.valueOf(bitArray));
                    i += bitLength;
                }
            }
            terrain().put(RegistryKey.of(RegistryKeys.WORLD, new Identifier(worldKeyString)), regionMap);
        }

        NbtCompound structuresCompound = nbt.getCompound(KEY_EXPLORED_STRUCTURES);
        for (String worldKeyString : structuresCompound.getKeys()) {
            Map<RegistryKey<Structure>, LongSet> structureMap = new HashMap<>();
            NbtCompound worldStructuresCompound = structuresCompound.getCompound(worldKeyString);
            for (String key : worldStructuresCompound.getKeys()) {
                structureMap.put(RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(key)), new LongOpenHashSet(LongSet.of(worldStructuresCompound.getLongArray(key))));
            }
            structures().put(RegistryKey.of(RegistryKeys.WORLD, new Identifier(worldKeyString)), structureMap);
        }
    }

    default void copyFrom(SurveyorExploration them) {
        terrain().clear();
        terrain().putAll(them.terrain());
        structures().clear();
        structures().putAll(them.structures());
    }
}
