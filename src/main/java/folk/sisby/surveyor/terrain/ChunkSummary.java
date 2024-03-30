package folk.sisby.surveyor.terrain;

import folk.sisby.surveyor.util.ChunkUtil;
import folk.sisby.surveyor.util.uints.UInts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ChunkSummary {
    public static final int MINIMUM_AIR_DEPTH = 2;
    public static final String KEY_AIR_COUNT = "air";
    public static final String KEY_LAYERS = "layers";

    protected final Integer airCount;
    protected final TreeMap<Integer, @Nullable LayerSummary> layers = new TreeMap<>();

    public ChunkSummary(World world, Chunk chunk, int[] layerHeights, Int2ObjectBiMap<Biome> biomePalette, Int2ObjectBiMap<Integer> rawBiomePalette, Int2ObjectBiMap<Block> blockPalette, Int2ObjectBiMap<Integer> rawBlockPalette, boolean countAir) {
        this.airCount = countAir ? ChunkUtil.airCount(chunk) : null;
        LayerSummary.FloorSummary[][] layerFloors = new LayerSummary.FloorSummary[layerHeights.length - 1][256];
        ChunkSection[] rawSections = chunk.getSectionArray();
        SectionSummary[] sections = new SectionSummary[rawSections.length];
        for (int i = 0; i < rawSections.length; i++) {
            sections[i] = SectionSummary.ofSection(rawSections[i]);
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int walkspaceHeight = 0;
                int waterDepth = 0;
                for (int layerIndex = 0; layerIndex < layerHeights.length - 1; layerIndex++) {
                    Block carpetBlock = null;
                    int carpetY = Integer.MAX_VALUE;
                    LayerSummary.FloorSummary foundFloor = null;
                    for (int y = layerHeights[layerIndex]; y >= layerHeights[layerIndex + 1]; y--) {
                        int sectionIndex = chunk.getSectionIndex(y);
                        SectionSummary section = sections[sectionIndex];
                        if (section == null) {
                            int sectionBottom = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(sectionIndex));
                            walkspaceHeight += (y - sectionBottom + 1);
                            waterDepth = 0;
                            y = sectionBottom;
                            continue;
                        }
                        BlockState state = section.getBlockState(x, y, z);
                        Fluid fluid = state.getFluidState().getFluid();

                        if (!state.blocksMovement() && fluid.matchesType(Fluids.EMPTY)) {
                            if (waterDepth > 0) walkspaceHeight = 0; // Erase walkspace when air below water (weird, but possible)
                            walkspaceHeight++;
                            waterDepth = 0;
                            if (walkspaceHeight >= MINIMUM_AIR_DEPTH && state.getMapColor(world, new BlockPos(x, y, z)) != MapColor.CLEAR) {
                                carpetY = y;
                                carpetBlock = state.getBlock();
                            }
                        } else if (fluid.matchesType(Fluids.WATER) || fluid.matchesType(Fluids.FLOWING_WATER)) { // keep walkspace when traversing water
                            waterDepth++;
                        } else { // Blocks Movement or Has Non-Water Fluid.
                            if (foundFloor == null) {
                                if (carpetY == y - 1) {
                                    foundFloor = new LayerSummary.FloorSummary(carpetY, section.getBiomeEntry(x, carpetY, z, world.getBottomY(), world.getTopY()).value(), carpetBlock, world.getLightLevel(LightType.BLOCK, new BlockPos(x, carpetY - 1, z)), waterDepth);
                                } else if (walkspaceHeight >= MINIMUM_AIR_DEPTH && state.getMapColor(world, new BlockPos(x, y, z)) != MapColor.CLEAR && y > layerHeights[layerIndex + 1]) {
                                    foundFloor = new LayerSummary.FloorSummary(y, section.getBiomeEntry(x, y, z, world.getBottomY(), world.getTopY()).value(), state.getBlock(), world.getLightLevel(LightType.BLOCK, new BlockPos(x, y - 1, z)), waterDepth);
                                }
                            }
                            walkspaceHeight = 0;
                            waterDepth = 0;
                        }
                    }
                    layerFloors[layerIndex][x * 16 + z] = foundFloor;
                }
            }
        }
        for (int i = 0; i < layerFloors.length; i++) {
            this.layers.put(layerHeights[i], LayerSummary.fromSummaries(world, layerFloors[i], layerHeights[i], biomePalette, rawBiomePalette, blockPalette, rawBlockPalette));
        }
    }

    public ChunkSummary(NbtCompound nbt) {
        this.airCount = nbt.contains(KEY_AIR_COUNT) ? nbt.getInt(KEY_AIR_COUNT) : null;
        NbtCompound layersCompound = nbt.getCompound(KEY_LAYERS);
        for (String key : layersCompound.getKeys()) {
            int layerY = Integer.parseInt(key);
            layers.put(layerY, LayerSummary.fromNbt(layersCompound.getCompound(key)));
        }
    }

    public ChunkSummary(PacketByteBuf buf) {
        layers.putAll(buf.readMap(PacketByteBuf::readVarInt, (b) -> {
            if (b.readByte() == 0) {
                return null;
            } else {
                return LayerSummary.fromBuf(buf);
            }
        }));
        this.airCount = -1;
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

    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(layers, PacketByteBuf::writeVarInt, (b, summary) -> {
            if (summary == null) {
                b.writeByte(0);
            } else {
                b.writeByte(1);
                summary.writeBuf(buf);
            }
        });
    }

    public void remap(Map<Integer, Integer> biomeRemap, Map<Integer, Integer> blockRemap) {
        Map<Integer, LayerSummary> newLayers = new HashMap<>();
        layers.forEach((y, layer) -> newLayers.put(y, layer == null ? null : new LayerSummary(layer.found, layer.depth, UInts.remap(layer.biome, biomeRemap::get, LayerSummary.BIOME_DEFAULT, layer.found.cardinality()), UInts.remap(layer.block, blockRemap::get, LayerSummary.BLOCK_DEFAULT, layer.found.cardinality()), layer.light, layer.water)));
        layers.clear();
        layers.putAll(newLayers);
    }

    public Integer getAirCount() {
        return airCount;
    }

    /**
     * Gets an uncompressed layer of the topmost floor found for each X,Z column within the specified range.
     *
     * @param minY        the minimum (inclusive) height of floors to include in the layer.
     * @param maxY        the maximum (inclusive) height of floors to include in the layer.
     * @param worldHeight the maximum height of the world - or any layer height > maxY to be reused in LayerSummary#getHeight() later.
     * @return A layer summary of top floors.
     */
    public @Nullable LayerSummary.Raw toSingleLayer(Integer minY, Integer maxY, int worldHeight) {
        LayerSummary.Raw outRaw = new LayerSummary.Raw(new BitSet(256), new int[256], new int[256], new int[256], new int[256], new int[256]);
        layers.descendingMap().forEach((y, layer) -> {
            if (layer != null) {
                layer.fillEmptyFloors(
                    worldHeight - y,
                    maxY == null ? Integer.MIN_VALUE : y - maxY,
                    minY == null ? Integer.MAX_VALUE : y - minY,
                    outRaw
                );
            }
        });
        return outRaw.exists().cardinality() == 0 ? null : outRaw;
    }
}
