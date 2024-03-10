package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.player.SurveyorPlayer;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.util.TextUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
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
    @Unique private final Map<Structure, LongSet> surveyor$exploredStructures = new HashMap<>();
    @Unique private int surveyor$playerViewDistance = -1;

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void writeSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
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

        Registry<Structure> structureRegistry = self.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        NbtCompound structuresCompound = new NbtCompound();
        for (Structure structure : surveyor$exploredStructures.keySet()) {
            structuresCompound.putLongArray(structureRegistry.getId(structure).toString(), surveyor$exploredStructures.get(structure).toLongArray());
        }
        modCompound.put(KEY_EXPLORED_STRUCTURES, structuresCompound);
        nbt.put(KEY_DATA, modCompound);
    }

    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void readSurveyorData(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        NbtCompound modCompound = nbt.getCompound(KEY_DATA);

        surveyor$exploredTerrain.clear();
        long[] regionArray = modCompound.getLongArray(KEY_EXPLORED_TERRAIN);
        for (int i = 0; i < regionArray.length / 17; i ++) {
            surveyor$exploredTerrain.put(new ChunkPos(regionArray[i * 17]), BitSet.valueOf(Arrays.copyOfRange(regionArray, i * 17 + 1, (i + 1) * 17)));
            i++;
        }

        Registry<Structure> structureRegistry = self.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        surveyor$exploredStructures.clear();
        NbtCompound structuresCompound = modCompound.getCompound(KEY_EXPLORED_STRUCTURES);
        for (String key : structuresCompound.getKeys()) {
            surveyor$exploredStructures.put(structureRegistry.get(new Identifier(key)), new LongOpenHashSet(LongSet.of(structuresCompound.getLongArray(key))));
        }
    }

    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void copySurveyorData(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (oldPlayer instanceof SurveyorPlayer them) {
            surveyor$exploredTerrain.clear();
            surveyor$exploredTerrain.putAll(them.surveyor$getExploredTerrain());
            surveyor$exploredStructures.clear();
            surveyor$exploredStructures.putAll(them.surveyor$getExploredStructures());
        }
    }

    @Inject(method = "setClientSettings", at = @At("HEAD"))
    public void setClientSettings(ClientSettingsC2SPacket packet, CallbackInfo ci) {
        surveyor$playerViewDistance = packet.viewDistance();
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
    public Map<Structure, LongSet> surveyor$getExploredStructures() {
        return surveyor$exploredStructures;
    }

    @Override
    public int surveyor$getViewDistance() {
        PlayerEntity self = (PlayerEntity) (Object) this;
        return surveyor$playerViewDistance == -1 ? self.getServer().getPlayerManager().getViewDistance() : surveyor$playerViewDistance;
    }

    @Override
    public void surveyor$addExploredChunk(ChunkPos pos) {
        surveyor$exploredTerrain.computeIfAbsent(new ChunkPos(pos.getRegionX(), pos.getRegionZ()), k -> new BitSet(RegionSummary.REGION_SIZE * RegionSummary.REGION_SIZE)).set(pos.getRegionRelativeX() * RegionSummary.REGION_SIZE + pos.getRegionRelativeZ());
    }

    @Override
    public void surveyor$addExploredStructure(Structure structure, ChunkPos pos) {
        surveyor$exploredStructures.computeIfAbsent(structure, s -> new LongOpenHashSet()).add(pos.toLong());
    }
}