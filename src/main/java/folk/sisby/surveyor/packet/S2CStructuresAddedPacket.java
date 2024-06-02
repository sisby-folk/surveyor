package folk.sisby.surveyor.packet;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.structure.StructureStartSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.util.MapUtil;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.List;
import java.util.Map;

public record S2CStructuresAddedPacket(boolean shared, Map<RegistryKey<Structure>, Map<ChunkPos, StructureStartSummary>> structures, Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> types, Multimap<RegistryKey<Structure>, TagKey<Structure>> tags) implements S2CPacket {
    public static final CustomPayload.Id<S2CStructuresAddedPacket> ID = new CustomPayload.Id<>(Identifier.of(Surveyor.ID, "s2c_structures_added"));
    public static final PacketCodec<RegistryByteBuf, S2CStructuresAddedPacket> CODEC = PacketCodec.tuple(
        PacketCodecs.BOOL, S2CStructuresAddedPacket::shared,
        SurveyorPacketCodecs.STRUCTURE_SUMMARIES, S2CStructuresAddedPacket::structures,
        SurveyorPacketCodecs.STRUCTURE_TYPES, S2CStructuresAddedPacket::types,
        SurveyorPacketCodecs.STRUCTURE_TAGS, S2CStructuresAddedPacket::tags,
        S2CStructuresAddedPacket::new
    );

    public static S2CStructuresAddedPacket of(boolean shared, Multimap<RegistryKey<Structure>, ChunkPos> keys, WorldStructureSummary summary) {
        return summary.createUpdatePacket(shared, keys);
    }

    public static S2CStructuresAddedPacket of(boolean shared, RegistryKey<Structure> key, ChunkPos pos, WorldStructureSummary summary) {
        return of(shared, MapUtil.asMultiMap(Map.of(key, List.of(pos))), summary);
    }

    @Override
    public CustomPayload.Id<S2CStructuresAddedPacket> getId() {
        return ID;
    }
}
