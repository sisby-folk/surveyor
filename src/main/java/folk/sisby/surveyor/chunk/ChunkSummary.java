package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.util.ArrayUtil;
import folk.sisby.surveyor.util.ChunkUtil;
import folk.sisby.surveyor.util.UIntArray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;

import static folk.sisby.surveyor.chunk.LayerSummary.BIOME_DEFAULT;
import static folk.sisby.surveyor.chunk.LayerSummary.BLOCK_DEFAULT;
import static folk.sisby.surveyor.chunk.LayerSummary.DEPTH_DEFAULT;
import static folk.sisby.surveyor.chunk.LayerSummary.LIGHT_DEFAULT;
import static folk.sisby.surveyor.chunk.LayerSummary.WATER_DEFAULT;

public class ChunkSummary {
    public static final int MINIMUM_AIR_DEPTH = 2;
    public static final String KEY_AIR_COUNT = "air";
    public static final String KEY_LAYERS = "layers";

    protected final Integer airCount;
    protected final TreeMap<Integer, @Nullable LayerSummary> layers = new TreeMap<>();

    public ChunkSummary(World world, Chunk chunk, NavigableSet<Integer> layerYs, Int2ObjectBiMap<Biome> biomePalette, Int2ObjectBiMap<Block> blockPalette, boolean countAir) {
        this.airCount = countAir ? ChunkUtil.airCount(chunk) : null;
        TreeMap<Integer, FloorSummary[][]> uncompressedLayers = new TreeMap<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int airDepth = 0;
                int waterDepth = 0;
                for (int i : layerYs.descendingSet()) {
                    if (!layerYs.first().equals(i)) {
                        int bottomY = layerYs.lower(i);
                        ChunkSection[] chunkSections = chunk.getSectionArray();
                        FloorSummary foundFloor = null;
                        for (int y = i; y > bottomY; y--) {
                            int sectionIndex = chunk.getSectionIndex(y);
                            if (chunkSections[sectionIndex].isEmpty()) {
                                int chunkBottom = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(sectionIndex));
                                airDepth += (y - chunkBottom + 1);
                                waterDepth = 0;
                                y = chunkBottom;
                                continue;
                            }

                            BlockState state = chunkSections[sectionIndex].getBlockState(x & 15, y & 15, z & 15);
                            if (!state.blocksMovement()) { // The current block's air counts for space - a 2 block high walkway with a torch or grass is valid - the floor is the torch/grass.
                                airDepth++;
                            }

                            if (state.getFluidState().isIn(FluidTags.WATER)) { // Floors can't be waterlogged, otherwise it ruins depth measurement.
                                waterDepth++;
                            } else if (state.getMapColor(world, new BlockPos(x, y, z)) != MapColor.CLEAR) {
                                if (foundFloor == null && airDepth > MINIMUM_AIR_DEPTH) {
                                    foundFloor = new FloorSummary(y, chunkSections[sectionIndex].getBiome(x & 3, y & 3, z & 3).value(), state.getBlock(), world.getLightLevel(LightType.BLOCK, new BlockPos(x, y, z)), waterDepth);
                                }
                                airDepth = 0;
                                waterDepth = 0;
                            } else {
                                waterDepth = 0;
                            }
                        }
                        uncompressedLayers.computeIfAbsent(i, k -> new FloorSummary[16][16])[x][z] = foundFloor;
                    }
                }
            }
        }
        uncompressedLayers.forEach((layerY, floors) -> this.layers.put(layerY, Arrays.stream(floors).allMatch(Objects::isNull) ? null : LayerSummary.fromSummaries(floors, layerY, biomePalette, blockPalette)));
    }

    public ChunkSummary(NbtCompound nbt) {
        this.airCount = nbt.contains(KEY_AIR_COUNT) ? nbt.getInt(KEY_AIR_COUNT) : null;
        NbtCompound layersCompound = nbt.getCompound(KEY_LAYERS);
        for (String key : layersCompound.getKeys()) {
            int layerY = Integer.parseInt(key);
            layers.put(layerY, LayerSummary.fromNbt(layersCompound.getCompound(key)));
        }
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        if (this.airCount != null) nbt.putInt(KEY_AIR_COUNT, this.airCount);
        NbtCompound layersCompound = new NbtCompound();
        layers.forEach((layerY, layerSummary) -> {
            NbtCompound layerCompound = new NbtCompound();
            if (layerSummary != null) layerSummary.writeNbt(layerCompound);
            layersCompound.put(String.valueOf(layerY), layerCompound);
        });
        nbt.put(KEY_LAYERS, layersCompound);
        return nbt;
    }

    public Integer getAirCount() {
        return airCount;
    }

    /**
     * Gets a compressed layer of the topmost floor found for each X,Z column within the specified range.
     *
     * @param minY        the minimum (inclusive) height of floors to include in the layer.
     * @param maxY        the maximum (inclusive) height of floors to include in the layer.
     * @param worldHeight the maximum height of the world - or any layer height > maxY to be reused in LayerSummary#getHeight() later.
     * @return A layer summary of top floors.
     */
    public @Nullable LayerSummary toSingleLayer(Integer minY, Integer maxY, int worldHeight) {
        int[] depth = ArrayUtil.ofSingle(-1, 256);
        int[] biome = new int[256];
        int[] block = new int[256];
        int[] light = new int[256];
        int[] water = new int[256];
        layers.descendingMap().forEach((y, layer) -> {
            if (layer != null) {
                layer.fillEmptyFloors(worldHeight - y, minY == null ? Integer.MIN_VALUE : y - minY, maxY == null ? Integer.MAX_VALUE : y - maxY, depth, biome, block, light, water);
            }
        });
        UIntArray compressedDepth = UIntArray.fromUInts(depth, DEPTH_DEFAULT);
        return compressedDepth == null ? null : new LayerSummary(
            compressedDepth,
            UIntArray.fromUInts(biome, BIOME_DEFAULT),
            UIntArray.fromUInts(block, BLOCK_DEFAULT),
            UIntArray.fromUInts(light, LIGHT_DEFAULT),
            UIntArray.fromUInts(water, WATER_DEFAULT)
        );
    }
}
