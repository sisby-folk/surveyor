package folk.sisby.surveyor.network.s2c;

import folk.sisby.surveyor.network.SurveyorPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public interface S2CPacket extends SurveyorPacket {
    default void send(ServerPlayerEntity playerEntity) {
        ServerPlayNetworking.send(playerEntity, getId(), toBuf());
    }

    default void send(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, getId(), toBuf());
        }
    }

    default void send(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, getId(), toBuf());
        }
    }
}
