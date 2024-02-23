package folk.sisby.surveyor.structure;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;

public class StructureSummary {
    protected final ChunkPos pos;
    protected final RegistryKey<Structure> key;
    protected final StructureType<?> type;
    protected final Collection<StructurePieceSummary> children;
    protected BlockBox boundingBox;

    public StructureSummary(ChunkPos pos, RegistryKey<Structure> key, StructureType<?> type, Collection<StructurePieceSummary> children) {
        this.pos = pos;
        this.key = key;
        this.type = type;
        this.children = children;
    }

    public BlockBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = BlockBox.encompass(children.stream().map(StructurePieceSummary::getBoundingBox)::iterator).orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox without pieces"));
        }
        return boundingBox;
    }

    public ChunkPos getPos() {
        return pos;
    }

    public RegistryKey<Structure> getKey() {
        return key;
    }

    public StructureType<?> getType() {
        return type;
    }

    public Collection<StructurePieceSummary> getChildren() {
        return children;
    }
}
