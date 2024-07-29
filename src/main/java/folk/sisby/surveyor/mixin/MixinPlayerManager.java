package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.ServerSummary;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Inject(method = "loadPlayerData", at = @At("RETURN"))
    public void loadPlayerSummary(ServerPlayerEntity player, CallbackInfoReturnable<Optional<NbtCompound>> cir) {
        ServerSummary.of(player.getServer()).updatePlayer(Surveyor.getUuid(player), cir.getReturnValue().orElse(new NbtCompound()), true, player.getServer());
    }
}
