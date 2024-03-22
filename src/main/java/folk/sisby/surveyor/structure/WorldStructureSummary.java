package folk.sisby.surveyor.structure;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WorldStructureSummary {
    public static final String KEY_STRUCTURES = "structures";
    public static final String KEY_TYPE = "type";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_STARTS = "starts";
    public static final String KEY_PIECES = "pieces";

    protected final RegistryKey<World> worldKey;
    protected final Map<RegistryKey<Structure>, Map<ChunkPos, StructureSummary>> structures = new ConcurrentHashMap<>();
    protected final Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> structureTypes = new ConcurrentHashMap<>();
    protected final Multimap<RegistryKey<Structure>, TagKey<Structure>> structureTags = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    protected boolean dirty = false;

    public WorldStructureSummary(RegistryKey<World> worldKey, Map<RegistryKey<Structure>, Map<ChunkPos, StructureSummary>> structures, Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> structureTypes, Multimap<RegistryKey<Structure>, TagKey<Structure>> structureTags) {
        this.worldKey = worldKey;
        this.structures.putAll(structures);
        this.structureTypes.putAll(structureTypes);
        this.structureTags.putAll(structureTags);
    }

    public RegistryKey<StructureType<?>> getType(RegistryKey<Structure> key) {
        return structureTypes.get(key);
    }

    public Collection<TagKey<Structure>> getTags(RegistryKey<Structure> key) {
        return structureTags.get(key);
    }

    public boolean contains(World world, StructureStart start) {
        RegistryKey<Structure> key = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(start.getStructure()).orElseThrow();
        return structures.containsKey(key) && structures.get(key).containsKey(start.getPos());
    }

    public boolean contains(RegistryKey<Structure> key, ChunkPos pos) {
        return structures.containsKey(key) && structures.get(key).containsKey(pos);
    }

    public StructureSummary get(RegistryKey<Structure> key, ChunkPos pos) {
        return structures.get(key).get(pos);
    }

    public Map<RegistryKey<Structure>, Map<ChunkPos, StructureSummary>> asMap(SurveyorExploration exploration) {
        Map<RegistryKey<Structure>, Map<ChunkPos, StructureSummary>> map = new HashMap<>();
        structures.forEach((key, starts) -> starts.forEach((pos, summary) -> map.computeIfAbsent(key, t -> new HashMap<>()).put(pos, summary)));
        if (exploration != null) {
            exploration.surveyor$exploredStructures().getOrDefault(worldKey, Map.of()).forEach((key, starts) -> {
                if (map.containsKey(key)) {
                    starts.forEach(l -> map.get(key).remove(new ChunkPos(l)));
                }
            });
        }
        return map;
    }

    public Map<RegistryKey<Structure>, Set<ChunkPos>> keySet(SurveyorExploration exploration) {
        Map<RegistryKey<Structure>, Set<ChunkPos>> map = new HashMap<>();
        if (exploration != null) {
            map.putAll(exploration.surveyor$exploredStructures().getOrDefault(worldKey, Map.of()).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longStream().mapToObj(ChunkPos::new).collect(Collectors.toSet()))));
        } else {
            structures.forEach((key, starts) -> starts.forEach((pos, summary) -> map.computeIfAbsent(key, p -> new HashSet<>()).add(pos)));
        }
        return map;
    }

    protected static StructureSummary summarisePieces(StructureStart start) {
        List<StructurePieceSummary> pieces = new ArrayList<>();
        for (StructurePiece piece : start.getChildren()) {
            if (piece.getType().equals(StructurePieceType.JIGSAW)) {
                pieces.addAll(JigsawPieceSummary.tryFromPiece(piece));
            } else {
                pieces.add(StructurePieceSummary.fromPiece(piece));
            }
        }
        return new StructureSummary(pieces);
    }

    public void put(World world, StructureStart start) {
        RegistryKey<Structure> key = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(start.getStructure()).orElseThrow();
        structures.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        ChunkPos pos = start.getPos();
        if (!structures.get(key).containsKey(pos)) {
            StructureSummary summary = summarisePieces(start);
            RegistryKey<StructureType<?>> type = world.getRegistryManager().get(RegistryKeys.STRUCTURE_TYPE).getKey(start.getStructure().getType()).orElseThrow();
            List<TagKey<Structure>> tags = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getEntry(start.getStructure()).streamTags().toList();
            structures.get(key).put(pos, summary);
            structureTypes.put(key, type);
            structureTags.putAll(key, tags);
            dirty = true;

            SurveyorEvents.Invoke.structureAdded(world, this, key, pos);
            if (world instanceof ServerWorld sw) {
                S2CStructuresAddedPacket.of(key, pos, summary, type, tags).send(sw);
            }
        }
    }

    public void put(World world, RegistryKey<Structure> key, ChunkPos pos, StructureSummary summary, RegistryKey<StructureType<?>> type, Collection<TagKey<Structure>> tagKeys) {
        structures.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(pos, summary);
        structureTypes.put(key, type);
        structureTags.putAll(key, tagKeys);
        dirty = true;
        SurveyorEvents.Invoke.structureAdded(world, this, key, pos);
    }

    protected NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound structuresCompound = new NbtCompound();
        structures.forEach((key, starts) -> {
            NbtCompound structureCompound = new NbtCompound();
            structureCompound.putString(KEY_TYPE, structureTypes.get(key).getValue().toString());
            structureCompound.put(KEY_TAGS, new NbtList(structureTags.get(key).stream().map(t -> (NbtElement) NbtString.of(t.id().toString())).toList(), NbtElement.STRING_TYPE));
            NbtCompound startsCompound = new NbtCompound();
            starts.forEach((pos, summary) -> {
                NbtList pieceList = new NbtList(summary.getChildren().stream().map(p -> (NbtElement) p.writeNbt(new NbtCompound())).toList(), NbtElement.COMPOUND_TYPE);
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

    public int save(World world, File folder) {
        if (dirty) {
            File structureFile = new File(folder, "structures.dat");
            try {
                NbtIo.writeCompressed(writeNbt(new NbtCompound()), structureFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing structure summary file for {}.", world.getRegistryKey().getValue(), e);
            }
            return structures.size();
        }
        return 0;
    }

    public static StructurePieceSummary readStructurePieceNbt(NbtCompound nbt) {
        if (nbt.getString(StructurePieceSummary.KEY_TYPE).equals(Registries.STRUCTURE_PIECE.getId(StructurePieceType.JIGSAW).toString())) {
            return JigsawPieceSummary.fromNbt(nbt);
        } else {
            return StructurePieceSummary.fromNbt(nbt);
        }
    }

    protected static WorldStructureSummary readNbt(RegistryKey<World> worldKey, NbtCompound nbt) {
        Map<RegistryKey<Structure>, Map<ChunkPos, StructureSummary>> structures = new ConcurrentHashMap<>();
        Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> structureTypes = new ConcurrentHashMap<>();
        Multimap<RegistryKey<Structure>, TagKey<Structure>> structureTags = HashMultimap.create();
        NbtCompound structuresCompound = nbt.getCompound(KEY_STRUCTURES);
        for (String structureId : structuresCompound.getKeys()) {
            RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(structureId));
            NbtCompound structureCompound = structuresCompound.getCompound(structureId);
            RegistryKey<StructureType<?>> type = RegistryKey.of(RegistryKeys.STRUCTURE_TYPE, new Identifier(structureCompound.getString(KEY_TYPE)));
            structureTypes.put(key, type);
            Collection<TagKey<Structure>> tags = structureCompound.getList(KEY_TAGS, NbtElement.STRING_TYPE).stream().map(e -> TagKey.of(RegistryKeys.STRUCTURE, new Identifier(e.asString()))).toList();
            structureTags.putAll(key, tags);
            NbtCompound startsCompound = structureCompound.getCompound(KEY_STARTS);
            for (String posKey : startsCompound.getKeys()) {
                int x = Integer.parseInt(posKey.split(",")[0]);
                int z = Integer.parseInt(posKey.split(",")[1]);
                NbtCompound startCompound = startsCompound.getCompound(posKey);
                Collection<StructurePieceSummary> pieces = new ArrayList<>();
                for (NbtElement pieceElement : startCompound.getList(KEY_PIECES, NbtElement.COMPOUND_TYPE)) {
                    pieces.add(readStructurePieceNbt((NbtCompound) pieceElement));
                }
                structures.computeIfAbsent(key, p -> new ConcurrentHashMap<>()).put(new ChunkPos(x, z), new StructureSummary(pieces));
            }
        }
        return new WorldStructureSummary(worldKey, structures, structureTypes, structureTags);
    }

    public static WorldStructureSummary load(World world, File folder) {
        NbtCompound structureNbt = new NbtCompound();
        File structuresFile = new File(folder, "structures.dat");
        if (structuresFile.exists()) {
            try {
                structureNbt = NbtIo.readCompressed(structuresFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error loading structure summary file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
        return WorldStructureSummary.readNbt(world.getRegistryKey(), structureNbt);
    }

    public static void onChunkLoad(World world, Chunk chunk) {
        WorldStructureSummary structures = ((SurveyorWorld) world).surveyor$getWorldSummary().structures();
        chunk.getStructureStarts().forEach((structure, start) -> {
            if (!structures.contains(world, start)) structures.put(world, start);
        });
    }

    public static void onStructurePlace(World world, StructureStart start) {
        WorldStructureSummary structures = ((SurveyorWorld) world).surveyor$getWorldSummary().structures();
        if (!structures.contains(world, start)) structures.put(world, start);
    }
}
