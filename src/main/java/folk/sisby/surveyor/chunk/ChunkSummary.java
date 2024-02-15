package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.util.NbtUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ChunkSummary {
    private static final String KEY_LAYERS = "layers";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_BIOME = "biome";
    private static final String KEY_BLOCK = "block";
    private static final String KEY_LIGHT = "light";

    protected final Map<Integer, FloorSummary[][]> layers = new HashMap<>();

    public static FloorSummary getFloorBetween(World world, Chunk chunk, int x, int topY, int bottomY, int z, MutableBoolean foundAir) {
        ChunkSection[] chunkSections = chunk.getSectionArray();
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
            if (foundAir.isFalse() && chunkSections[sectionIndex].getBlockState(x & 15, y & 15, z & 15).isAir()) {
                foundAir.setTrue();
                continue;
            }
            if (foundAir.isTrue()) {
                BlockState state = chunkSections[sectionIndex].getBlockState(x & 15, y & 15, z & 15);
                if (state.blocksMovement() || !state.getFluidState().isEmpty()) {
                    foundAir.setFalse(); // We technically lose definition of floors at the top of the next scan by stopping here, but that's much less important when we actually found a hit.
                    return new FloorSummary(y, chunkSections[sectionIndex].getBiome(x & 3, y & 3, z & 3).value(), state.getBlock(), world.getLightLevel(LightType.BLOCK, new BlockPos(x, y, z)));
                }
            }
        }
        return null;
    }

    public ChunkSummary(World world, Chunk chunk, TreeSet<Integer> layerTops) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                MutableBoolean foundAir = new MutableBoolean(false);
                for (int i : layerTops.descendingSet()) {
                    layers.computeIfAbsent(i, k -> new FloorSummary[16][16])[x][z] = getFloorBetween(world, chunk, x, i, !layerTops.first().equals(i) ? layerTops.lower(i) : chunk.getBottomY(), z, foundAir);
                }
            }
        }
    }

    public ChunkSummary(NbtCompound nbt, List<Biome> biomePalette, List<Block> blockPalette) {
        NbtCompound layersCompound = nbt.getCompound(KEY_LAYERS);
        for (String key : layersCompound.getKeys()) {
            int layerY = Integer.parseInt(key);
            FloorSummary[][] layer = new FloorSummary[16][16];
            NbtCompound layerCompound = layersCompound.getCompound(key);
            int[] heightArray = NbtUtil.readOptionalUInts(layerCompound.get(KEY_HEIGHT));
            int[] biomeArray = NbtUtil.readOptionalUInts(layerCompound.get(KEY_BIOME));
            int[] blockArray = NbtUtil.readOptionalUInts(layerCompound.get(KEY_BLOCK));
            int[] lightArray = NbtUtil.readOptionalUInts(layerCompound.get(KEY_LIGHT));
            for (int i = 0; i < 255; i++) {
                layer[i / 16][i % 16] = heightArray[i] == -1 ? null : new FloorSummary(layerY - heightArray[i], biomePalette.get(biomeArray[i]), blockPalette.get(blockArray[i]), lightArray[i]);
            }
            layers.put(layerY, layer);
        }
    }

    public NbtCompound writeNbt(NbtCompound nbt, List<Biome> biomePalette, List<Block> blockPalette) {
        NbtCompound layersCompound = new NbtCompound();
        layers.forEach((layerY, floorSummaries) -> {
            NbtCompound layerCompound = new NbtCompound();
            NbtUtil.writeOptionalUInts(layerCompound, KEY_HEIGHT, Arrays.stream(floorSummaries).flatMap(Arrays::stream).mapToInt(f -> f == null ? layerY + 1 : f.y()).map(i -> layerY - i).toArray());
            NbtUtil.writeOptionalUInts(layerCompound, KEY_BIOME, Arrays.stream(floorSummaries).flatMap(Arrays::stream).mapToInt(f -> f == null ? -1 : biomePalette.indexOf(f.biome())).toArray());
            NbtUtil.writeOptionalUInts(layerCompound, KEY_BLOCK, Arrays.stream(floorSummaries).flatMap(Arrays::stream).mapToInt(f -> f == null ? -1 : blockPalette.indexOf(f.block())).toArray());
            NbtUtil.writeOptionalUInts(layerCompound, KEY_LIGHT, Arrays.stream(floorSummaries).flatMap(Arrays::stream).mapToInt(f -> f == null ? -1 : f.lightLevel()).toArray());
            layersCompound.put(String.valueOf(layerY), layerCompound);
        });
        nbt.put(KEY_LAYERS, layersCompound);
        return nbt;
    }
}
