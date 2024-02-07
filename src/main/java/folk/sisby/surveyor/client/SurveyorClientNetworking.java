package folk.sisby.surveyor.client;

import folk.sisby.surveyor.network.SurveyorNetworking;
import folk.sisby.surveyor.network.s2c.S2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Function;

public class SurveyorClientNetworking {

    public static void init() {
        SurveyorNetworking.C2S_SENDER = p -> ClientPlayNetworking.send(p.getId(), p.toBuf());
    }

    private static <T extends S2CPacket> void handleClient(PacketByteBuf buf, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        MinecraftClient.getInstance().execute(() -> handler.handle(MinecraftClient.getInstance().world, packet));
    }

    public interface ClientPacketHandler<T extends S2CPacket> {
        void handle(ClientWorld clientWorld, T packet);
    }
}
