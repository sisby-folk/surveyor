package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.NetherPortalLandmark;
import folk.sisby.surveyor.landmark.PointOfInterestLandmark;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements SurveyorWorld {
    @Unique private WorldSummary surveyor$worldSummary = null;

    @Override
    public WorldSummary surveyor$getWorldSummary() {
        return surveyor$worldSummary;
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionOptions;chunkGenerator()Lnet/minecraft/world/gen/chunk/ChunkGenerator;"))
    public void loadSummary(CallbackInfo ci) {
        surveyor$worldSummary = WorldSummary.load(WorldSummary.Type.SERVER, (ServerWorld) (Object) this, Surveyor.getSavePath((ServerWorld) (Object) this));
        SurveyorEvents.Invoke.worldLoad((ServerWorld) (Object) this, surveyor$worldSummary);
    }

    @Inject(method = "saveLevel", at = @At("TAIL"))
    public void saveSummary(CallbackInfo ci) {
        if (surveyor$worldSummary != null) surveyor$worldSummary.save((ServerWorld) (Object) this, Surveyor.getSavePath((ServerWorld) (Object) this));
    }

    @Inject(method = "method_19499", at = @At("HEAD"))
    public void onPointOfInterestAdded(BlockPos blockPos, RegistryEntry<PointOfInterestType> poiType, CallbackInfo ci) {
        if (poiType.getKey().orElse(null) == PointOfInterestTypes.NETHER_PORTAL) {
            surveyor$getWorldSummary().landmarks().put((ServerWorld) (Object) this, new NetherPortalLandmark(blockPos.toImmutable()));
        }
    }

    @Inject(method = "method_39222", at = @At("HEAD"))
    public void onPointOfInterestRemoved(BlockPos blockPos, CallbackInfo ci) {
        surveyor$getWorldSummary().landmarks().removeAll((ServerWorld) (Object) this, PointOfInterestLandmark.class, blockPos);
    }
}
