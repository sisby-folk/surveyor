package folk.sisby.surveyor.structure;

import net.minecraft.util.math.BlockBox;

import java.util.Collection;

public class StructureStartSummary {
    protected final Collection<StructurePieceSummary> children;
    protected BlockBox boundingBox;

    public StructureStartSummary(Collection<StructurePieceSummary> children) {
        this.children = children;
    }

    public BlockBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = BlockBox.encompass(children.stream().map(StructurePieceSummary::getBoundingBox)::iterator).orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox without pieces"));
        }
        return boundingBox;
    }

    public Collection<StructurePieceSummary> getChildren() {
        return children;
    }
}
