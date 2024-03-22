package folk.sisby.surveyor;

import folk.sisby.surveyor.terrain.RegionSummary;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public interface SurveyorExploration {
    String KEY_DATA = "surveyor";
    String KEY_EXPLORED_TERRAIN = "exploredTerrain";
    String KEY_EXPLORED_STRUCTURES = "exploredStructures";

    Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$exploredTerrain();

    Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> surveyor$exploredStructures();

    World surveyor$getWorld();

    int surveyor$getViewDistance();

    default void surveyor$addExploredChunk(ChunkPos pos) {
        surveyor$exploredTerrain().computeIfAbsent(surveyor$getWorld().getRegistryKey(), k -> new HashMap<>()).computeIfAbsent(new ChunkPos(pos.getRegionX(), pos.getRegionZ()), k -> new BitSet(RegionSummary.REGION_SIZE * RegionSummary.REGION_SIZE)).set(pos.getRegionRelativeX() * RegionSummary.REGION_SIZE + pos.getRegionRelativeZ());
    }

    default void surveyor$addExploredStructure(Structure structure, ChunkPos pos) {
        surveyor$exploredStructures().computeIfAbsent(surveyor$getWorld().getRegistryKey(), k -> new HashMap<>()).computeIfAbsent(surveyor$getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(structure).orElseThrow(), s -> new LongOpenHashSet()).add(pos.toLong());
    }

    default NbtCompound writeExplorationData(NbtCompound nbt) {
        NbtCompound terrainCompound = new NbtCompound();
        surveyor$exploredTerrain().forEach((worldKey, map) -> {
            long[] regionArray = new long[map.size() * 17];
            int i = 0;
            for (Map.Entry<ChunkPos, BitSet> entry : map.entrySet()) {
                regionArray[i * 17] = entry.getKey().toLong();
                long[] regionBits = entry.getValue().toLongArray();
                System.arraycopy(regionBits, 0, regionArray, (i * 17) + 1, regionBits.length);
                i++;
            }
            terrainCompound.putLongArray(worldKey.getValue().toString(), regionArray);
        });
        nbt.put(KEY_EXPLORED_TERRAIN, terrainCompound);

        NbtCompound structuresCompound = new NbtCompound();
        surveyor$exploredStructures().forEach((worldKey, map) -> {
            NbtCompound worldStructuresCompound = new NbtCompound();
            for (RegistryKey<Structure> structure : map.keySet()) {
                worldStructuresCompound.putLongArray(structure.getValue().toString(), map.get(structure).toLongArray());
            }
            structuresCompound.put(worldKey.getValue().toString(), worldStructuresCompound);
        });
        nbt.put(KEY_EXPLORED_STRUCTURES, structuresCompound);
        return nbt;
    }

    default void readExplorationData(NbtCompound nbt) {
        surveyor$exploredTerrain().clear();
        NbtCompound terrainCompound = nbt.getCompound(KEY_EXPLORED_TERRAIN);
        for (String worldKeyString : terrainCompound.getKeys()) {
            long[] regionArray = terrainCompound.getLongArray(worldKeyString);
            Map<ChunkPos, BitSet> regionMap = new HashMap<>();
            for (int i = 0; i < regionArray.length / 17; i++) {
                regionMap.put(new ChunkPos(regionArray[i * 17]), BitSet.valueOf(Arrays.copyOfRange(regionArray, i * 17 + 1, (i + 1) * 17)));
                i++;
            }
            surveyor$exploredTerrain().put(RegistryKey.of(RegistryKeys.WORLD, new Identifier(worldKeyString)), regionMap);
        }

        surveyor$exploredStructures().clear();
        NbtCompound structuresCompound = nbt.getCompound(KEY_EXPLORED_STRUCTURES);
        for (String worldKeyString : structuresCompound.getKeys()) {
            Map<RegistryKey<Structure>, LongSet> structureMap = new HashMap<>();
            NbtCompound worldStructuresCompound = structuresCompound.getCompound(worldKeyString);
            for (String key : worldStructuresCompound.getKeys()) {
                structureMap.put(RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(key)), new LongOpenHashSet(LongSet.of(structuresCompound.getLongArray(key))));
            }
            surveyor$exploredStructures().put(RegistryKey.of(RegistryKeys.WORLD, new Identifier(worldKeyString)), structureMap);
        }
    }
}
