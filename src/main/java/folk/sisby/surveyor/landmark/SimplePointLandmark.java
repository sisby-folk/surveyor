package folk.sisby.surveyor.landmark;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

public record SimplePointLandmark(BlockPos pos, Optional<UUID> optionalOwner, Optional<DyeColor> optionalColor, Optional<Text> optionalName, Optional<Identifier> optionalTexture) implements VariableLandmark<SimplePointLandmark> {
	public static final LandmarkType<SimplePointLandmark> TYPE = new SimpleLandmarkType<>(
		Identifier.of(Surveyor.ID, "point"),
		pos -> RecordCodecBuilder.create(instance -> instance.group(
			Uuids.CODEC.optionalFieldOf("owner").forGetter(VariableLandmark::optionalOwner),
			DyeColor.CODEC.optionalFieldOf("color").orElse(null).forGetter(VariableLandmark::optionalColor),
			TextCodecs.CODEC.optionalFieldOf("name").orElse(null).forGetter(VariableLandmark::optionalName),
			Identifier.CODEC.optionalFieldOf("texture").orElse(null).forGetter(VariableLandmark::optionalTexture)
		).apply(instance, (owner, color, name, texture) -> new SimplePointLandmark(pos, owner, color, name, texture)))
	);

	public SimplePointLandmark(BlockPos pos, UUID owner, DyeColor color, Text name, Identifier texture) {
		this(pos, Optional.ofNullable(owner), Optional.ofNullable(color), Optional.ofNullable(name), Optional.ofNullable(texture));
	}

	@Override
	public LandmarkType<SimplePointLandmark> type() {
		return TYPE;
	}

}
