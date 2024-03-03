package folk.sisby.surveyor.packet;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public interface S2CPacket extends SurveyorPacket {
    default void send(ServerPlayerEntity playerEntity) {
        toBufs().forEach(buf -> ServerPlayNetworking.send(playerEntity, getId(), buf));
    }

    default void send(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            send(player);
        }
    }

    default void send(ServerPlayerEntity sender, ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player != sender) send(player);
        }
    }

    default void send(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            send(player);
        }
    }

    default void send(ServerPlayerEntity sender, MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player != sender) send(player);
        }
    }
}
