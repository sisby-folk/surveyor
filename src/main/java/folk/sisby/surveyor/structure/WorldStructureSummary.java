package folk.sisby.surveyor.structure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.HashMap;
import java.util.Map;

public class WorldStructureSummary {
    public static final String KEY_STRUCTURES = "structures";
    public static final String KEY_X = "x";
    public static final String KEY_Z = "z";
    public static final String KEY_TYPE = "type";
    public static final String KEY_STARTS = "starts";
    public static final String KEY_SUMMARY = "summary";

    private final Map<ChunkPos, Map<StructureKey, StructureSummary>> structures;

    protected boolean dirty = false;

    public WorldStructureSummary(Map<ChunkPos, Map<StructureKey, StructureSummary>> structures) {
        this.structures = structures;
    }

    public boolean contains(World world, StructureStart start) {
        return structures.containsKey(start.getPos()) && structures.get(start.getPos()).containsKey(new StructureKey(world, start));
    }

    public void putStructure(World world, StructureStart start) {
        structures.computeIfAbsent(start.getPos(), p -> new HashMap<>()).computeIfAbsent(new StructureKey(world, start), k -> {
            dirty = true;
            return StructureSummary.fromStart(start);
        });
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        Map<StructureKey, Map<ChunkPos, StructureSummary>> perStructure = new HashMap<>();
        structures.forEach((pos, map) -> map.forEach((structure, summary) -> {
            perStructure.computeIfAbsent(structure, p -> new HashMap<>()).put(pos, summary);
        }));
        NbtCompound structuresCompound = new NbtCompound();
        perStructure.forEach((structure, map) -> {
            NbtCompound structureCompound = new NbtCompound();
            structureCompound.putString(KEY_TYPE, Registries.STRUCTURE_TYPE.getId(structure.type).toString());
            NbtList startList = new NbtList();
            map.forEach((pos, summary) -> {
                NbtCompound startCompound = new NbtCompound();
                startCompound.putInt(KEY_X, pos.x);
                startCompound.putInt(KEY_Z, pos.z);
                startCompound.put(KEY_SUMMARY, summary.writeNbt(new NbtCompound()));
                startList.add(startCompound);
            });
            structureCompound.put(KEY_STARTS, startList);
            structuresCompound.put(structure.key.getValue().toString(), structureCompound);
        });
        nbt.put(KEY_STRUCTURES, structuresCompound);
        return nbt;
    }

    public static WorldStructureSummary readNbt(NbtCompound nbt) {
        Map<ChunkPos, Map<StructureKey, StructureSummary>> structures = new HashMap<>();
        for (String structureId : nbt.getCompound(KEY_STRUCTURES).getKeys()) {
            RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(structureId));
            StructureType<?> type = Registries.STRUCTURE_TYPE.get(new Identifier(nbt.getCompound(structureId).getString(KEY_TYPE)));
            NbtList startList = nbt.getCompound(structureId).getCompound(KEY_STARTS).getList(structureId, NbtElement.COMPOUND_TYPE);
            for (NbtElement startElement : startList) {
                NbtCompound startCompound = (NbtCompound) startElement;
                ChunkPos pos = new ChunkPos(startCompound.getInt(KEY_X), startCompound.getInt(KEY_Z));
                StructureSummary summary = StructureSummary.fromNbt(startCompound.getCompound(KEY_SUMMARY));
                structures.computeIfAbsent(pos, p -> new HashMap<>()).put(new StructureKey(key, type), summary);
            }
        }
        return new WorldStructureSummary(structures);
    }

    public boolean isDirty() {
        return dirty;
    }

    public record StructureKey(RegistryKey<Structure> key, StructureType<?> type) {
        StructureKey(World world, StructureStart start) {
            this(world.getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(start.getStructure()).orElseThrow(), start.getStructure().getType());
        }
    }
}
