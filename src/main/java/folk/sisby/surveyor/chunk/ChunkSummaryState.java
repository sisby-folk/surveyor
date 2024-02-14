package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.Surveyor;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChunkSummaryState extends PersistentState {
    public static final String STATE_KEY = "surveyor_chunk_summary";
    public static final String KEY_BIOMES = "biomes";
    public static final String KEY_BLOCKS = "blocks";
    public static final String KEY_CHUNKS = "chunks";

    private final Map<ChunkPos, ChunkSummary> chunks;
    private final DynamicRegistryManager manager;

    public ChunkSummaryState(Map<ChunkPos, ChunkSummary> chunks, DynamicRegistryManager manager) {
        this.chunks = chunks;
        this.manager = manager;
    }

    public boolean contains(Chunk chunk) {
        return chunks.containsKey(chunk.getPos());
    }

    public void putChunk(World world, Chunk chunk) {
        chunks.put(chunk.getPos(), new ChunkSummary(chunk, Surveyor.CONFIG.getLayers(world, chunk)));
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        List<Biome> biomePalette = chunks.values().stream().flatMap(summary -> summary.layers.values().stream()).flatMap(Arrays::stream).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::biome).distinct().sorted(Comparator.comparingInt(b -> manager.get(RegistryKeys.BIOME).getRawId(b))).toList();
        List<Block> blockPalette = chunks.values().stream().flatMap(summary -> summary.layers.values().stream()).flatMap(Arrays::stream).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::block).distinct().sorted(Comparator.comparingInt(b -> manager.get(RegistryKeys.BLOCK).getRawId(b))).toList();
        nbt.put(KEY_BIOMES, new NbtList(biomePalette.stream().map(b -> (NbtElement) NbtString.of(manager.get(RegistryKeys.BIOME).getId(b).toString())).toList(), NbtElement.STRING_TYPE));
        nbt.put(KEY_BLOCKS, new NbtList(blockPalette.stream().map(b -> (NbtElement) NbtString.of(manager.get(RegistryKeys.BLOCK).getId(b).toString())).toList(), NbtElement.STRING_TYPE));
        NbtCompound chunksCompound = new NbtCompound();
        chunks.forEach((pos, summary) -> chunksCompound.put("%s,%s".formatted(pos.x, pos.z), summary.writeNbt(new NbtCompound(), biomePalette, blockPalette)));
        nbt.put(KEY_CHUNKS, chunksCompound);
        return nbt;
    }

    public static ChunkSummaryState readNbt(NbtCompound nbt, DynamicRegistryManager manager) {
        Map<ChunkPos, ChunkSummary> chunks = new HashMap<>();
        List<Biome> biomePalette =  nbt.getList(KEY_BIOMES, NbtElement.STRING_TYPE).stream().map(e -> manager.get(RegistryKeys.BIOME).get(new Identifier(e.asString()))).toList();
        List<Block> blockPalette =  nbt.getList(KEY_BLOCKS, NbtElement.STRING_TYPE).stream().map(e -> manager.get(RegistryKeys.BLOCK).get(new Identifier(e.asString()))).toList();
        NbtCompound chunksCompound = nbt.getCompound(KEY_CHUNKS);
        for (String posKey : chunksCompound.getKeys()) {
            int x = Integer.parseInt(posKey.split(",")[0]);
            int z = Integer.parseInt(posKey.split(",")[1]);
            chunks.put(
                new ChunkPos(x, z),
                new ChunkSummary(chunksCompound.getCompound(posKey), biomePalette, blockPalette)
            );
        }
        return new ChunkSummaryState(chunks, manager);
    }

    public static ChunkSummaryState getOrCreate(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(nbt -> ChunkSummaryState.readNbt(nbt, world.getRegistryManager()), () -> new ChunkSummaryState(new HashMap<>(), world.getRegistryManager()), STATE_KEY);
    }

    public static void onChunkLoad(ServerWorld world, Chunk chunk) {
        ChunkSummaryState state = ChunkSummaryState.getOrCreate(world);
        if (!state.contains(chunk)) state.putChunk(world, chunk);
    }

    public static void onChunkUnload(ServerWorld world, WorldChunk chunk) {
        ChunkSummaryState state = ChunkSummaryState.getOrCreate(world);
        if (chunk.needsSaving()) state.putChunk(world, chunk);
    }
}
