package folk.sisby.surveyor.client;

import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.packet.s2c.OnJoinWorldS2CPacket;
import folk.sisby.surveyor.packet.s2c.S2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Function;

public class SurveyorClientNetworking {
    public static void init() {
        SurveyorNetworking.C2S_SENDER = p -> {
            ClientPlayNetworking.send(p.getId(), p.toBuf());
        };
        ClientPlayNetworking.registerGlobalReceiver(SurveyorNetworking.S2C_ON_JOIN_WORLD, (c, h, b, s) -> handleClient(b, OnJoinWorldS2CPacket::new, SurveyorClientNetworking::handleOnJoinWorld));
    }

    private static void handleOnJoinWorld(ClientWorld world, WorldSummary summary, OnJoinWorldS2CPacket packet) {
        packet.structures().forEach((pos, structures) -> structures.forEach((structure, pair) -> summary.putStructureSummary(pos, structure, pair.left(), pair.right())));
    }

    private static <T extends S2CPacket> void handleClient(PacketByteBuf buf, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        MinecraftClient.getInstance().execute(() -> handler.handle(MinecraftClient.getInstance().world, MinecraftClient.getInstance().world == null ? null : ((SurveyorWorld) MinecraftClient.getInstance().world).surveyor$getWorldSummary(), packet));
    }

    public interface ClientPacketHandler<T extends S2CPacket> {
        void handle(ClientWorld clientWorld, WorldSummary summary, T packet);
    }
}
