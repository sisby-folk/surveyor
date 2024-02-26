package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.function.Function;

public record SimpleLandmarkType<T extends Landmark<T>>(Identifier id, Function<BlockPos, Codec<T>> codec) implements LandmarkType<T> {
    @Override
    public Codec<T> createCodec(BlockPos pos) {
        return codec.apply(pos);
    }
}
