package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "joinWorld", at = @At("HEAD"))
    void onJoinWorld(ClientWorld world, CallbackInfo ci) {
        ClientWorld currentWorld = MinecraftClient.getInstance().world;
        if (currentWorld instanceof SurveyorWorld csw) {
            csw.surveyor$getChunkSummaryState().save(currentWorld);
            if (world instanceof SurveyorWorld sw) sw.surveyor$getChunkSummaryState();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    void onDisconnect(Screen screen, CallbackInfo ci) {
        ClientWorld currentWorld = MinecraftClient.getInstance().world;
        if (currentWorld instanceof SurveyorWorld sw) sw.surveyor$getChunkSummaryState().save(currentWorld);
    }
}
