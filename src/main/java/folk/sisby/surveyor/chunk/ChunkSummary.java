package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.util.SimplePalette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class ChunkSummary {
    public static final int MINIMUM_AIR_DEPTH = 2;
    public static final String KEY_LAYERS = "layers";

    protected final TreeMap<Integer, @Nullable LayerSummary> layers = new TreeMap<>();

    public ChunkSummary(World world, Chunk chunk, TreeSet<Integer> layerYs, SimplePalette<Biome, ChunkPos> biomePalette, SimplePalette<Block, ChunkPos> blockPalette) {
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
                            if (state.isAir()) {
                                airDepth++;
                                waterDepth = 0;
                                continue;
                            }

                            if (state.getMapColor(world, new BlockPos(x, y, z)) != MapColor.CLEAR && (state.blocksMovement() || (!state.getFluidState().isEmpty() && !state.getFluidState().isIn(FluidTags.WATER)))) {
                                if (foundFloor == null && airDepth > MINIMUM_AIR_DEPTH) foundFloor = new FloorSummary(y, chunkSections[sectionIndex].getBiome(x & 3, y & 3, z & 3).value(), state.getBlock(), world.getLightLevel(LightType.BLOCK, new BlockPos(x, y, z)), waterDepth);
                                airDepth = 0;
                                waterDepth = 0;
                            } else if (state.getFluidState().isIn(FluidTags.WATER)) {
                                waterDepth++;
                            }
                        }
                        uncompressedLayers.computeIfAbsent(i, k -> new FloorSummary[16][16])[x][z] = foundFloor;
                    }
                }
            }
        }
        biomePalette.clearOccurrences(chunk.getPos());
        blockPalette.clearOccurrences(chunk.getPos());
        uncompressedLayers.forEach((layerY, floors) -> this.layers.put(layerY, Arrays.stream(floors).allMatch(Objects::isNull) ? null : LayerSummary.fromSummaries(floors, layerY, biomePalette, blockPalette, chunk.getPos())));
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

    public void remapBiomes(Map<Integer, Integer> mapping) {
        layers.values().stream().filter(Objects::nonNull).forEach(l -> l.remapBiomes(mapping));
    }

    public void remapBlocks(Map<Integer, Integer> mapping) {
        layers.values().stream().filter(Objects::nonNull).forEach(l -> l.remapBlocks(mapping));
    }

    public @Nullable FloorSummary getTopFloor(int x, int z, SimplePalette<Biome, ChunkPos> biomePalette, SimplePalette<Block, ChunkPos> blockPalette) {
        for (Integer layerY : layers.descendingKeySet()) {
            LayerSummary layer = layers.get(layerY);
            if (layer != null && !layer.isEmpty(x, z)) return layer.getFloor(layerY, x, z, biomePalette, blockPalette);
        }
        return null;
    }

    public SortedMap<Integer, FloorSummary> getFloors(int x, int z, SimplePalette<Biome, ChunkPos> biomePalette, SimplePalette<Block, ChunkPos> blockPalette) {
        SortedMap<Integer, FloorSummary> map = new TreeMap<>();
        for (Integer layerY : layers.descendingKeySet()) {
            LayerSummary layer = layers.get(layerY);
            if (layer != null && !layer.isEmpty(x, z)) map.put(layerY, layer.getFloor(layerY, x, z, biomePalette, blockPalette));
        }
        return map;
    }

    public SortedMap<Integer, @Nullable FloorSummary> getLayers(int x, int z, SimplePalette<Biome, ChunkPos> biomePalette, SimplePalette<Block, ChunkPos> blockPalette) {
        SortedMap<Integer, FloorSummary> map = new TreeMap<>();
        for (Integer layerY : layers.descendingKeySet()) {
            LayerSummary layer = layers.get(layerY);
            map.put(layerY, layer == null ? null : layer.getFloor(layerY, x, z, biomePalette, blockPalette));
        }
        return map;
    }
}
