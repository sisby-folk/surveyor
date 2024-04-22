package folk.sisby.surveyor.mixin;

import com.mojang.authlib.GameProfile;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.ServerSummary;
import folk.sisby.surveyor.SurveyorPlayer;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.util.TextUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements SurveyorPlayer {
    @Unique
    PlayerSummary.ServerPlayerEntitySummary surveyor$summary = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(MinecraftServer server, ServerWorld world, GameProfile profile, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        surveyor$summary = new PlayerSummary.ServerPlayerEntitySummary(self);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        surveyor$summary.writeNbt(nbt);
        ServerSummary.of(self.getServer()).updatePlayer(self.getUuid(), nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        surveyor$summary.read(nbt);
        ServerSummary.of(self.getServer()).updatePlayer(self.getUuid(), nbt);
    }

    @Inject(method = "setClientSettings", at = @At("HEAD"))
    public void setSurveyorViewDistance(ClientSettingsC2SPacket packet, CallbackInfo ci) {
        surveyor$summary.setViewDistance(packet.viewDistance());
    }

    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageTracker;update()V"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        WorldSummary.of(self.getServerWorld()).landmarks().put(
            self.getServerWorld(),
            new PlayerDeathLandmark(self.getBlockPos(), self.getUuid(), TextUtil.stripInteraction(self.getDamageTracker().getDeathMessage()), self.getServerWorld().getTimeOfDay(), self.getRandom().nextInt())
        );
    }

    @Override
    public PlayerSummary surveyor$getSummary() {
        return surveyor$summary;
    }
}