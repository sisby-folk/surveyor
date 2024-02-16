package folk.sisby.surveyor.chunk;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.Int2ObjectBiMap;
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
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class ChunkSummary {
    public static final String KEY_LAYERS = "layers";

    protected final TreeMap<Integer, @Nullable LayerSummary> layers = new TreeMap<>();

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

    public ChunkSummary(World world, Chunk chunk, TreeSet<Integer> layerYs, Int2ObjectBiMap<Biome> biomePalette, Int2ObjectBiMap<Block> blockPalette) {
        TreeMap<Integer, FloorSummary[][]> uncompressedLayers = new TreeMap<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                MutableBoolean foundAir = new MutableBoolean(false);
                for (int i : layerYs.descendingSet()) {
                    if (!layerYs.first().equals(i)) uncompressedLayers.computeIfAbsent(i, k -> new FloorSummary[16][16])[x][z] = getTopFloor(world, chunk, x, i, layerYs.lower(i), z, foundAir);
                }
            }
        }
        uncompressedLayers.forEach((layerY, floors) -> this.layers.put(layerY, Arrays.stream(floors).allMatch(Objects::isNull) ? null : LayerSummary.fromSummaries(floors, layerY, biomePalette, blockPalette)));
    }

    public ChunkSummary(NbtCompound nbt) {
        NbtCompound layersCompound = nbt.getCompound(KEY_LAYERS);
        for (String key : layersCompound.getKeys()) {
            int layerY = Integer.parseInt(key);
            layers.put(layerY, LayerSummary.fromNbt(layersCompound.getCompound(key)));
        }
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound layersCompound = new NbtCompound();
        layers.forEach((layerY, layerSummary) -> {
            NbtCompound layerCompound = new NbtCompound();
            if (layerSummary != null) layerSummary.writeNbt(layerCompound);
            layersCompound.put(String.valueOf(layerY), layerCompound);
        });
        nbt.put(KEY_LAYERS, layersCompound);
        return nbt;
    }

    public @Nullable FloorSummary getTopFloor(int x, int z, IndexedIterable<Biome> biomePalette, IndexedIterable<Block> blockPalette) {
        for (Integer layerY : layers.descendingKeySet()) {
            LayerSummary layer = layers.get(layerY);
            if (layer != null && layer.getFloor(layerY, x, z, biomePalette, blockPalette) != null) return layer.getFloor(layerY, x, z, biomePalette, blockPalette);
        }
        return null;
    }

    public SortedMap<Integer, FloorSummary> getFloors(int x, int z, IndexedIterable<Biome> biomePalette, IndexedIterable<Block> blockPalette) {
        SortedMap<Integer, FloorSummary> map = new TreeMap<>();
        for (Integer layerY : layers.descendingKeySet()) {
            LayerSummary layer = layers.get(layerY);
            if (layer != null && layer.getFloor(layerY, x, z, biomePalette, blockPalette) != null) map.put(layerY, layer.getFloor(layerY, x, z, biomePalette, blockPalette));
        }
        return map;
    }

    public SortedMap<Integer, @Nullable FloorSummary> getLayers(int x, int z, IndexedIterable<Biome> biomePalette, IndexedIterable<Block> blockPalette) {
        SortedMap<Integer, FloorSummary> map = new TreeMap<>();
        for (Integer layerY : layers.descendingKeySet()) {
            LayerSummary layer = layers.get(layerY);
            map.put(layerY, layer == null ? null : layer.getFloor(layerY, x, z, biomePalette, blockPalette));
        }
        return map;
    }
}
