package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.util.TextUtil;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements SurveyorExploration {
    @Unique
    private final Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$exploredTerrain = new HashMap<>();
    @Unique
    private final Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> surveyor$exploredStructures = new HashMap<>();
    @Unique
    private int surveyor$playerViewDistance = -1;

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void writeSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        nbt.put(KEY_DATA, writeExplorationData(new NbtCompound()));
    }

    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void readSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        readExplorationData(nbt);
    }

    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void copySurveyorData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (oldPlayer instanceof SurveyorExploration them) {
            surveyor$exploredTerrain.clear();
            surveyor$exploredTerrain.putAll(them.surveyor$exploredTerrain());
            surveyor$exploredStructures.clear();
            surveyor$exploredStructures.putAll(them.surveyor$exploredStructures());
        }
    }

    @Inject(method = "setClientSettings", at = @At("HEAD"))
    public void setClientSettings(ClientSettingsC2SPacket packet, CallbackInfo ci) {
        surveyor$playerViewDistance = packet.viewDistance();
    }

    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageTracker;update()V"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        ((SurveyorWorld) self.getServerWorld()).surveyor$getWorldSummary().landmarks().put(
            self.getServerWorld(),
            new PlayerDeathLandmark(self.getBlockPos(), self.getUuid(), TextUtil.stripInteraction(self.getDamageTracker().getDeathMessage()), self.getServerWorld().getTimeOfDay(), self.getRandom().nextInt())
        );
    }

    @Override
    public Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$exploredTerrain() {
        return surveyor$exploredTerrain;
    }

    @Override
    public Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> surveyor$exploredStructures() {
        return surveyor$exploredStructures;
    }

    @Override
    public World surveyor$getWorld() {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        return self.getWorld();
    }

    @Override
    public @Nullable ServerPlayerEntity surveyor$getServerPlayer() {
        return (ServerPlayerEntity) (Object) this;
    }

    @Override
    public int surveyor$getViewDistance() {
        PlayerEntity self = (PlayerEntity) (Object) this;
        return surveyor$playerViewDistance == -1 ? self.getServer().getPlayerManager().getViewDistance() : surveyor$playerViewDistance;
    }
}