package folk.sisby.surveyor.structure;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;

public record StructureSummary(ChunkPos pos, RegistryKey<Structure> key, StructureType<?> type, Collection<StructurePieceSummary> pieces) {
}
