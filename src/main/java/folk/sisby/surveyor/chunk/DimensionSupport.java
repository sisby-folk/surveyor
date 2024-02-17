package folk.sisby.surveyor.chunk;

import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.TreeSet;

public class DimensionSupport {
    public static TreeSet<Integer> getSummaryLayers(World world) {
        TreeSet<Integer> set = new TreeSet<>();
        DimensionType dimension = world.getDimension();
        set.add(dimension.minY() + dimension.height() - 1); // Layer at Max Y
        set.add(dimension.minY() + dimension.logicalHeight() - 1); // Layer At Playable Limit
        if (dimension.hasSkyLight()) set.add(world.getSeaLevel() - 2); // Layer below sea level (assume caves underneath)
        if (dimension.minY() + dimension.height() > 256) set.add(256); // Layer At Y=256 (assume special layer change)
        if (dimension.minY() < 0) set.add(0); // Layer At Y=0 (assume special layer change)
        if (world.getDimensionKey() == DimensionTypes.THE_NETHER) {
            set.add(70); // Mid outcrops
            set.add(40); // Lava Shores
        }
        set.add(dimension.minY()); // End Layers at Min Y
        return set;
    }
}
