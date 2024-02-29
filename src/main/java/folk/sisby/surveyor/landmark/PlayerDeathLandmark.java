package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record PlayerDeathLandmark(BlockPos pos, UUID owner, Text name, long created, int seed) implements EventLandmark<PlayerDeathLandmark>, FlavourLandmark<PlayerDeathLandmark> {
    public static LandmarkType<PlayerDeathLandmark> TYPE = new SimpleLandmarkType<>(
        new Identifier(Surveyor.ID, "player_death"),
        pos -> RecordCodecBuilder.create(instance -> instance.group(
            Uuids.CODEC.fieldOf("owner").forGetter(Landmark::owner),
            Codecs.TEXT.fieldOf("name").forGetter(Landmark::name),
            Codec.LONG.fieldOf("created").forGetter(EventLandmark::created),
            Codec.INT.fieldOf("seed").forGetter(FlavourLandmark::seed)
        ).apply(instance, (owner, name, created, seed) -> new PlayerDeathLandmark(pos, owner, name, created, seed)))
    );

    @Override
    public LandmarkType<PlayerDeathLandmark> type() {
        return TYPE;
    }

    @Override
    public DyeColor color() {
        return DyeColor.GRAY;
    }
}
