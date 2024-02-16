package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.util.NbtUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

public class ChunkSummary {
    private static final String KEY_LAYERS = "layers";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_BIOME = "biome";
    private static final String KEY_BLOCK = "block";
    private static final String KEY_LIGHT = "light";

    protected final TreeMap<Integer, FloorSummary[][]> layers = new TreeMap<>();

    public static FloorSummary getTopFloor(World world, Chunk chunk, int x, int topY, int bottomY, int z, MutableBoolean foundAir) {
        ChunkSection[] chunkSections = chunk.getSectionArray();
        FloorSummary foundFloor = null;
        for (int y = topY; y > bottomY; y--) {
            int sectionIndex = chunk.getSectionIndex(y);
            if (foundAir.isFalse() && chunkSections[sectionIndex].nonEmptyBlockCount == 4096) {
                y = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(sectionIndex));
                continue;
            }
            if (chunkSections[sectionIndex].isEmpty()) {
                foundAir.setTrue();
                y = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(sectionIndex));
                continue;
            }
            BlockState state = chunkSections[sectionIndex].getBlockState(x & 15, y & 15, z & 15);
            if (state.isAir()) {
                foundAir.setTrue();
                continue;
            }
            if (state.blocksMovement() || !state.getFluidState().isEmpty()) {
                if (foundFloor == null && foundAir.isTrue()) foundFloor = new FloorSummary(y, chunkSections[sectionIndex].getBiome(x & 3, y & 3, z & 3).value(), state.getBlock(), world.getLightLevel(LightType.BLOCK, new BlockPos(x, y, z)));
                foundAir.setFalse();
            }
        }
        return foundFloor;
    }

    public ChunkSummary(World world, Chunk chunk, TreeSet<Integer> layers) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                MutableBoolean foundAir = new MutableBoolean(false);
                for (int i : layers.descendingSet()) {
                    if (!layers.first().equals(i)) this.layers.computeIfAbsent(i, k -> new FloorSummary[16][16])[x][z] = getTopFloor(world, chunk, x, i, layers.lower(i), z, foundAir);
                }
            }
        }
    }

    public ChunkSummary(NbtCompound nbt, IndexedIterable<Biome> biomePalette, IndexedIterable<Block> blockPalette) {
        NbtCompound layersCompound = nbt.getCompound(KEY_LAYERS);
        for (String key : layersCompound.getKeys()) {
            int layerY = Integer.parseInt(key);
            FloorSummary[][] layer = new FloorSummary[16][16];
            NbtCompound layerCompound = layersCompound.getCompound(key);
            int[] heightArray = NbtUtil.readUInts(layerCompound.get(KEY_HEIGHT));
            int[] biomeArray = NbtUtil.readUInts(layerCompound.get(KEY_BIOME));
            int[] blockArray = NbtUtil.readUInts(layerCompound.get(KEY_BLOCK));
            int[] lightArray = NbtUtil.readUInts(layerCompound.get(KEY_LIGHT));
            for (int i = 0; i < 255; i++) {
                layer[i / 16][i % 16] = heightArray[i] == -1 ? null : new FloorSummary(layerY - heightArray[i], biomePalette.get(biomeArray[i]), blockPalette.get(blockArray[i]), lightArray[i]);
            }
            layers.put(layerY, layer);
        }
    }

    public NbtCompound writeNbt(NbtCompound nbt, IndexedIterable<Biome> biomePalette, IndexedIterable<Block> blockPalette) {
        NbtCompound layersCompound = new NbtCompound();
        layers.forEach((layerY, floorSummaries) -> {
            NbtCompound layerCompound = new NbtCompound();
            NbtUtil.writeNullableUInts(layerCompound, KEY_HEIGHT, Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? -1 : layerY - f.y()).toList());
            NbtUtil.writeNullableUInts(layerCompound, KEY_BIOME, Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? null : biomePalette.getRawId(f.biome())).toList());
            NbtUtil.writeNullableUInts(layerCompound, KEY_BLOCK, Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? null : blockPalette.getRawId(f.block())).toList());
            NbtUtil.writeNullableUInts(layerCompound, KEY_LIGHT, Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? null : f.lightLevel()).toList());
            layersCompound.put(String.valueOf(layerY), layerCompound);
        });
        nbt.put(KEY_LAYERS, layersCompound);
        return nbt;
    }

    public @Nullable FloorSummary getTopFloor(int x, int z) {
        for (Integer layerY : layers.descendingKeySet()) {
            if (layers.get(layerY)[x][z] != null) return layers.get(layerY)[x][z];
        }
        return null;
    }

    public TreeMap<Integer, FloorSummary> getFloors(int x, int z) {
        TreeMap<Integer, FloorSummary> map = new TreeMap<>();
        for (Integer layerY : layers.descendingKeySet()) {
            if (layers.get(layerY)[x][z] != null) map.put(layerY, layers.get(layerY)[x][z]);
        }
        return map;
    }

    public TreeMap<Integer, @Nullable FloorSummary> getLayers(int x, int z) {
        TreeMap<Integer, FloorSummary> map = new TreeMap<>();
        for (Integer layerY : layers.descendingKeySet()) {
            map.put(layerY, layers.get(layerY)[x][z]);
        }
        return map;
    }
}
