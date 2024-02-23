package folk.sisby.surveyor.structure;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorldStructureSummary {
    public static final String KEY_STRUCTURES = "structures";
    public static final String KEY_TYPE = "type";
    public static final String KEY_STARTS = "starts";
    public static final String KEY_PIECES = "pieces";

    private final Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures;

    protected boolean dirty = false;

    public static Collection<StructurePieceSummary> summarisePieces(StructureStart start) {
        List<StructurePieceSummary> pieces = new ArrayList<>();
        for (StructurePiece piece : start.getChildren()) {
            if (piece.getType().equals(StructurePieceType.JIGSAW)) {
                pieces.addAll(JigsawPieceSummary.tryFromPiece(piece));
            } else {
                pieces.add(StructurePieceSummary.fromPiece(piece));
            }
        }
        return pieces;
    }

    public WorldStructureSummary(Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures) {
        this.structures = structures;
    }

    public boolean contains(World world, StructureStart start) {
        return structures.containsKey(start.getPos()) && structures.get(start.getPos()).containsKey(world.getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(start.getStructure()).orElseThrow());
    }

    public boolean contains(ChunkPos pos, RegistryKey<Structure> structure) {
        return structures.containsKey(pos) && structures.get(pos).containsKey(structure);
    }

    public Collection<StructureSummary> getStructures() {
        Collection<StructureSummary> outStructures = new ArrayList<>();
        structures.forEach((pos, map) -> map.forEach((key, pair) -> outStructures.add(new StructureSummary(pos, key, pair.left(), pair.right()))));
        return outStructures;
    }

    public Map<RegistryKey<Structure>, Set<ChunkPos>> getStructureKeys() {
        Map<RegistryKey<Structure>, Set<ChunkPos>> outMap = new HashMap<>();
        structures.forEach((pos, map) -> map.forEach((key, pair) -> outMap.computeIfAbsent(key, p -> new HashSet<>()).add(pos)));
        return outMap;
    }

    public void putStructure(World world, StructureStart start) {
        structures.computeIfAbsent(start.getPos(), p -> new HashMap<>()).computeIfAbsent(world.getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(start.getStructure()).orElseThrow(), k -> {
            dirty = true;
            return Pair.of(world.getRegistryManager().get(RegistryKeys.STRUCTURE_TYPE).getKey(start.getStructure().getType()).orElseThrow(), summarisePieces(start));
        });
    }

    public void putStructureSummary(ChunkPos pos, RegistryKey<Structure> structure, RegistryKey<StructureType<?>> type, Collection<StructurePieceSummary> pieces) {
        structures.computeIfAbsent(pos, p -> new HashMap<>()).put(structure, Pair.of(type, pieces));
        dirty = true;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Map<ChunkPos, Collection<StructurePieceSummary>>>> perStructure = new HashMap<>();
        structures.forEach((pos, map) -> map.forEach((structure, pair) -> perStructure.computeIfAbsent(structure, p -> Pair.of(pair.left(), new HashMap<>())).right().put(pos, pair.right())));
        NbtCompound structuresCompound = new NbtCompound();
        perStructure.forEach((key, pair) -> {
            NbtCompound structureCompound = new NbtCompound();
            structureCompound.putString(KEY_TYPE, pair.left().getValue().toString());
            NbtCompound startsCompound = new NbtCompound();
            pair.right().forEach((pos, pieces) -> {
                NbtList pieceList = new NbtList(pieces.stream().map(p -> (NbtElement) p.writeNbt(new NbtCompound())).toList(), NbtElement.COMPOUND_TYPE);
                NbtCompound startCompound = new NbtCompound();
                startCompound.put(KEY_PIECES, pieceList);
                startsCompound.put("%s,%s".formatted(pos.x, pos.z), startCompound);
            });
            structureCompound.put(KEY_STARTS, startsCompound);
            structuresCompound.put(key.getValue().toString(), structureCompound);
        });
        nbt.put(KEY_STRUCTURES, structuresCompound);
        return nbt;
    }

    public static StructurePieceSummary readStructurePieceNbt(NbtCompound nbt) {
        if (nbt.getString(StructurePieceSummary.KEY_TYPE).equals(Registries.STRUCTURE_PIECE.getId(StructurePieceType.JIGSAW).toString())) {
            return JigsawPieceSummary.fromNbt(nbt);
        } else {
            return StructurePieceSummary.fromNbt(nbt);
        }
    }

    public static WorldStructureSummary readNbt(NbtCompound nbt) {
        Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures = new HashMap<>();
        NbtCompound structuresCompound = nbt.getCompound(KEY_STRUCTURES);
        for (String structureId : structuresCompound.getKeys()) {
            RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(structureId));
            NbtCompound structureCompound = structuresCompound.getCompound(structureId);
            RegistryKey<StructureType<?>> type = RegistryKey.of(RegistryKeys.STRUCTURE_TYPE, new Identifier(structureCompound.getString(KEY_TYPE)));
            NbtCompound startsCompound = structureCompound.getCompound(KEY_STARTS);
            for (String posKey : startsCompound.getKeys()) {
                int x = Integer.parseInt(posKey.split(",")[0]);
                int z = Integer.parseInt(posKey.split(",")[1]);
                NbtCompound startCompound = startsCompound.getCompound(posKey);
                Collection<StructurePieceSummary> pieces = new ArrayList<>();
                for (NbtElement pieceElement : startCompound.getList(KEY_PIECES, NbtElement.COMPOUND_TYPE)) {
                    pieces.add(readStructurePieceNbt((NbtCompound) pieceElement));
                }
                structures.computeIfAbsent(new ChunkPos(x, z), p -> new HashMap<>()).put(key, Pair.of(type, pieces));
            }
        }
        return new WorldStructureSummary(structures);
    }

    public boolean isDirty() {
        return dirty;
    }
}
