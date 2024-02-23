package folk.sisby.surveyor.structure;

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
import java.util.List;
import java.util.Map;

public class WorldStructureSummary {
    public static final String KEY_STRUCTURES = "structures";
    public static final String KEY_TYPE = "type";
    public static final String KEY_STARTS = "starts";
    public static final String KEY_PIECES = "pieces";

    private final Map<ChunkPos, Map<StructureKey, Collection<StructurePieceSummary>>> structures;

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

    public WorldStructureSummary(Map<ChunkPos, Map<StructureKey, Collection<StructurePieceSummary>>> structures) {
        this.structures = structures;
    }

    public boolean contains(World world, StructureStart start) {
        return structures.containsKey(start.getPos()) && structures.get(start.getPos()).containsKey(new StructureKey(world, start));
    }

    public Collection<StructureSummary> getStructures() {
        Collection<StructureSummary> outStructures =  new ArrayList<>();
        structures.forEach((pos, map) -> map.forEach((key, pieces) -> outStructures.add(new StructureSummary(pos, key.key(), key.type(), pieces))));
        return outStructures;
    }

    public void putStructure(World world, StructureStart start) {
        structures.computeIfAbsent(start.getPos(), p -> new HashMap<>()).computeIfAbsent(new StructureKey(world, start), k -> {
            dirty = true;
            return summarisePieces(start);
        });
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        Map<StructureKey, Map<ChunkPos, Collection<StructurePieceSummary>>> perStructure = new HashMap<>();
        structures.forEach((pos, map) -> map.forEach((structure, summary) -> perStructure.computeIfAbsent(structure, p -> new HashMap<>()).put(pos, summary)));
        NbtCompound structuresCompound = new NbtCompound();
        perStructure.forEach((structure, map) -> {
            NbtCompound structureCompound = new NbtCompound();
            structureCompound.putString(KEY_TYPE, Registries.STRUCTURE_TYPE.getId(structure.type).toString());
            NbtCompound startsCompound = new NbtCompound();
            map.forEach((pos, pieces) -> {
                NbtList pieceList = new NbtList(pieces.stream().map(p -> (NbtElement) p.writeNbt(new NbtCompound())).toList(), NbtElement.COMPOUND_TYPE);
                NbtCompound startCompound = new NbtCompound();
                startCompound.put(KEY_PIECES, pieceList);
                startsCompound.put("%s,%s".formatted(pos.x, pos.z), startCompound);
            });
            structureCompound.put(KEY_STARTS, startsCompound);
            structuresCompound.put(structure.key.getValue().toString(), structureCompound);
        });
        nbt.put(KEY_STRUCTURES, structuresCompound);
        return nbt;
    }

    public static WorldStructureSummary readNbt(NbtCompound nbt) {
        Map<ChunkPos, Map<StructureKey, Collection<StructurePieceSummary>>> structures = new HashMap<>();
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
                Collection<StructurePieceSummary> pieces = new ArrayList<>();
                for (NbtElement pieceElement : startCompound.getList(KEY_PIECES, NbtElement.COMPOUND_TYPE)) {
                    if (((NbtCompound) pieceElement).getString(StructurePieceSummary.KEY_TYPE).equals(Registries.STRUCTURE_PIECE.getId(StructurePieceType.JIGSAW).toString())) {
                        pieces.add(JigsawPieceSummary.fromNbt((NbtCompound) pieceElement));
                    } else {
                        pieces.add(StructurePieceSummary.fromNbt((NbtCompound) pieceElement));
                    }
                }
                structures.computeIfAbsent(new ChunkPos(x, z), p -> new HashMap<>()).put(new StructureKey(key, type), pieces);
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
