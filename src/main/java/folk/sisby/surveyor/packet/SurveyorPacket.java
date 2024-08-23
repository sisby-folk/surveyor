package folk.sisby.surveyor.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

public interface SurveyorPacket {
	int MAX_PAYLOAD_SIZE = 1_048_576;

	void writeBuf(PacketByteBuf buf);

	Identifier getId();

	default Collection<PacketByteBuf> toBufs() {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		writeBuf(buf);
		return List.of(buf);
	}
}
