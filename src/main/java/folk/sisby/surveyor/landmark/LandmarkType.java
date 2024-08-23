package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public interface LandmarkType<T extends Landmark<T>> {
	Codec<LandmarkType<?>> CODEC = Identifier.CODEC.xmap(Landmarks::getType, LandmarkType::id);

	Identifier id();

	Codec<T> createCodec(BlockPos pos);
}
