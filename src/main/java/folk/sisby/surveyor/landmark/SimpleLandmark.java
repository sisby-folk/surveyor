package folk.sisby.surveyor.landmark;

import folk.sisby.surveyor.Surveyor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SimpleLandmark() implements Landmark<SimpleLandmark> {
    public static final Identifier ID = new Identifier(Surveyor.ID, "simple");
    public static final LandmarkType<SimpleLandmark> TYPE = new Type();

    @Override
    public LandmarkType<SimpleLandmark> getType() {
        return TYPE;
    }

    @Override
    public BlockPos getPos() {
        return null;
    }

    @Override
    public DyeColor getColor() {
        return null;
    }

    @Override
    public Text getDisplayName() {
        return null;
    }

    public static class Type implements LandmarkType<SimpleLandmark> {
        @Override
        public Identifier getId() {
            return ID;
        }

        @Override
        public NbtCompound toNbt(SimpleLandmark landmark, NbtCompound nbt) {
            return null;
        }

        @Override
        public SimpleLandmark fromNbt(NbtCompound nbt) {
            return null;
        }
    }
}
