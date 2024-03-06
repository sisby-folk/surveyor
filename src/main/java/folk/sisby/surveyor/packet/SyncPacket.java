package folk.sisby.surveyor.packet;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public interface SyncPacket extends C2SPacket, S2CPacket {
    default void send(ServerPlayerEntity sender, World world) {
        if (world instanceof ServerWorld sw) {
            send(sender, sw);
        } else {
            send();
        }
    }
}
