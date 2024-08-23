package folk.sisby.surveyor.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.math.BlockPos;

public class SurveyorCodecs {
	public static final Codec<BlockPos> STRINGIFIED_BLOCKPOS = Codec.STRING.comapFlatMap(
		string -> DataResult.success(new BlockPos(Integer.parseInt(string.split(",")[0]), Integer.parseInt(string.split(",")[1]), Integer.parseInt(string.split(",")[2]))),
		pos -> "%s,%s,%s".formatted(pos.getX(), pos.getY(), pos.getZ())
	).stable();
}
