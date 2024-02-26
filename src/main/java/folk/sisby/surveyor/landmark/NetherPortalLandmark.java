package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

public record NetherPortalLandmark(BlockPos pos) implements PointOfInterestLandmark<NetherPortalLandmark> {
    public static final Identifier ID = new Identifier(Surveyor.ID, "poi/nether_portal");
    public static final Codec<NetherPortalLandmark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(NetherPortalLandmark::pos)
    ).apply(instance, NetherPortalLandmark::new));

    @Override
    public Identifier type() {
        return ID;
    }

    @Override
    public Codec<NetherPortalLandmark> codec() {
        return CODEC;
    }

    @Override
    public RegistryKey<PointOfInterestType> poiType() {
        return PointOfInterestTypes.NETHER_PORTAL;
    }

    @Override
    public DyeColor color() {
        return DyeColor.PURPLE;
    }

    @Override
    public Text name() {
        return Text.translatable("block.minecraft.nether_portal");
    }
}
