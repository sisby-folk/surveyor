package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.util.UIntArray;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class LayerSummary {
    public static final String KEY_DEPTH = "depth";
    public static final String KEY_BIOME = "biome";
    public static final String KEY_BLOCK = "block";
    public static final String KEY_LIGHT = "light";

    protected final UIntArray depth;
    protected final UIntArray biome;
    protected final UIntArray block;
    protected final UIntArray light;

    protected LayerSummary(UIntArray depth, UIntArray biome, UIntArray block, UIntArray light) {
        this.depth = depth;
        this.biome = biome;
        this.block = block;
        this.light = light;
    }

    public static LayerSummary fromSummaries(FloorSummary[][] floorSummaries, int layerY, Int2ObjectBiMap<Biome> biomePalette, Int2ObjectBiMap<Block> blockPalette) {
        UIntArray depth = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? -1 : layerY - f.y()).toList());
        if (depth == null) return null;
        UIntArray biome = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? null : biomePalette.getRawId(f.biome()) == -1 ? biomePalette.add(f.biome()) : biomePalette.getRawId(f.biome())).toList());
        UIntArray block = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? null : blockPalette.getRawId(f.block()) == -1 ? blockPalette.add(f.block()) : blockPalette.getRawId(f.block())).toList());
        UIntArray light = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).map(f -> f == null ? null : f.lightLevel()).toList());
        return new LayerSummary(depth, biome, block, light);
    }

    public static LayerSummary fromNbt(NbtCompound nbt) {
        UIntArray depth = UIntArray.readNbt(nbt.get(KEY_DEPTH));
        if (depth == null) return null;
        UIntArray biome = UIntArray.readNbt(nbt.get(KEY_BIOME));
        UIntArray block = UIntArray.readNbt(nbt.get(KEY_BLOCK));
        UIntArray light = UIntArray.readNbt(nbt.get(KEY_LIGHT));
        return new LayerSummary(depth, biome, block, light);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        this.depth.writeNbt(nbt, KEY_DEPTH);
        this.biome.writeNbt(nbt, KEY_BIOME);
        this.block.writeNbt(nbt, KEY_BLOCK);
        this.light.writeNbt(nbt, KEY_LIGHT);
        return nbt;
    }

    public @Nullable FloorSummary getFloor(int layerY, int x, int z, IndexedIterable<Biome> biomePalette, IndexedIterable<Block> blockPalette) {
        int i = x * 16 + z;
        if (!depth.isEmpty(i)) return new FloorSummary(layerY + depth.get(i), biomePalette.get(biome.get(i)), blockPalette.get(block.get(i)), light.get(i));
        return null;
    }
}
