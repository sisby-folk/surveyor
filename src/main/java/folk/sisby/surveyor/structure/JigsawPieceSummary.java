package folk.sisby.surveyor.structure;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.pool.FeaturePoolElement;
import net.minecraft.structure.pool.ListPoolElement;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JigsawPieceSummary extends StructurePieceSummary {
    public static final BiMap<String, StructurePoolElementType<?>> TYPE_KEYS = HashBiMap.create(Map.of(
        "single", StructurePoolElementType.SINGLE_POOL_ELEMENT,
        "feature", StructurePoolElementType.FEATURE_POOL_ELEMENT
    ));
    public static final String KEY_JUNCTIONS = "junctions";
    public static final String KEY_JUNCTION_X = "source_x";
    public static final String KEY_JUNCTION_Z = "source_z";
    public final StructurePoolElementType<?> jigsawType;
    public final Identifier id;
    public final BlockBox boundingBox;
    public final List<JigsawJunction> junctions;

    public JigsawPieceSummary(StructurePoolElementType<?> jigsawType, Identifier id, BlockBox boundingBox, List<JigsawJunction> junctions) {
        super(StructurePieceType.JIGSAW, boundingBox);
        this.jigsawType = jigsawType;
        this.id = id;
        this.boundingBox = boundingBox;
        this.junctions = junctions;
    }

    public static List<StructurePieceSummary> tryFromElement(StructurePoolElement poolElement, PoolStructurePiece piece) {
        if (poolElement instanceof ListPoolElement listElement) {
            List<StructurePieceSummary> allSummaries = new ArrayList<>();
            listElement.elements.forEach(e -> allSummaries.addAll(tryFromElement(e, piece)));
            return allSummaries;
        } else if (poolElement instanceof SinglePoolElement singleElement && singleElement.location.left().isPresent()) {
            return List.of(new JigsawPieceSummary(StructurePoolElementType.SINGLE_POOL_ELEMENT, singleElement.location.left().orElseThrow(), poolElement.getBoundingBox(piece.structureTemplateManager, piece.getPos(), piece.getRotation()), piece.getJunctions()));
        } else if (poolElement instanceof FeaturePoolElement featureElement && featureElement.feature.getKey().isPresent()) {
            return List.of(new JigsawPieceSummary(StructurePoolElementType.FEATURE_POOL_ELEMENT, featureElement.feature.getKey().orElseThrow().getValue(), poolElement.getBoundingBox(piece.structureTemplateManager, piece.getPos(), piece.getRotation()), piece.getJunctions()));
        }
        return List.of();
    }

    public static List<StructurePieceSummary> tryFromPiece(StructurePiece piece) {
        if (piece instanceof PoolStructurePiece poolPiece) {
            return tryFromElement(poolPiece.getPoolElement(), poolPiece);
        }
        return List.of();
    }

    public static JigsawPieceSummary fromNbt(NbtCompound nbt) {
        StructurePieceSummary baseSummary = StructurePieceSummary.fromNbt(nbt);
        List<JigsawJunction> junctions = new ArrayList<>();
        nbt.getList(KEY_JUNCTIONS, NbtElement.COMPOUND_TYPE).forEach(junctionElement -> junctions.add(JigsawJunction.deserialize(new Dynamic<>(NbtOps.INSTANCE, junctionElement))));

        String idKey = TYPE_KEYS.keySet().stream().filter(nbt::contains).findFirst().orElseThrow();

        return new JigsawPieceSummary(
            TYPE_KEYS.get(idKey),
            new Identifier(nbt.getString(idKey)),
            baseSummary.boundingBox,
            junctions
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        String idKey = TYPE_KEYS.inverse().get(jigsawType);
        nbt.putString(idKey, id.toString());
        NbtList junctionList = new NbtList(junctions.stream().map(j -> {
            NbtCompound junctionCompound = new NbtCompound();
            junctionCompound.putInt(KEY_JUNCTION_X, j.getSourceX());
            junctionCompound.putInt(KEY_JUNCTION_Z, j.getSourceZ());
            return (NbtElement) junctionCompound;
        }).toList(), NbtElement.COMPOUND_TYPE);
        if (!junctionList.isEmpty()) nbt.put(KEY_JUNCTIONS, junctionList);
        return nbt;
    }
}
