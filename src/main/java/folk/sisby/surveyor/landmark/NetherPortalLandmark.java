package folk.sisby.surveyor.landmark;

import com.google.common.collect.Multimap;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

public record NetherPortalLandmark(BlockBox box, Direction.Axis axis) implements Landmark<NetherPortalLandmark>, HasAxisBlockBoxMergeable, HasPoiType {
    public NetherPortalLandmark(BlockPos pos, Direction.Axis axis) {
        this(new BlockBox(pos), axis);
    }

    public static LandmarkType<NetherPortalLandmark> TYPE = new SimpleLandmarkType<>(
        Identifier.of(Surveyor.ID, "poi/nether_portal"),
        pos -> RecordCodecBuilder.create(instance -> instance.group(
            BlockBox.CODEC.fieldOf("box").forGetter(HasBlockBox::box),
            Direction.Axis.CODEC.fieldOf("axis").forGetter(HasAxis::axis)
        ).apply(instance, NetherPortalLandmark::new))
    );

    @Override
    public LandmarkType<NetherPortalLandmark> type() {
        return TYPE;
    }

    @Override
    public Multimap<LandmarkType<?>, BlockPos> put(Multimap<LandmarkType<?>, BlockPos> changes, World world, WorldLandmarks landmarks) {
        Landmark.super.put(changes, world, landmarks);
        return tryMerge(changes, world, landmarks);
    }

    @Override
    public BlockPos pos() {
        return new BlockPos(box.getMinX(), box.getMinY(), box.getMinZ());
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
