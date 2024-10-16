package folk.sisby.surveyor.mixin;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.HasPoiType;
import folk.sisby.surveyor.landmark.NetherPortalLandmark;
import net.minecraft.block.NetherPortalBlock;
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
public class MixinServerWorld implements SurveyorWorld {
	@Unique
	private WorldSummary surveyor$summary = null;

	@Override
	public WorldSummary surveyor$getSummary() {
		return surveyor$summary;
	}

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionOptions;chunkGenerator()Lnet/minecraft/world/gen/chunk/ChunkGenerator;"))
	public void loadSummary(CallbackInfo ci) {
		ServerWorld self = (ServerWorld) (Object) this;
		surveyor$summary = WorldSummary.load(self, Surveyor.getSavePath(self.getRegistryKey(), self.getServer()), false);
		SurveyorEvents.Invoke.worldLoad(self);
	}

	@Inject(method = "method_19499", at = @At("HEAD"))
	public void onPointOfInterestAdded(BlockPos blockPos, RegistryEntry<PointOfInterestType> poiType, CallbackInfo ci) {
		if (!Surveyor.CONFIG.netherPortalLandmarks) return;
		ServerWorld self = (ServerWorld) (Object) this;
		WorldSummary summary = WorldSummary.of(self);
		if (summary.landmarks() != null && poiType.getKey().orElse(null) == PointOfInterestTypes.NETHER_PORTAL && self.getBlockState(blockPos).contains(NetherPortalBlock.AXIS)) {
			summary.landmarks().put(self, new NetherPortalLandmark(blockPos, self.getBlockState(blockPos).get(NetherPortalBlock.AXIS)));
		}
	}

	@Inject(method = "method_39222", at = @At("HEAD"))
	public void onPointOfInterestRemoved(BlockPos blockPos, CallbackInfo ci) {
		ServerWorld self = (ServerWorld) (Object) this;
		WorldSummary summary = WorldSummary.of(self);
		if (summary.landmarks() == null) return;
		summary.landmarks().removeAll(self, HasPoiType.class, blockPos);
	}
}
