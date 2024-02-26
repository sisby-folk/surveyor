package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
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
    public static final Identifier ID = new Identifier(Surveyor.ID, "point");
    public static final Codec<SimplePointLandmark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(SimplePointLandmark::pos),
        Uuids.CODEC.fieldOf("owner").orElse(null).forGetter(SimplePointLandmark::owner),
        DyeColor.CODEC.fieldOf("color").orElse(null).forGetter(SimplePointLandmark::color),
        Codecs.TEXT.fieldOf("name").orElse(null).forGetter(SimplePointLandmark::name),
        Identifier.CODEC.fieldOf("texture").orElse(null).forGetter(SimplePointLandmark::texture)
    ).apply(instance, SimplePointLandmark::new));

    @Override
    public Identifier type() {
        return ID;
    }

    @Override
    public Codec<SimplePointLandmark> codec() {
        return CODEC;
    }

    enum FallbackEncoder implements Encoder<Landmark<?>> {
        INSTANCE;

        @Override
        public <T> DataResult<T> encode(Landmark<?> input, DynamicOps<T> ops, T prefix) {
            return ops.mapBuilder()
                .add(ops.createString("type"), Codec.STRING.encode(ID.toString(), ops, prefix))
                .add(ops.createString("pos"), BlockPos.CODEC.encode(input.pos(), ops, prefix))
                .add(ops.createString("color"), DyeColor.CODEC.encode(input.color(), ops, prefix))
                .add(ops.createString("name"), Codecs.TEXT.encode(input.name(), ops, prefix))
                .add(ops.createString("texture"), Identifier.CODEC.encode(input.texture(), ops, prefix))
                .build(prefix);
        }
    }
}
