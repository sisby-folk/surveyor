package folk.sisby.surveyor.mixin.client;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements SurveyorWorld {
    @Unique
    private WorldSummary surveyor$worldSummary = null;

    @Override
    public WorldSummary surveyor$getWorldSummary() {
        if (surveyor$worldSummary == null) {
            if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
                surveyor$worldSummary = WorldSummary.of(MinecraftClient.getInstance().getServer().getWorld(((ClientWorld) (Object) this).getRegistryKey()));
            } else {
                surveyor$worldSummary = WorldSummary.load((ClientWorld) (Object) this, SurveyorClient.getWorldSavePath((ClientWorld) (Object) this), true);
            }
        }
        return surveyor$worldSummary;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onJoinWorld(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ClientWorld self = (ClientWorld) (Object) this;
        for (AbstractClientPlayerEntity player : self.getPlayers()) {
            if (MinecraftClient.getInstance().player == player && SurveyorClientEvents.INITIALIZING_WORLD) {
                SurveyorClientEvents.INITIALIZING_WORLD = false;
                SurveyorClientEvents.Invoke.clientPlayerLoad(player.clientWorld, WorldSummary.of(player.getWorld()), MinecraftClient.getInstance().player);
            }
        }
    }
}
