package folk.sisby.surveyor.mixin.client;

import folk.sisby.surveyor.player.SurveyorPlayer;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity implements SurveyorPlayer {
    @Unique
    private final Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$exploredTerrain = new HashMap<>();
    @Unique
    private final Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> surveyor$exploredStructures = new HashMap<>();
    @Unique
    private int surveyor$playerViewDistance = -1;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(MinecraftClient client, ClientWorld world, ClientPlayNetworkHandler networkHandler, StatHandler stats, ClientRecipeBook recipeBook, boolean lastSneaking, boolean lastSprinting, CallbackInfo ci) {
    }

    @Override
    public Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$getExploredTerrain() {
        return null;
    }

    @Override
    public Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> surveyor$getExploredStructures() {
        return null;
    }

    @Override
    public int surveyor$getViewDistance() {
        return 0;
    }

    @Override
    public void surveyor$addExploredChunk(ChunkPos pos) {

    }

    @Override
    public void surveyor$addExploredStructure(Structure structure, ChunkPos pos) {

    }
}
