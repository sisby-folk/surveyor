package folk.sisby.surveyor.landmark;

import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

public interface Landmark<T extends Landmark<T>> {
    LandmarkType<T> getType();

    BlockPos getPos();

    DyeColor getColor();

    Text getDisplayName();
}
