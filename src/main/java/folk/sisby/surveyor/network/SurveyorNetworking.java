package folk.sisby.surveyor.network;

import folk.sisby.surveyor.network.c2s.C2SPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.function.Consumer;
import java.util.function.Function;

public class SurveyorNetworking {
    public static Consumer<C2SPacket> C2S_SENDER = p -> {};

    public static void init() {
    }

    private static <T extends C2SPacket> void handleServer(ServerPlayerEntity player, PacketByteBuf buf, Function<PacketByteBuf, T> reader, ServerPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        handler.handle(player.getServerWorld(), packet);
    }

    public interface ServerPacketHandler<T extends C2SPacket> {
        void handle(ServerWorld world, T packet);
    }
}
