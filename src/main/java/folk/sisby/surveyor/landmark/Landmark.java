package folk.sisby.surveyor.landmark;

import com.google.common.collect.Multimap;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

    default Multimap<LandmarkType<?>, BlockPos> put(Multimap<LandmarkType<?>, BlockPos> changed, World world, WorldLandmarks landmarks) {
        return landmarks.putLocalBatched(changed, this);
    }

    default Multimap<LandmarkType<?>, BlockPos> remove(Multimap<LandmarkType<?>, BlockPos> changed, World world, WorldLandmarks landmarks) {
        return landmarks.removeLocalBatched(changed, this.type(), this.pos());
    }
}
