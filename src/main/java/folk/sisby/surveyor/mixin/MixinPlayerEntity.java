package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.player.SurveyorPlayer;
import folk.sisby.surveyor.util.TextUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public class MixinPlayerEntity implements SurveyorPlayer {
    @Unique private final Set<ChunkPos> surveyor$exploredChunks = new HashSet<>();

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void writeExploredChunks(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound modCompound = new NbtCompound();
        List<Integer> coordList = new ArrayList<>();
        surveyor$exploredChunks.forEach(pos -> {
            coordList.add(pos.x);
            coordList.add(pos.z);
        });
        NbtIntArray coordArray = new NbtIntArray(coordList);
        modCompound.put(KEY_EXPLORED_CHUNKS, coordArray);
        nbt.put(KEY_DATA, modCompound);
    }

    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void readExploredChunks(NbtCompound nbt, CallbackInfo ci) {
        surveyor$exploredChunks.clear();
        int[] coordArray = nbt.getCompound(KEY_DATA).getIntArray(KEY_EXPLORED_CHUNKS);
        for (int i = 1; i < coordArray.length; i += 2) {
            surveyor$exploredChunks.add(new ChunkPos(coordArray[i - 1], coordArray[i]));
        }
    }

    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void copyExploredChunks(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (oldPlayer instanceof SurveyorPlayer them) {
            surveyor$exploredChunks.clear();
            surveyor$exploredChunks.addAll(them.surveyor$getExploredChunks());
        }
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
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
    public Set<ChunkPos> surveyor$getExploredChunks() {
        return surveyor$exploredChunks;
    }

    @Override
    public void surveyor$addExploredChunk(ChunkPos pos) {
        surveyor$exploredChunks.add(pos);
    }
}