package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.util.UIntArray;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class LayerSummary {
    public static final String KEY_DEPTH = "depth";
    public static final String KEY_BIOME = "biome";
    public static final String KEY_BLOCK = "block";
    public static final String KEY_LIGHT = "light";
    public static final String KEY_WATER = "water";

    protected final UIntArray depth; // Null Mask
    protected final UIntArray biome;
    protected final UIntArray block;
    protected final UIntArray light;
    protected final UIntArray water;

    protected LayerSummary(UIntArray depth, UIntArray biome, UIntArray block, UIntArray light, UIntArray water) {
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
        UIntArray depth = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).mapToInt(f -> f == null ? -1 : layerY - f.y()).toArray());
        if (depth == null) return null;
        UIntArray biome = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::biome).mapToInt(b -> idOrAdd(biomePalette, b)).toArray());
        UIntArray block = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::block).mapToInt(b -> idOrAdd(blockPalette, b)).toArray());
        UIntArray light = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::lightLevel).toArray());
        UIntArray fluid = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::fluidDepth).toArray());
        return new LayerSummary(depth, biome, block, light, fluid);
    }

    public static LayerSummary fromNbt(NbtCompound nbt) {
        UIntArray depth = UIntArray.readNbt(nbt.get(KEY_DEPTH));
        if (depth == null) return null;
        UIntArray biome = UIntArray.readNbt(nbt.get(KEY_BIOME));
        UIntArray block = UIntArray.readNbt(nbt.get(KEY_BLOCK));
        UIntArray light = UIntArray.readNbt(nbt.get(KEY_LIGHT));
        UIntArray water = UIntArray.readNbt(nbt.get(KEY_WATER));
        return new LayerSummary(depth, biome, block, light, water);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        this.depth.writeNbt(nbt, KEY_DEPTH);
        this.biome.writeNbt(nbt, KEY_BIOME);
        this.block.writeNbt(nbt, KEY_BLOCK);
        this.light.writeNbt(nbt, KEY_LIGHT);
        this.water.writeNbt(nbt, KEY_WATER);
        return nbt;
    }

    public @Nullable FloorSummary getFloor(int layerY, int x, int z, IndexedIterable<Biome> biomePalette, IndexedIterable<Block> blockPalette) {
        int i = x * 16 + z;
        if (!depth.isEmpty(i)) return new FloorSummary(layerY + depth.getMasked(depth, i), biomePalette.get(biome.getMasked(depth, i)), blockPalette.get(block.getMasked(depth, i)), light.getMasked(depth, i), water.getMasked(depth, i));
        return null;
    }
}
