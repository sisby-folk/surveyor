package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"))
    private void saveSummaries(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer self = (MinecraftServer) (Object) this;
        for (ServerWorld world : self.getWorlds()) {
            if (!world.savingDisabled || force) ((SurveyorWorld) world).surveyor$getWorldSummary().save(world, Surveyor.getSavePath(world), suppressLogs);
        }
    }
}
