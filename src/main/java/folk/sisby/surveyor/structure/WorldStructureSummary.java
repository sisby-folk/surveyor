package folk.sisby.surveyor.structure;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.util.MapUtil;
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
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldStructureSummary {
    public static final String KEY_STRUCTURES = "structures";
    public static final String KEY_TYPE = "type";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_STARTS = "starts";
    public static final String KEY_PIECES = "pieces";

    protected final RegistryKey<World> worldKey;
    protected final Map<RegistryKey<Structure>, Map<ChunkPos, StructureStartSummary>> structures = new ConcurrentHashMap<>();
    protected final Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> structureTypes = new ConcurrentHashMap<>();
    protected final Multimap<RegistryKey<Structure>, TagKey<Structure>> structureTags = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    protected boolean dirty = false;

    public WorldStructureSummary(RegistryKey<World> worldKey, Map<RegistryKey<Structure>, Map<ChunkPos, StructureStartSummary>> structures, Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> structureTypes, Multimap<RegistryKey<Structure>, TagKey<Structure>> structureTags) {
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

    public StructureStartSummary get(RegistryKey<Structure> key, ChunkPos pos) {
        return structures.get(key).get(pos);
    }

    public Map<RegistryKey<Structure>, Map<ChunkPos, StructureStartSummary>> asMap(SurveyorExploration exploration) {
        Multimap<RegistryKey<Structure>, ChunkPos> keySet = keySet(exploration);
        Map<RegistryKey<Structure>, Map<ChunkPos, StructureStartSummary>> map = new HashMap<>();
        keySet.forEach((key, pos) -> map.computeIfAbsent(key, k -> new HashMap<>()).put(pos, get(key, pos)));
        return map;
    }

    public Multimap<RegistryKey<Structure>, ChunkPos> keySet(SurveyorExploration exploration) {
        Multimap<RegistryKey<Structure>, ChunkPos> map = MapUtil.keyMultiMap(structures);
        if (exploration != null) exploration.limitStructureKeySet(worldKey, map);
        return map;
    }

    protected static StructureStartSummary summarisePieces(StructureContext context, StructureStart start) {
        List<StructurePieceSummary> pieces = new ArrayList<>();
        for (StructurePiece piece : start.getChildren()) {
            if (piece.getType().equals(StructurePieceType.JIGSAW)) {
                pieces.addAll(JigsawPieceSummary.tryFromPiece(piece));
            } else {
                pieces.add(StructurePieceSummary.fromPiece(context, piece, start.getChildren().size() <= 10));
            }
        }
        return new StructureStartSummary(pieces);
    }

    public void put(ServerWorld world, StructureStart start) {
        RegistryKey<Structure> key = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getKey(start.getStructure()).orElseThrow();
        structures.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        ChunkPos pos = start.getPos();
        if (!structures.get(key).containsKey(pos)) {
            StructureStartSummary summary = summarisePieces(StructureContext.from(world), start);
            RegistryKey<StructureType<?>> type = world.getRegistryManager().get(RegistryKeys.STRUCTURE_TYPE).getKey(start.getStructure().getType()).orElseThrow();
            List<TagKey<Structure>> tags = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getEntry(start.getStructure()).streamTags().toList();
            structures.get(key).put(pos, summary);
            structureTypes.put(key, type);
            structureTags.putAll(key, tags);
            dirty = true;
            SurveyorEvents.Invoke.structuresAdded(world, key, pos);
        }
    }

    public void put(World world, RegistryKey<Structure> key, ChunkPos pos, StructureStartSummary summary, RegistryKey<StructureType<?>> type, Collection<TagKey<Structure>> tagKeys) {
        structures.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(pos, summary);
        structureTypes.put(key, type);
        structureTags.putAll(key, tagKeys);
        dirty = true;
        SurveyorEvents.Invoke.structuresAdded(world, key, pos);
    }

    protected NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound structuresCompound = new NbtCompound();
        structures.forEach((key, starts) -> {
            NbtCompound structureCompound = new NbtCompound();
            structureCompound.putString(KEY_TYPE, structureTypes.get(key).getValue().toString());
            structureCompound.put(KEY_TAGS, new NbtList(structureTags.get(key).stream().map(t -> (NbtElement) NbtString.of(t.id().toString())).toList(), NbtElement.STRING_TYPE));
            NbtCompound startsCompound = new NbtCompound();
            starts.forEach((pos, summary) -> {
                NbtList pieceList = new NbtList(summary.getChildren().stream().map(p -> (NbtElement) p.toNbt()).toList(), NbtElement.COMPOUND_TYPE);
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
            return structures.values().stream().mapToInt(Map::size).sum();
        }
        return 0;
    }

    public static StructurePieceSummary readStructurePieceNbt(NbtCompound nbt) {
        if (nbt.getString("id").equals(Registries.STRUCTURE_PIECE.getId(StructurePieceType.JIGSAW).toString())) {
            return new JigsawPieceSummary(nbt);
        } else {
            return new StructurePieceSummary(nbt);
        }
    }

    protected static WorldStructureSummary readNbt(RegistryKey<World> worldKey, NbtCompound nbt) {
        Map<RegistryKey<Structure>, Map<ChunkPos, StructureStartSummary>> structures = new ConcurrentHashMap<>();
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
                structures.computeIfAbsent(key, p -> new ConcurrentHashMap<>()).put(new ChunkPos(x, z), new StructureStartSummary(pieces));
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

    public static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        WorldStructureSummary structures = WorldSummary.of(world).structures();
        chunk.getStructureStarts().forEach((structure, start) -> {
            if (!structures.contains(world, start)) structures.put(world, start);
        });
    }

    public static void onStructurePlace(ServerWorld world, StructureStart start) {
        WorldStructureSummary structures = WorldSummary.of(world).structures();
        if (!structures.contains(world, start)) structures.put(world, start);
    }
}
