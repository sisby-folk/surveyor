package folk.sisby.surveyor.terrain;

import folk.sisby.surveyor.util.ArrayUtil;
import folk.sisby.surveyor.util.PaletteUtil;
import folk.sisby.surveyor.util.uints.UIntArray;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

public class LayerSummary {
    public static final String KEY_FOUND = "found";
    public static final String KEY_DEPTH = "depth";
    public static final String KEY_BIOME = "biome";
    public static final String KEY_BLOCK = "block";
    public static final String KEY_LIGHT = "light";
    public static final String KEY_WATER = "water";

    public static final int DEPTH_DEFAULT = 0;
    public static final int BIOME_DEFAULT = 0;
    public static final int BLOCK_DEFAULT = 0;
    public static final int LIGHT_DEFAULT = 0;
    public static final int WATER_DEFAULT = 0;

    public static final int[] DEPTH_DEFAULT_ARRAY = ArrayUtil.ofSingle(DEPTH_DEFAULT, 256);
    public static final int[] BIOME_DEFAULT_ARRAY = ArrayUtil.ofSingle(BIOME_DEFAULT, 256);
    public static final int[] BLOCK_DEFAULT_ARRAY = ArrayUtil.ofSingle(BLOCK_DEFAULT, 256);
    public static final int[] LIGHT_DEFAULT_ARRAY = ArrayUtil.ofSingle(LIGHT_DEFAULT, 256);
    public static final int[] WATER_DEFAULT_ARRAY = ArrayUtil.ofSingle(WATER_DEFAULT, 256);

    protected final @NotNull BitSet found;
    protected final @Nullable UIntArray depth;
    protected final @Nullable UIntArray biome;
    protected final @Nullable UIntArray block;
    protected final @Nullable UIntArray light;
    protected final @Nullable UIntArray water;

    protected LayerSummary(@NotNull BitSet found, @Nullable UIntArray depth, @Nullable UIntArray biome, @Nullable UIntArray block, @Nullable UIntArray light, @Nullable UIntArray water) {
        this.found = found;
        this.depth = depth;
        this.biome = biome;
        this.block = block;
        this.light = light;
        this.water = water;
    }

    public static LayerSummary fromSummaries(World world, FloorSummary[][] floorSummaries, int layerY, Int2ObjectBiMap<Biome> biomePalette, Int2ObjectBiMap<Integer> rawBiomePalette, Int2ObjectBiMap<Block> blockPalette, Int2ObjectBiMap<Integer> rawBlockPalette) {
        Registry<Biome> biomeRegistry = world.getRegistryManager().get(RegistryKeys.BIOME);
        Registry<Block> blockRegistry = world.getRegistryManager().get(RegistryKeys.BLOCK);
        BitSet found = new BitSet(256);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                found.set(x * 16 + z, floorSummaries[x][z] != null);
            }
        }
        if (found.cardinality() == 0) return null;
        UIntArray depth = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(f -> layerY - f.y()).toArray(), LIGHT_DEFAULT);
        UIntArray biome = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::biome).mapToInt(b -> PaletteUtil.idOrAdd(biomePalette, rawBiomePalette, b, biomeRegistry)).toArray(), BIOME_DEFAULT);
        UIntArray block = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::block).mapToInt(b -> PaletteUtil.idOrAdd(blockPalette, rawBlockPalette, b, blockRegistry)).toArray(), BLOCK_DEFAULT);
        UIntArray light = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::lightLevel).toArray(), LIGHT_DEFAULT);
        UIntArray fluid = UIntArray.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::fluidDepth).toArray(), WATER_DEFAULT);
        return new LayerSummary(found, depth, biome, block, light, fluid);
    }

    public static LayerSummary fromNbt(NbtCompound nbt) {
        if (!nbt.contains(KEY_FOUND)) return null;
        BitSet found = BitSet.valueOf(nbt.getLongArray(KEY_FOUND));
        UIntArray depth = UIntArray.readNbt(nbt.get(KEY_DEPTH));
        UIntArray biome = UIntArray.readNbt(nbt.get(KEY_BIOME));
        UIntArray block = UIntArray.readNbt(nbt.get(KEY_BLOCK));
        UIntArray light = UIntArray.readNbt(nbt.get(KEY_LIGHT));
        UIntArray water = UIntArray.readNbt(nbt.get(KEY_WATER));
        return new LayerSummary(found, depth, biome, block, light, water);
    }

    public static LayerSummary fromBuf(PacketByteBuf buf) {
        return new LayerSummary(
            buf.readBitSet(256),
            UIntArray.readBuf(buf),
            UIntArray.readBuf(buf),
            UIntArray.readBuf(buf),
            UIntArray.readBuf(buf),
            UIntArray.readBuf(buf)
        );
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLongArray(KEY_FOUND, found.toLongArray());
        if (depth != null) this.depth.writeNbt(nbt, KEY_DEPTH);
        if (biome != null) this.biome.writeNbt(nbt, KEY_BIOME);
        if (block != null) this.block.writeNbt(nbt, KEY_BLOCK);
        if (light != null) this.light.writeNbt(nbt, KEY_LIGHT);
        if (water != null) this.water.writeNbt(nbt, KEY_WATER);
        return nbt;
    }

    public void writeBuf(PacketByteBuf buf) {
        buf.writeBitSet(found, 256);
        UIntArray.writeBuf(depth, buf);
        UIntArray.writeBuf(biome, buf);
        UIntArray.writeBuf(block, buf);
        UIntArray.writeBuf(light, buf);
        UIntArray.writeBuf(water, buf);
    }

    public int[] rawDepths() {
        return depth == null ? DEPTH_DEFAULT_ARRAY : depth.getUnmasked(found);
    }

    public int[] rawBiomes() {
        return biome == null ? BIOME_DEFAULT_ARRAY : biome.getUnmasked(found);
    }

    public int[] rawBlocks() {
        return block == null ? BLOCK_DEFAULT_ARRAY : block.getUnmasked(found);
    }

    public int[] rawLightLevels() {
        return light == null ? LIGHT_DEFAULT_ARRAY : light.getUnmasked(found);
    }

    public int[] rawWaterDepths() {
        return water == null ? WATER_DEFAULT_ARRAY : water.getUnmasked(found);
    }

    public void fillEmptyFloors(int depthOffset, int minDepth, int maxDepth, BitSet outFound, int[] outDepth, int[] outBiome, int[] outBlock, int[] outLight, int[] outWater) {
        int[] depthFull = rawDepths();
        int[] biomeFull = rawBiomes();
        int[] blockFull = rawBlocks();
        int[] lightFull = rawLightLevels();
        int[] waterFull = rawWaterDepths();
        for (int i = 0; i < 256; i++) {
            if (!outFound.get(i) && found.get(i) && depthFull[i] >= minDepth && depthFull[i] <= maxDepth) {
                outFound.set(i);
                outDepth[i] = depthFull[i] + depthOffset;
                outBiome[i] = biomeFull[i];
                outBlock[i] = blockFull[i];
                outLight[i] = lightFull[i];
                outWater[i] = waterFull[i];
            }
        }
    }

    public record Raw(BitSet exists, int[] depths, int[] biomes, int[] blocks, int[] lightLevels, int[] waterDepths) { }

    public record FloorSummary(int y, Biome biome, Block block, int lightLevel, int fluidDepth) { }
}
