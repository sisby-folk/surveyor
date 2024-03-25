package folk.sisby.surveyor.packet;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.structure.StructureStartSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.util.MapUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public record S2CStructuresAddedPacket(Map<RegistryKey<Structure>, Map<ChunkPos, StructureStartSummary>> structures, Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> structureTypes, Multimap<RegistryKey<Structure>, TagKey<Structure>> structureTags) implements S2CPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "s2c_structures_added");

    public static S2CStructuresAddedPacket of(RegistryKey<Structure> key, ChunkPos pos, StructureStartSummary summary, RegistryKey<StructureType<?>> structureType, Collection<TagKey<Structure>> structureTags) {
        return new S2CStructuresAddedPacket(Map.of(key, Map.of(pos, summary)), Map.of(key, structureType), MapUtil.asMultiMap(Map.of(key, structureTags)));
    }

    public static S2CStructuresAddedPacket read(PacketByteBuf buf) {
        return new S2CStructuresAddedPacket(
            buf.readMap(
                b -> b.readRegistryKey(RegistryKeys.STRUCTURE),
                b -> b.readMap(
                    PacketByteBuf::readChunkPos,
                    b2 -> new StructureStartSummary(b2.readList(b3 -> WorldStructureSummary.readStructurePieceNbt(Objects.requireNonNull(b3.readNbt()))))
                )
            ),
            buf.readMap(
                b -> b.readRegistryKey(RegistryKeys.STRUCTURE),
                b -> b.readRegistryKey(RegistryKeys.STRUCTURE_TYPE)
            ),
            MapUtil.asMultiMap(buf.readMap(
                b -> b.readRegistryKey(RegistryKeys.STRUCTURE),
                b -> b.readList(b2 -> TagKey.of(RegistryKeys.STRUCTURE, b2.readIdentifier()))
            ))
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(structures,
            PacketByteBuf::writeRegistryKey,
            (b, posMap) -> b.writeMap(posMap,
                PacketByteBuf::writeChunkPos,
                (b2, summary) -> b2.writeCollection(summary.getChildren(), (b3, piece) -> b3.writeNbt(piece.toNbt()))
            )
        );
        buf.writeMap(structureTypes,
            PacketByteBuf::writeRegistryKey,
            PacketByteBuf::writeRegistryKey
        );
        buf.writeMap(structureTags.asMap(),
            PacketByteBuf::writeRegistryKey,
            (b, c) -> b.writeCollection(c, (b2, t) -> b2.writeIdentifier(t.id()))
        );
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
