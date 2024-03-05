package folk.sisby.surveyor.player;

import net.minecraft.util.math.ChunkPos;

import java.util.BitSet;
import java.util.Map;

public interface SurveyorPlayer {
    String KEY_DATA = "surveyor";
    String KEY_EXPLORED_TERRAIN = "exploredTerrain";

    Map<ChunkPos, BitSet> surveyor$getExploredTerrain();

    void surveyor$addExploredChunk(ChunkPos pos);
}
