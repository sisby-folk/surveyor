package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.SurveyorPlayer;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.util.TextUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements SurveyorPlayer {
    @Unique private final ServerPlayerExploration surveyor$exploration = new ServerPlayerExploration((ServerPlayerEntity) (Object) this);

    @Override
    public SurveyorExploration surveyor$getExploration() {
        return surveyor$exploration;
    }

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void writeSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        nbt.put(ServerPlayerExploration.KEY_DATA, surveyor$exploration.write(new NbtCompound()));
    }

    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void readSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        surveyor$exploration.read(nbt.getCompound(ServerPlayerExploration.KEY_DATA));
    }

    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void copySurveyorData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        surveyor$exploration.copyFrom(SurveyorExploration.of(oldPlayer));
    }

    @Inject(method = "setClientSettings", at = @At("HEAD"))
    public void setSurveyorViewDistance(ClientSettingsC2SPacket packet, CallbackInfo ci) {
        surveyor$exploration.surveyor$playerViewDistance = packet.viewDistance();
    }

    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageTracker;update()V"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        WorldSummary.of(self.getServerWorld()).landmarks().put(
            self.getServerWorld(),
            new PlayerDeathLandmark(self.getBlockPos(), self.getUuid(), TextUtil.stripInteraction(self.getDamageTracker().getDeathMessage()), self.getServerWorld().getTimeOfDay(), self.getRandom().nextInt())
        );
    }
}