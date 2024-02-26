package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

public record NetherPortalLandmark(BlockPos pos) implements PointOfInterestLandmark<NetherPortalLandmark> {
    public static LandmarkType<NetherPortalLandmark> TYPE = new SimpleLandmarkType<>(
            new Identifier(Surveyor.ID, "poi/nether_portal"),
            pos -> Codec.EMPTY.codec().comapFlatMap(u -> DataResult.success(new NetherPortalLandmark(pos)), u -> null)
    );

    @Override
    public LandmarkType<NetherPortalLandmark> type() {
        return TYPE;
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
