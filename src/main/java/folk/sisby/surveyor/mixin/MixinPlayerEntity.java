package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.player.SurveyorPlayer;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.util.TextUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

@Mixin(ServerPlayerEntity.class)
public class MixinPlayerEntity implements SurveyorPlayer {
    @Unique private final Map<ChunkPos, BitSet> surveyor$exploredTerrain = new HashMap<>();

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void writeExploredChunks(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound modCompound = new NbtCompound();
        long[] regionArray = new long[surveyor$exploredTerrain.size() * 17];
        int i = 0;
        for (Map.Entry<ChunkPos, BitSet> entry : surveyor$exploredTerrain.entrySet()) {
            regionArray[i * 17] = entry.getKey().toLong();
            long[] regionBits = entry.getValue().toLongArray();
            System.arraycopy(regionBits, 0, regionArray, (i * 17) + 1, regionBits.length);
            i++;
        }
        modCompound.put(KEY_EXPLORED_TERRAIN, new NbtLongArray(regionArray));
        nbt.put(KEY_DATA, modCompound);
    }

    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void readExploredChunks(NbtCompound nbt, CallbackInfo ci) {
        surveyor$exploredTerrain.clear();
        long[] regionArray = nbt.getCompound(KEY_DATA).getLongArray(KEY_EXPLORED_TERRAIN);
        for (int i = 0; i < regionArray.length / 17; i ++) {
            surveyor$exploredTerrain.put(new ChunkPos(regionArray[i * 17]), BitSet.valueOf(Arrays.copyOfRange(regionArray, i * 17 + 1, (i + 1) * 17)));
            i++;
        }
    }

    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void copyExploredChunks(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (oldPlayer instanceof SurveyorPlayer them) {
            surveyor$exploredTerrain.clear();
            surveyor$exploredTerrain.putAll(them.surveyor$getExploredTerrain());
        }
    }

    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageTracker;update()V"))
    public void onClientDeath(DamageSource damageSource, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity) return;
        if (((SurveyorWorld) self.getWorld()).surveyor$getWorldSummary().isClient()) {
            ((SurveyorWorld) self.getWorld()).surveyor$getWorldSummary().landmarks().put(
                self.getWorld(),
                new PlayerDeathLandmark(self.getBlockPos(), self.getUuid(), TextUtil.stripInteraction(self.getDamageTracker().getDeathMessage()), self.getWorld().getTimeOfDay(), self.getRandom().nextInt())
            );
        }
    }

    @Override
    public Map<ChunkPos, BitSet> surveyor$getExploredTerrain() {
        return surveyor$exploredTerrain;
    }

    @Override
    public void surveyor$addExploredChunk(ChunkPos pos) {
        surveyor$exploredTerrain.computeIfAbsent(new ChunkPos(pos.getRegionX(), pos.getRegionZ()), k -> new BitSet(RegionSummary.REGION_SIZE * RegionSummary.REGION_SIZE)).set(pos.getRegionRelativeX() * RegionSummary.REGION_SIZE + pos.getRegionRelativeZ());
    }
}