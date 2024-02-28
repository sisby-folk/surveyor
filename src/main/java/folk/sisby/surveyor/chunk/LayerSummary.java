package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.util.ArrayUtil;
import folk.sisby.surveyor.util.UIntArray;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class LayerSummary {
    public static final String KEY_DEPTH = "depth";
    public static final String KEY_BIOME = "biome";
    public static final String KEY_BLOCK = "block";
    public static final String KEY_LIGHT = "light";
    public static final String KEY_WATER = "water";

    public static final int DEPTH_DEFAULT = -1;
    public static final int BIOME_DEFAULT = 0;
    public static final int BLOCK_DEFAULT = 0;
    public static final int LIGHT_DEFAULT = 0;
    public static final int WATER_DEFAULT = 0;

    public static final int[] BIOME_DEFAULT_ARRAY = ArrayUtil.ofSingle(BIOME_DEFAULT, 256);
    public static final int[] BLOCK_DEFAULT_ARRAY = ArrayUtil.ofSingle(BLOCK_DEFAULT, 256);
    public static final int[] LIGHT_DEFAULT_ARRAY = ArrayUtil.ofSingle(LIGHT_DEFAULT, 256);
    public static final int[] WATER_DEFAULT_ARRAY = ArrayUtil.ofSingle(WATER_DEFAULT, 256);

    protected final @NotNull UIntArray depth; // Null Mask
    protected final @Nullable UIntArray biome;
    protected final @Nullable UIntArray block;
    protected final @Nullable UIntArray light;
    protected final @Nullable UIntArray water;

    protected LayerSummary(@NotNull UIntArray depth, @Nullable UIntArray biome, @Nullable UIntArray block, @Nullable UIntArray light, @Nullable UIntArray water) {
        this.depth = depth;
        this.biome = biome;
        this.block = block;
        this.light = light;
        this.water = water;
    }

    public static <T> int idOrAdd(Int2ObjectBiMap<T> palette, T value) {
        int id = palette.getRawId(value);
        return id == -1 ? palette.add(value) : id;
    }

    public static LayerSummary fromSummaries(FloorSummary[][] floorSummaries, int layerY, Int2ObjectBiMap<Biome> biomePalette, Int2ObjectBiMap<Block> blockPalette) {
        UIntArray depth = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).mapToInt(f -> f == null ? -1 : layerY - f.y()).toArray(), DEPTH_DEFAULT);
        if (depth == null) return null;
        UIntArray biome = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::biome).mapToInt(b -> idOrAdd(biomePalette, b)).toArray(), BIOME_DEFAULT);
        UIntArray block = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::block).mapToInt(b -> idOrAdd(blockPalette, b)).toArray(), BLOCK_DEFAULT);
        UIntArray light = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::lightLevel).toArray(), LIGHT_DEFAULT);
        UIntArray fluid = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::fluidDepth).toArray(), WATER_DEFAULT);
        return new LayerSummary(depth, biome, block, light, fluid);
    }

    public static LayerSummary fromNbt(NbtCompound nbt) {
        UIntArray depth = UIntArray.readNbt(nbt.get(KEY_DEPTH), DEPTH_DEFAULT);
        if (depth == null) return null;
        UIntArray biome = UIntArray.readNbt(nbt.get(KEY_BIOME), BIOME_DEFAULT);
        UIntArray block = UIntArray.readNbt(nbt.get(KEY_BLOCK), BLOCK_DEFAULT);
        UIntArray light = UIntArray.readNbt(nbt.get(KEY_LIGHT), LIGHT_DEFAULT);
        UIntArray water = UIntArray.readNbt(nbt.get(KEY_WATER), WATER_DEFAULT);
        return new LayerSummary(depth, biome, block, light, water);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        this.depth.writeNbt(nbt, KEY_DEPTH);
        if (biome != null) this.biome.writeNbt(nbt, KEY_BIOME);
        if (block != null) this.block.writeNbt(nbt, KEY_BLOCK);
        if (light != null) this.light.writeNbt(nbt, KEY_LIGHT);
        if (water != null) this.water.writeNbt(nbt, KEY_WATER);
        return nbt;
    }

    public boolean isEmpty(int x, int z) {
        return depth.isEmpty(x * 16 + z);
    }

    public int getDepth(int x, int z) {
        return depth.get(x * 16 + z);
    }

    public int getBiome(int x, int z) {
        return biome == null ? BIOME_DEFAULT : isEmpty(x, z) ? -1 : biome.getMasked(depth, x * 16 + z);
    }

    public int getBlock(int x, int z) {
        return block == null ? BLOCK_DEFAULT : isEmpty(x, z) ? -1 : block.getMasked(depth, x * 16 + z);
    }

    public int getLight(int x, int z) {
        return light == null ? LIGHT_DEFAULT : isEmpty(x, z) ? -1 : light.getMasked(depth, x * 16 + z);
    }

    public int getWater(int x, int z) {
        return water == null ? WATER_DEFAULT : isEmpty(x, z) ? -1 : water.getMasked(depth, x * 16 + z);
    }

    public int getHeight(int x, int z, int layerHeight, boolean ignoreWater) {
        return layerHeight - getDepth(x, z) + (ignoreWater ? 0 : getWater(x, z));
    }

    public Biome getBiome(int x, int z, IndexedIterable<Biome> biomePalette) {
        return biomePalette.get(getBiome(x, z));
    }

    public Block getBlock(int x, int z, IndexedIterable<Block> blockPalette, boolean ignoreWater) {
        return (ignoreWater || getWater(x, z) == 0) ? blockPalette.get(getBlock(x, z)) : Blocks.WATER;
    }

    public int[] rawDepths() {
        return depth.getUncompressed();
    }

    public int[] rawBiomes() {
        return biome == null ? BIOME_DEFAULT_ARRAY : biome.getUnmasked(depth);
    }

    public int[] rawBlocks() {
        return block == null ? BLOCK_DEFAULT_ARRAY : block.getUnmasked(depth);
    }

    public int[] rawLightLevels() {
        return light == null ? LIGHT_DEFAULT_ARRAY : light.getUnmasked(depth);
    }

    public int[] getWaterDepths() {
        return water == null ? WATER_DEFAULT_ARRAY : water.getUnmasked(depth);
    }

    public void fillEmptyFloors(int heightOffset, int minDepth, int maxDepth, int[] outHeight, int[] outBiome, int[] outBlock, int[] outLight, int[] outWater) {
        int[] depthFull = rawDepths();
        int[] biomeFull = rawBiomes();
        int[] blockFull = rawBlocks();
        int[] lightFull = rawLightLevels();
        int[] waterFull = getWaterDepths();
        for (int i = 0; i < 256; i++) {
            if (outHeight[i] == -1 && depthFull[i] != -1 && depthFull[i] <= maxDepth && depthFull[i] >= minDepth) {
                outHeight[i] = depthFull[i] + heightOffset;
                outBiome[i] = biomeFull[i];
                outBlock[i] = blockFull[i];
                outLight[i] = lightFull[i];
                outWater[i] = waterFull[i];
            }
        }
    }
}
