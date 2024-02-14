package folk.sisby.surveyor;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;
import java.util.TreeSet;

public class SurveyorConfig extends WrappedConfig {
    public final List<Integer> defaultLayers = ValueList.create(0, 60);

    public TreeSet<Integer> getLayers(World world, Chunk chunk) {
        TreeSet<Integer> set = new TreeSet<>();
        set.add(chunk.getTopY() - 1);
        set.addAll(defaultLayers.stream().filter(i -> i > chunk.getBottomY() && i < chunk.getTopY() - 1).toList());
        return set;
    }
}
