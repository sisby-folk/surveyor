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

    private static void softAdd(DimensionType dimension, List<Integer> layers, int y) {
        if (dimension.minY() < y && y < dimension.minY() + dimension.height()) layers.add(y);
    }

    private static int[] getSummaryLayersInternal(World world) {
        List<Integer> layers = new ArrayList<>();
        DimensionType dimension = world.getDimension();
        layers.add(dimension.minY() + dimension.height() - 1); // Layer at Max Y
        if (dimension.logicalHeight() != dimension.height()) softAdd(dimension, layers, dimension.minY() + dimension.logicalHeight() - 2); // Layer below Playable Limit
        if (dimension.minY() + dimension.height() > 256) softAdd(dimension, layers, 256); // Layer At Y=256 (assume special layer change)
        if (dimension.hasSkyLight()) softAdd(dimension, layers, world.getSeaLevel() - 2);  // Layer below sea level (assume caves underneath)
        if (dimension.minY() < 0) softAdd(dimension, layers, 0); // Layer At Y=0 (assume special layer change)
        if (world.getDimensionKey() == DimensionTypes.THE_NETHER) {
            softAdd(dimension, layers, 70); // Mid outcrops
            softAdd(dimension, layers, 40); // Lava Shores
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
