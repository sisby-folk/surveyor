package folk.sisby.surveyor.mixin.client;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Inject(method = "init", at = @At("TAIL"))
    public void onJoinWorld(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (SurveyorClientEvents.INITIALIZING_WORLD) {
            SurveyorClientEvents.INITIALIZING_WORLD = false;
            SurveyorClientEvents.Invoke.clientPlayerLoad(self.clientWorld, ((SurveyorWorld) self.getWorld()).surveyor$getWorldSummary(), self);
        }
    }
}
