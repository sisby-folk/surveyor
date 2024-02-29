package folk.sisby.surveyor.terrain;

import net.minecraft.block.Block;
import net.minecraft.world.biome.Biome;

public record FloorSummary(int y, Biome biome, Block block, int lightLevel, int fluidDepth) {
}
