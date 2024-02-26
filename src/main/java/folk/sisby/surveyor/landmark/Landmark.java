package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Encoder;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface Landmark<T extends Landmark<T>> {
    Identifier type();

    Codec<T> codec();

    default Encoder<? extends Landmark<?>> fallback() {
        return SimplePointLandmark.FallbackEncoder.INSTANCE;
    }

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
}
