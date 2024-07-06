package folk.sisby.surveyor.terrain;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DimensionSupport {
    public static final Map<RegistryKey<World>, int[]> cache = new HashMap<>();

    private static int[] getSummaryLayersInternal(World world) {
        List<Integer> layers = new ArrayList<>();
        DimensionType dimension = world.getDimension();
        layers.add(dimension.minY() + dimension.height() - 1); // Layer at Max Y
        if (dimension.logicalHeight() != dimension.height()) layers.add(dimension.minY() + dimension.logicalHeight() - 2); // Layer below Playable Limit
        if (dimension.minY() + dimension.height() > 256) layers.add(256); // Layer At Y=256 (assume special layer change)
        if (dimension.hasSkyLight()) {  // Layer below sea level (assume caves underneath)
            int newLayer = world.getSeaLevel() - 2;
            if (newLayer > dimension.minY() && newLayer < dimension.minY() + dimension.height()) layers.add(newLayer);
        }
        if (dimension.minY() < 0) { // Layer At Y=0 (assume special layer change)
            int newLayer = 0;
            if (newLayer < dimension.minY() + dimension.height()) layers.add(newLayer);
        }
        if (world.getDimensionEntry().getKey().orElseThrow() == DimensionTypes.THE_NETHER) {
            layers.add(70); // Mid outcrops
            layers.add(40); // Lava Shores
        }
        layers.add(dimension.minY()); // End Layers at Min Y
        layers.sort(Comparator.<Integer>comparingInt(i -> i).reversed());
        int[] outLayers = new int[layers.size()];
        for (int i = 0; i < outLayers.length; i++) {
            outLayers[i] = layers.get(i);
        }
        return outLayers;
    }

    public static int[] getSummaryLayers(World world) {
        return cache.computeIfAbsent(world.getRegistryKey(), k -> getSummaryLayersInternal(world));
    }
}
