package folk.sisby.surveyor.client;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.packet.c2s.OnJoinWorldC2SPacket;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class SurveyorClient implements ClientModInitializer {
    public static final String SERVERS_FILE_NAME = "servers.txt";

    public static File getSavePath(ClientWorld world) {
        String saveFolder = String.valueOf(world.getBiomeAccess().seed);
        String dimNamespace = world.getRegistryKey().getValue().getNamespace();
        String dimPath = world.getRegistryKey().getValue().getPath();
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
        return savePath.resolve(dimNamespace).resolve(dimPath).toFile();
    }

    @Override
    public void onInitializeClient() {
        SurveyorClientNetworking.init();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            if (MinecraftClient.getInstance().world instanceof SurveyorWorld nsw && nsw.surveyor$getWorldSummary().terrain().type() == WorldSummary.Type.CLIENT) {
                WorldSummary summary = nsw.surveyor$getWorldSummary();
                new OnJoinWorldC2SPacket(summary.terrain().keySet(), summary.structures().keySet()).send();
            }
        }));
        ClientChunkEvents.CHUNK_LOAD.register(WorldTerrainSummary::onChunkLoad);
        ClientChunkEvents.CHUNK_LOAD.register(WorldStructureSummary::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(WorldTerrainSummary::onChunkUnload);
    }
}
