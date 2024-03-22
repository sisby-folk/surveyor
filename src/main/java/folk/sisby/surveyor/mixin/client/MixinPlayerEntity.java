package folk.sisby.surveyor.mixin.client;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.util.TextUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onClientDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity) return;
        if (((SurveyorWorld) self.getWorld()).surveyor$getWorldSummary().isClient() && !SurveyorClient.serverSupported()) {
            ((SurveyorWorld) self.getWorld()).surveyor$getWorldSummary().landmarks().put(
                self.getWorld(),
                new PlayerDeathLandmark(self.getBlockPos(), self.getUuid(), TextUtil.stripInteraction(self.getDamageTracker().getDeathMessage()), self.getWorld().getTimeOfDay(), self.getRandom().nextInt())
            );
        }
    }
}
