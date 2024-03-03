package folk.sisby.surveyor.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

public interface SurveyorPacket {
    void writeBuf(PacketByteBuf buf);

    Identifier getId();

    default Collection<PacketByteBuf> toBufs() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        writeBuf(buf);
        return List.of(buf);
    }
}
