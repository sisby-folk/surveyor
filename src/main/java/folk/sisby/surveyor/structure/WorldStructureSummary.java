package folk.sisby.surveyor.structure;

import net.minecraft.nbt.NbtCompound;
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
    public static final String KEY_TYPE = "type";
    public static final String KEY_STARTS = "starts";

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
        structures.forEach((pos, map) -> map.forEach((structure, summary) -> perStructure.computeIfAbsent(structure, p -> new HashMap<>()).put(pos, summary)));
        NbtCompound structuresCompound = new NbtCompound();
        perStructure.forEach((structure, map) -> {
            NbtCompound structureCompound = new NbtCompound();
            structureCompound.putString(KEY_TYPE, Registries.STRUCTURE_TYPE.getId(structure.type).toString());
            NbtCompound startsCompound = new NbtCompound();
            map.forEach((pos, summary) -> {
                NbtCompound startCompound = summary.writeNbt(new NbtCompound());
                startsCompound.put("%s,%s".formatted(pos.x, pos.z), startCompound);
            });
            structureCompound.put(KEY_STARTS, startsCompound);
            structuresCompound.put(structure.key.getValue().toString(), structureCompound);
        });
        nbt.put(KEY_STRUCTURES, structuresCompound);
        return nbt;
    }

    public static WorldStructureSummary readNbt(NbtCompound nbt) {
        Map<ChunkPos, Map<StructureKey, StructureSummary>> structures = new HashMap<>();
        NbtCompound structuresCompound = nbt.getCompound(KEY_STRUCTURES);
        for (String structureId : structuresCompound.getKeys()) {
            RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(structureId));
            NbtCompound structureCompound = structuresCompound.getCompound(structureId);
            StructureType<?> type = Registries.STRUCTURE_TYPE.get(new Identifier(structureCompound.getString(KEY_TYPE)));
            NbtCompound startsCompound = structureCompound.getCompound(KEY_STARTS);
            for (String posKey : startsCompound.getKeys()) {
                int x = Integer.parseInt(posKey.split(",")[0]);
                int z = Integer.parseInt(posKey.split(",")[1]);
                NbtCompound startCompound = startsCompound.getCompound(posKey);
                structures.computeIfAbsent(new ChunkPos(x, z), p -> new HashMap<>()).put(new StructureKey(key, type), StructureSummary.fromNbt(startCompound));
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
