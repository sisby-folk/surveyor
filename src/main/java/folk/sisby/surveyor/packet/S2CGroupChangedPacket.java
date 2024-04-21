package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.Surveyor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record S2CGroupChangedPacket(Set<UUID> players) implements S2CPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "s2c_group_changed");

    public static S2CGroupChangedPacket read(PacketByteBuf buf) {
        return new S2CGroupChangedPacket(buf.readCollection(HashSet::new, PacketByteBuf::readUuid));
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeCollection(players, PacketByteBuf::writeUuid);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
