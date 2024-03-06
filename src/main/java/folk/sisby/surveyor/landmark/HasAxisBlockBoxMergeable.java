package folk.sisby.surveyor.landmark;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public interface HasAxisBlockBoxMergeable extends HasAxis, HasBlockBox {
    private static boolean tryMergeOnce(World world, WorldLandmarks landmarks) {
        Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> mergeableLandmarks = landmarks.asMap();

        for (Map<BlockPos, Landmark<?>> posMap : mergeableLandmarks.values()) {
            for (Landmark<?> genericLandmark : posMap.values()) {
                if (!(genericLandmark instanceof HasAxisBlockBoxMergeable landmark)) break;
                for (Landmark<?> genericLandmark2 : posMap.values()) {
                    if (!(genericLandmark2 instanceof HasAxisBlockBoxMergeable landmark2)) break;
                    if (genericLandmark == genericLandmark2) continue;
                    if (landmark.axis().equals(landmark2.axis())) {
                        BlockBox joined = BlockBox.encompass(List.of(landmark.box(), landmark2.box())).orElseThrow();
                        if (joined.getBlockCountX() * joined.getBlockCountY() * joined.getBlockCountZ() == landmark.box().getBlockCountX() * landmark.box().getBlockCountY() * landmark.box().getBlockCountZ() + landmark2.box().getBlockCountX() * landmark2.box().getBlockCountY() * landmark2.box().getBlockCountZ()) {
                            landmarks.remove(world, genericLandmark.type(), genericLandmark.pos());
                            landmarks.remove(world, genericLandmark2.type(), genericLandmark2.pos());
                            landmark.box().encompass(landmark2.box());
                            landmarks.put(world, genericLandmark);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    default void tryMerge(World world, WorldLandmarks landmarks) {
        while (tryMergeOnce(world, landmarks)) {}
    }
}
