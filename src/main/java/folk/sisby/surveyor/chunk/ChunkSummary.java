package folk.sisby.surveyor.chunk;

import net.minecraft.nbt.NbtCompound;

public interface ChunkSummary {
    NbtCompound writeNbt(NbtCompound nbt);
}
