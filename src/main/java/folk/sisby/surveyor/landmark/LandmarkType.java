package folk.sisby.surveyor.landmark;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public interface LandmarkType<T extends Landmark<T>> {
    Identifier getId();

    NbtCompound toNbt(T landmark, NbtCompound nbt);

    T fromNbt(NbtCompound nbt);
}
