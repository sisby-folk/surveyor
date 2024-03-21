package folk.sisby.surveyor.player;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.Map;

public interface SurveyorPlayer {
    String KEY_DATA = "surveyor";
    String KEY_EXPLORED_TERRAIN = "exploredTerrain";
    String KEY_EXPLORED_STRUCTURES = "exploredStructures";

    Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$getExploredTerrain();

    Map<RegistryKey<World>, Map<Structure, LongSet>> surveyor$getExploredStructures();

    int surveyor$getViewDistance();

    void surveyor$addExploredChunk(ChunkPos pos);

    void surveyor$addExploredStructure(Structure structure, ChunkPos pos);
}
