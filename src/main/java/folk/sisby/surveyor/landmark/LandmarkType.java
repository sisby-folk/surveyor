package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public interface LandmarkType<T extends Landmark<T>> {
    Identifier id();

    Codec<T> createCodec(BlockPos pos);
}
