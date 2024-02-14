package folk.sisby.surveyor;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class SurveyorConfig extends WrappedConfig {
    public final Map<String, Integer> lowCeilings = ValueMap.builder(0)
        .put("minecraft:the_nether", 127)
        .build();

    public final List<Integer> defaultLayers = ValueList.create(0, 60);

    public TreeSet<Integer> getLayers(World world, Chunk chunk) {
        TreeSet<Integer> set = new TreeSet<>();
        Identifier worldId = world.getRegistryKey().getValue();
        int top = worldId != null && lowCeilings.containsKey(worldId.toString()) ? Math.min(lowCeilings.get(worldId.toString()), world.getTopY() - 1) : world.getTopY() - 1;
        set.add(top);
        set.addAll(defaultLayers.stream().filter(i -> i > chunk.getBottomY() && i < top).toList());
        return set;
    }
}
