package folk.sisby.surveyor;

import folk.sisby.surveyor.player.SurveyorPlayer;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class Surveyor implements ModInitializer {
    public static final String ID = "surveyor";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final String DATA_SUBFOLDER = "data";
    public static final SurveyorConfig CONFIG = SurveyorConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", "surveyor", SurveyorConfig.class);

    public static File getSavePath(ServerWorld world) {
        return DimensionType.getSaveDirectory(world.getRegistryKey(), world.getServer().getSavePath(WorldSavePath.ROOT)).resolve(DATA_SUBFOLDER).resolve(Surveyor.ID).toFile();
    }

    public static void checkStructureExploration(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        SurveyorPlayer sp = (SurveyorPlayer) player;
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
        Map<Structure, LongSet> structureReferences = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES).getStructureReferences();
        if (!structureReferences.isEmpty()) {
            structureReferences.forEach((structure, chunkPosSet) -> {
                LongSet unexploredSet = new LongArraySet(chunkPosSet.toLongArray());
                if (sp.surveyor$getExploredStructures().containsKey(structure)) unexploredSet.removeAll(sp.surveyor$getExploredStructures().get(structure));
                for (Long longPos : unexploredSet) {
                    ChunkPos startPos = new ChunkPos(longPos);
                    StructureStart start = world.getChunk(startPos.x, startPos.z, ChunkStatus.STRUCTURE_STARTS).getStructureStart(structure);
                    boolean found = false;
                    if (start.getBoundingBox().contains(pos)) {
                        for (StructurePiece piece : start.getChildren()) {
                            if (piece.getBoundingBox().contains(pos)) {
                                sp.surveyor$addExploredStructure(structure, startPos);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found && CONFIG.debugMode) {
                        player.sendMessageToClient(Text.literal("Discovered ").append(Text.literal(StringUtils.capitalize(world.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(structure).getPath().replace("_", " "))).formatted(Formatting.GREEN)).append(Text.literal(" at ")).append(Text.literal("[%s,%s]".formatted(startPos.x << 4, startPos.z << 4)).formatted(Formatting.GOLD)).formatted(Formatting.GRAY), true);
                    }
                }
            });
        }
    }

    @Override
    public void onInitialize() {
        SurveyorNetworking.init();
        ServerChunkEvents.CHUNK_LOAD.register(WorldTerrainSummary::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(WorldTerrainSummary::onChunkUnload);
        ServerTickEvents.END_WORLD_TICK.register((world -> {
            if ((world.getTime() & 7) != 0) return;
            for (ServerPlayerEntity player : world.getPlayers()) {
                checkStructureExploration(world, player, player.getBlockPos());
                checkStructureExploration(world, player, BlockPos.ofFloored(player.raycast(((SurveyorPlayer) player).surveyor$getViewDistance() << 4, 1.0F, false).getPos()));
            }
        }));
    }
}
