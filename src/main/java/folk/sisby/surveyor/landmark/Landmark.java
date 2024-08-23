package folk.sisby.surveyor.landmark;

import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface Landmark<T extends Landmark<T>> {
	LandmarkType<T> type();

	BlockPos pos();

	default @Nullable UUID owner() {
		return null;
	}

	default @Nullable DyeColor color() {
		return null;
	}

	default @Nullable Text name() {
		return null;
	}

	default @Nullable Identifier texture() {
		return null;
	}

	default Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> put(Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed, World world, WorldLandmarks landmarks) {
		return landmarks.putForBatch(changed, this);
	}

	default Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> remove(Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed, World world, WorldLandmarks landmarks) {
		return landmarks.removeForBatch(changed, this.type(), this.pos());
	}
}
