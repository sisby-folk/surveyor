package folk.sisby.surveyor.landmark;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record SimplePointLandmark(BlockPos pos, UUID owner, DyeColor color, Text name, Identifier texture) implements Landmark<SimplePointLandmark> {
    public static final LandmarkType<SimplePointLandmark> TYPE = new SimpleLandmarkType<>(
            new Identifier(Surveyor.ID, "point"),
            pos -> RecordCodecBuilder.create(instance -> instance.group(
                    Uuids.CODEC.fieldOf("owner").orElse(null).forGetter(Landmark::owner),
                    DyeColor.CODEC.fieldOf("color").orElse(null).forGetter(Landmark::color),
                    Codecs.TEXT.fieldOf("name").orElse(null).forGetter(Landmark::name),
                    Identifier.CODEC.fieldOf("texture").orElse(null).forGetter(Landmark::texture)
            ).apply(instance, (owner, color, name, texture) -> new SimplePointLandmark(pos, owner, color, name, texture)))
    );

    @Override
    public LandmarkType<SimplePointLandmark> type() {
        return TYPE;
    }

}
