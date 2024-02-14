package folk.sisby.surveyor.chunk;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ChunkSummary {
    private static final String KEY_LAYERS = "layers";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_BIOME = "biome";
    private static final String KEY_BLOCK = "block";

    protected final Map<Integer, FloorSummary[][]> layers = new HashMap<>();

    public static FloorSummary getFloorBetween(Chunk chunk, int x, int topY, int bottomY, int z, MutableBoolean foundAir) {
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
                    return new FloorSummary(y, chunkSections[sectionIndex].getBiome(x & 3, y & 3, z & 3).value(), state.getBlock());
                }
            }
        }
        return null;
    }

    public ChunkSummary(Chunk chunk, TreeSet<Integer> layerTops) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                MutableBoolean foundAir = new MutableBoolean(false);
                for (int i : layerTops.descendingSet()) {
                    layers.computeIfAbsent(i, k -> new FloorSummary[16][16])[x][z] = getFloorBetween(chunk, x, i, !layerTops.first().equals(i) ? layerTops.lower(i) : chunk.getBottomY(), z, foundAir);
                }
            }
        }
    }

    public ChunkSummary(NbtCompound nbt, List<Biome> biomePalette, List<Block> blockPalette) {
        NbtCompound layersCompound = nbt.getCompound(KEY_LAYERS);
        for (String key : layersCompound.getKeys()) {
            int y = Integer.parseInt(key);
            FloorSummary[][] layer = new FloorSummary[16][16];
            NbtCompound layerCompound = layersCompound.getCompound(key);
            int[] heightArray = layerCompound.getType(KEY_HEIGHT) == NbtElement.INT_ARRAY_TYPE ? layerCompound.getIntArray(KEY_HEIGHT) : Collections.nCopies(255, layerCompound.getInt(KEY_HEIGHT)).stream().mapToInt(i -> i).toArray();
            int[] biomeArray = layerCompound.getType(KEY_BIOME) == NbtElement.INT_ARRAY_TYPE ? layerCompound.getIntArray(KEY_BIOME) : Collections.nCopies(255, layerCompound.getInt(KEY_BIOME)).stream().mapToInt(i -> i).toArray();
            int[] blockArray = layerCompound.getType(KEY_BLOCK) == NbtElement.INT_ARRAY_TYPE ? layerCompound.getIntArray(KEY_BLOCK) : Collections.nCopies(255, layerCompound.getInt(KEY_BLOCK)).stream().mapToInt(i -> i).toArray();
            for (int i = 0; i < 255; i++) {
                layer[i / 16][i % 16] = biomeArray[i] == -1 ? null : new FloorSummary(heightArray[i], biomePalette.get(biomeArray[i]), blockPalette.get(blockArray[i]));
            }
            layers.put(y, layer);
        }
    }

    public NbtCompound writeNbt(NbtCompound nbt, List<Biome> biomePalette, List<Block> blockPalette) {
        NbtCompound layersCompound = new NbtCompound();
        layers.forEach((topY, floorSummaries) -> {
            NbtCompound layerCompound = new NbtCompound();
            NbtIntArray heightArray = new NbtIntArray(new int[]{});
            NbtIntArray biomeArray = new NbtIntArray(new int[]{});
            NbtIntArray blockArray = new NbtIntArray(new int[]{});
            for (FloorSummary summary : Arrays.stream(floorSummaries).flatMap(Arrays::stream).toList()) {
                if (summary != null) {
                    heightArray.add(NbtInt.of(summary.y()));
                    biomeArray.add(NbtInt.of(biomePalette.indexOf(summary.biome())));
                    blockArray.add(NbtInt.of(blockPalette.indexOf(summary.block())));
                } else {
                    heightArray.add(NbtInt.of(-1));
                    biomeArray.add(NbtInt.of(-1));
                    blockArray.add(NbtInt.of(-1));
                }
            }
            layerCompound.put(KEY_HEIGHT, heightArray.stream().distinct().count() == 1 ? heightArray.get(0) : heightArray);
            layerCompound.put(KEY_BIOME, biomeArray.stream().distinct().count() == 1 ? biomeArray.get(0) : biomeArray);
            layerCompound.put(KEY_BLOCK, blockArray.stream().distinct().count() == 1 ? blockArray.get(0) : blockArray);

            layersCompound.put(String.valueOf(topY), layerCompound);
        });
        nbt.put(KEY_LAYERS, layersCompound);
        return nbt;
    }
}
