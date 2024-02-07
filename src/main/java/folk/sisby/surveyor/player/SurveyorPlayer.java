package folk.sisby.surveyor.player;

import net.minecraft.util.math.ChunkPos;

import java.util.Set;

public interface SurveyorPlayer {
    String KEY_DATA = "surveyor";
    String KEY_EXPLORED_CHUNKS = "exploredChunks";

    Set<ChunkPos> surveyor$getExploredChunks();

    void surveyor$addExploredChunk(ChunkPos pos);
}
