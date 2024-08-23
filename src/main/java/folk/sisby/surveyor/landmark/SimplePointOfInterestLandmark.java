package folk.sisby.surveyor.landmark;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestType;

public record SimplePointOfInterestLandmark(BlockPos pos, RegistryKey<PointOfInterestType> poiType, DyeColor color, Text name, Identifier texture) implements Landmark<SimplePointOfInterestLandmark>, HasPoiType {
	public static final LandmarkType<SimplePointOfInterestLandmark> TYPE = new SimpleLandmarkType<>(
		Identifier.of(Surveyor.ID, "poi"),
		pos -> RecordCodecBuilder.create(instance -> instance.group(
			RegistryKey.createCodec(RegistryKeys.POINT_OF_INTEREST_TYPE).fieldOf("poiType").forGetter(HasPoiType::poiType),
			DyeColor.CODEC.fieldOf("color").orElse(null).forGetter(Landmark::color),
			TextCodecs.CODEC.fieldOf("name").orElse(null).forGetter(Landmark::name),
			Identifier.CODEC.fieldOf("texture").orElse(null).forGetter(Landmark::texture)
		).apply(instance, (poiType, color, name, texture) -> new SimplePointOfInterestLandmark(pos, poiType, color, name, texture)))
	);

	@Override
	public LandmarkType<SimplePointOfInterestLandmark> type() {
		return TYPE;
	}
}
