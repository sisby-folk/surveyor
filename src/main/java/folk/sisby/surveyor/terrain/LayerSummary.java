package folk.sisby.surveyor.terrain;

import folk.sisby.surveyor.util.PaletteUtil;
import folk.sisby.surveyor.util.uints.UInts;
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

    protected final @NotNull BitSet found;
    protected final @Nullable UInts depth;
    protected final @Nullable UInts biome;
    protected final @Nullable UInts block;
    protected final @Nullable UInts light;
    protected final @Nullable UInts water;

    protected LayerSummary(@NotNull BitSet found, @Nullable UInts depth, @Nullable UInts biome, @Nullable UInts block, @Nullable UInts light, @Nullable UInts water) {
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
        UInts depth = UInts.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(f -> layerY - f.y()).toArray(), LIGHT_DEFAULT);
        UInts biome = UInts.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::biome).mapToInt(b -> PaletteUtil.idOrAdd(biomePalette, rawBiomePalette, b, biomeRegistry)).toArray(), BIOME_DEFAULT);
        UInts block = UInts.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).map(FloorSummary::block).mapToInt(b -> PaletteUtil.idOrAdd(blockPalette, rawBlockPalette, b, blockRegistry)).toArray(), BLOCK_DEFAULT);
        UInts light = UInts.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::lightLevel).toArray(), LIGHT_DEFAULT);
        UInts fluid = UInts.fromUInts(Arrays.stream(floorSummaries).flatMap(Arrays::stream).filter(Objects::nonNull).mapToInt(FloorSummary::fluidDepth).toArray(), WATER_DEFAULT);
        return new LayerSummary(found, depth, biome, block, light, fluid);
    }

    public static LayerSummary fromNbt(NbtCompound nbt) {
        if (!nbt.contains(KEY_FOUND)) return null;
        BitSet found = BitSet.valueOf(nbt.getLongArray(KEY_FOUND));
        int cardinality = found.cardinality();
        UInts depth = UInts.readNbt(nbt.get(KEY_DEPTH), cardinality);
        UInts biome = UInts.readNbt(nbt.get(KEY_BIOME), cardinality);
        UInts block = UInts.readNbt(nbt.get(KEY_BLOCK), cardinality);
        UInts light = UInts.readNbt(nbt.get(KEY_LIGHT), cardinality);
        UInts water = UInts.readNbt(nbt.get(KEY_WATER), cardinality);
        return new LayerSummary(found, depth, biome, block, light, water);
    }

    public static LayerSummary fromBuf(PacketByteBuf buf) {
        BitSet found = buf.readBitSet(256);
        int cardinality = found.cardinality();
        return new LayerSummary(
            found,
            UInts.readBuf(buf, cardinality),
            UInts.readBuf(buf, cardinality),
            UInts.readBuf(buf, cardinality),
            UInts.readBuf(buf, cardinality),
            UInts.readBuf(buf, cardinality)
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
        UInts.writeBuf(depth, buf);
        UInts.writeBuf(biome, buf);
        UInts.writeBuf(block, buf);
        UInts.writeBuf(light, buf);
        UInts.writeBuf(water, buf);
    }

    public void fillEmptyFloors(int depthOffset, int minDepth, int maxDepth, LayerSummary.Raw outLayer) {
        int i = 0;
        for (int j = 0; j < 256; j++) {
            if (found.get(j)) {
                int floorDepth = depth == null ? DEPTH_DEFAULT : depth.get(i);
                if (!outLayer.exists.get(j) && floorDepth >= minDepth && floorDepth <= maxDepth) {
                    outLayer.exists.set(j);
                    outLayer.depths[j] = floorDepth + depthOffset;
                    outLayer.biomes[j] = biome == null ? BIOME_DEFAULT : biome.get(i);
                    outLayer.blocks[j] = block == null ? BLOCK_DEFAULT : block.get(i);
                    outLayer.lightLevels[j] = light == null ? LIGHT_DEFAULT : light.get(i);
                    outLayer.waterDepths[j] = water == null ? WATER_DEFAULT : water.get(i);
                }
                i++;
            }
        }
    }

    public record Raw(BitSet exists, int[] depths, int[] biomes, int[] blocks, int[] lightLevels, int[] waterDepths) {
    }

    public record FloorSummary(int y, Biome biome, Block block, int lightLevel, int fluidDepth) {
    }
}
