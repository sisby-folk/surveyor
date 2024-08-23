package folk.sisby.surveyor.structure;

import net.minecraft.util.math.BlockBox;

import java.util.List;

public class StructureStartSummary {
	protected final List<StructurePieceSummary> children;
	protected BlockBox boundingBox;

	public StructureStartSummary(List<StructurePieceSummary> children) {
		this.children = children;
	}

	public BlockBox getBoundingBox() {
		if (boundingBox == null) {
			boundingBox = BlockBox.encompass(children.stream().map(StructurePieceSummary::getBoundingBox)::iterator).orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox without pieces"));
		}
		return boundingBox;
	}

	public List<StructurePieceSummary> getChildren() {
		return children;
	}
}
