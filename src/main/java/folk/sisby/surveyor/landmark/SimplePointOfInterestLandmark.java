package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestType;

public record SimplePointOfInterestLandmark(BlockPos pos, RegistryKey<PointOfInterestType> poiType, DyeColor color, Text name, Identifier texture) implements Landmark<SimplePointOfInterestLandmark> {
    public static final Identifier ID = new Identifier(Surveyor.ID, "poi");
    public static final Codec<SimplePointOfInterestLandmark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(SimplePointOfInterestLandmark::pos),
        RegistryKey.createCodec(RegistryKeys.POINT_OF_INTEREST_TYPE).fieldOf("poiType").forGetter(SimplePointOfInterestLandmark::poiType),
        DyeColor.CODEC.fieldOf("color").orElse(null).forGetter(SimplePointOfInterestLandmark::color),
        Codecs.TEXT.fieldOf("name").orElse(null).forGetter(SimplePointOfInterestLandmark::name),
        Identifier.CODEC.fieldOf("texture").orElse(null).forGetter(SimplePointOfInterestLandmark::texture)
    ).apply(instance, SimplePointOfInterestLandmark::new));

    @Override
    public Identifier type() {
        return ID;
    }

    @Override
    public Codec<SimplePointOfInterestLandmark> codec() {
        return CODEC;
    }

    enum FallbackEncoder implements Encoder<PointOfInterestLandmark<?>> {
        INSTANCE;

        @Override
        public <T> DataResult<T> encode(PointOfInterestLandmark<?> input, DynamicOps<T> ops, T prefix) {
            return ops.mapBuilder()
                .add(ops.createString("type"), Codec.STRING.encode(ID.toString(), ops, prefix))
                .add(ops.createString("poiType"), RegistryKey.createCodec(RegistryKeys.POINT_OF_INTEREST_TYPE).encode(input.poiType(), ops, prefix))
                .add(ops.createString("pos"), BlockPos.CODEC.encode(input.pos(), ops, prefix))
                .add(ops.createString("color"), DyeColor.CODEC.encode(input.color(), ops, prefix))
                .add(ops.createString("name"), Codecs.TEXT.encode(input.name(), ops, prefix))
                .add(ops.createString("texture"), Identifier.CODEC.encode(input.texture(), ops, prefix))
                .build(prefix);
        }
    }
}
