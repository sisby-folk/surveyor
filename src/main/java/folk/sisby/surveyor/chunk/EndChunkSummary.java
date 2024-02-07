package folk.sisby.surveyor.chunk;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class EndChunkSummary extends OverworldChunkSummary implements ChunkSummary {
    public static final ChunkSummaryFactory<EndChunkSummary> FACTORY = new ChunkSummaryFactory<>() {
        @Override
        public EndChunkSummary fromChunk(World world, Chunk chunk) {
            return new EndChunkSummary(world, chunk);
        }

        @Override
        public EndChunkSummary fromNbt(NbtCompound nbt) {
            return new EndChunkSummary(nbt);
        }
    };

    public EndChunkSummary(World world, Chunk chunk) {
        super(world, chunk);
    }

    public EndChunkSummary(NbtCompound nbt) {
        super(nbt);
    }
}
