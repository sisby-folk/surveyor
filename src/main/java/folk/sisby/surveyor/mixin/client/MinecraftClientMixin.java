package folk.sisby.surveyor.mixin.client;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.packet.WorldLoadedC2SPacket;
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
    void onJoinWorld(ClientWorld newWorld, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world instanceof SurveyorWorld csw && csw.surveyor$getWorldSummary().isClient()) {
            csw.surveyor$getWorldSummary().save(MinecraftClient.getInstance().world, SurveyorClient.getSavePath(MinecraftClient.getInstance().world));
            if (newWorld instanceof SurveyorWorld nsw && nsw.surveyor$getWorldSummary().isClient()) {
                WorldSummary summary = nsw.surveyor$getWorldSummary();
                new WorldLoadedC2SPacket(summary.terrain().keySet(), summary.structures().keySet()).send();
            }
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    void saveSummaryOnDisconnect(Screen screen, CallbackInfo ci) {
        ClientWorld currentWorld = MinecraftClient.getInstance().world;
        if (currentWorld instanceof SurveyorWorld sw && sw.surveyor$getWorldSummary().isClient()) sw.surveyor$getWorldSummary().save(currentWorld, SurveyorClient.getSavePath(MinecraftClient.getInstance().world));
    }
}
