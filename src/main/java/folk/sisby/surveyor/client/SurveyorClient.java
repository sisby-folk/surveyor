package folk.sisby.surveyor.client;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.packet.C2SKnownLandmarksPacket;
import folk.sisby.surveyor.packet.C2SKnownStructuresPacket;
import folk.sisby.surveyor.packet.C2SKnownTerrainPacket;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class SurveyorClient implements ClientModInitializer {
    public static final String SERVERS_FILE_NAME = "servers.txt";

    public static File getSavePath(World world) {
        String saveFolder = String.valueOf(world.getBiomeAccess().seed);
        Path savePath = FabricLoader.getInstance().getGameDir().resolve(Surveyor.DATA_SUBFOLDER).resolve(Surveyor.ID).resolve(saveFolder);
        savePath.toFile().mkdirs();
        File serversFile = savePath.resolve(SERVERS_FILE_NAME).toFile();
        try {
            ServerInfo info = MinecraftClient.getInstance().getCurrentServerEntry();
            if (info != null && (!serversFile.exists() || !FileUtils.readFileToString(serversFile, StandardCharsets.UTF_8).contains(info.name + "\n" + info.address))) {
                FileUtils.writeStringToFile(serversFile, info.name + "\n" + info.address + "\n", StandardCharsets.UTF_8, true);
            }
        } catch (IOException e) {
            Surveyor.LOGGER.error("[Surveyor] Error writing servers file for save {}.", savePath, e);
        }
        return savePath.toFile();
    }

    public static File getWorldSavePath(World world) {
        String dimNamespace = world.getRegistryKey().getValue().getNamespace();
        String dimPath = world.getRegistryKey().getValue().getPath();
        return getSavePath(world).toPath().resolve(dimNamespace).resolve(dimPath).toFile();
    }

    public static boolean serverSupported() {
        return ClientPlayNetworking.canSend(C2SKnownTerrainPacket.ID);
    }

    @Override
    public void onInitializeClient() {
        SurveyorClientNetworking.init();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            if (MinecraftClient.getInstance().world instanceof SurveyorWorld nsw && nsw.surveyor$getWorldSummary().isClient()) {
                WorldSummary summary = nsw.surveyor$getWorldSummary();
                new C2SKnownTerrainPacket(summary.terrain().bitSet()).send();
                new C2SKnownStructuresPacket(summary.structures().keySet()).send();
                new C2SKnownLandmarksPacket(summary.landmarks().keySet().asMap()).send();
                ClientExploration.onLoad();
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
            ClientExploration.onUnload();
        }));
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            ClientExploration.INSTANCE.surveyor$addExploredChunk(chunk.getPos());
            if (((SurveyorWorld) world).surveyor$getWorldSummary().isClient()) {
                WorldTerrainSummary.onChunkLoad(world, chunk);
                WorldStructureSummary.onChunkLoad(world, chunk);
            }
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (((SurveyorWorld) world).surveyor$getWorldSummary().isClient()) WorldTerrainSummary.onChunkUnload(world, chunk);
        });
    }

    record ClientExploration(Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$exploredTerrain, Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> surveyor$exploredStructures) implements SurveyorExploration {
        public static ClientExploration INSTANCE = new ClientExploration(new HashMap<>(), new HashMap<>());
        public static File saveFile = null;

        public static void onLoad() {
            saveFile = getSavePath(INSTANCE.surveyor$getWorld()).toPath().resolve(Uuids.getUuidFromProfile(MinecraftClient.getInstance().getSession().getProfile()).toString() + ".dat").toFile();
            NbtCompound explorationNbt = new NbtCompound();
            if (saveFile.exists()) {
                try {
                    explorationNbt = NbtIo.readCompressed(saveFile);
                } catch (IOException e) {
                    Surveyor.LOGGER.error("[Surveyor] Error loading client exploration file.", e);
                }
            }
            ClientExploration.INSTANCE.readExplorationData(explorationNbt);
        }

        public static void onUnload() {
            if (saveFile == null) return;
            try {
                NbtIo.writeCompressed(ClientExploration.INSTANCE.writeExplorationData(new NbtCompound()), saveFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error saving client exploration file.", e);
            }
            saveFile = null;
        }

        @Override
        public World surveyor$getWorld() {
            return MinecraftClient.getInstance().world;
        }

        @Override
        public @Nullable ServerPlayerEntity surveyor$getServerPlayer() {
            return null;
        }

        @Override
        public int surveyor$getViewDistance() {
            return MinecraftClient.getInstance().options.getViewDistance().getValue();
        }
    }
}
