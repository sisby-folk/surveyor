package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements SurveyorWorld {
    @Unique WorldSummary surveyor$worldSummary = null;

    @Override
    public WorldSummary surveyor$getWorldSummary() {
        return surveyor$worldSummary;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientChunkManager;<init>(Lnet/minecraft/client/world/ClientWorld;I)V"))
    public void loadSummary(CallbackInfo ci) {
        surveyor$worldSummary = MinecraftClient.getInstance().isIntegratedServerRunning() ?
            ((SurveyorWorld) MinecraftClient.getInstance().getServer().getWorld(((ClientWorld) (Object) this).getRegistryKey())).surveyor$getWorldSummary() :
            WorldSummary.load((ClientWorld) (Object) this);
    }
}
