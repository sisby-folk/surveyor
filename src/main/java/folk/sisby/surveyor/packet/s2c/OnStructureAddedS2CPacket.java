package folk.sisby.surveyor.packet.s2c;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.structure.StructurePieceSummary;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public record OnStructureAddedS2CPacket(ChunkPos pos, RegistryKey<Structure> key, RegistryKey<StructureType<?>> type, Collection<StructurePieceSummary> pieces) implements S2CPacket {
    public OnStructureAddedS2CPacket(PacketByteBuf buf) {
        this(
            new ChunkPos(buf.readVarInt(), buf.readVarInt()),
            RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(buf.readString())),
            RegistryKey.of(RegistryKeys.STRUCTURE_TYPE, new Identifier(buf.readString())),
            buf.readCollection(ArrayList::new, b -> WorldStructureSummary.readStructurePieceNbt(Objects.requireNonNull(b.readNbt())))
        );
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeVarInt(pos.x);
        buf.writeVarInt(pos.z);
        buf.writeString(key.getValue().toString());
        buf.writeString(type.getValue().toString());
        buf.writeCollection(pieces, (b, piece) -> b.writeNbt(piece.writeNbt(new NbtCompound())));
    }

    @Override
    public Identifier getId() {
        return SurveyorNetworking.S2C_ON_STRUCTURE_ADDED;
    }
}
