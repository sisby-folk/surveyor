package folk.sisby.surveyor.client;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.packet.C2SKnownTerrainPacket;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
import java.util.Set;
import java.util.UUID;

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

    public static SurveyorExploration getExploration(ClientPlayerEntity player) {
        if (MinecraftClient.getInstance().isIntegratedServerRunning()) {
            return SurveyorExploration.of((ServerPlayerEntity) MinecraftClient.getInstance().getServer().getWorld(MinecraftClient.getInstance().world.getRegistryKey()).getPlayerByUuid(player.getUuid()));
        } else {
            return ClientExploration.INSTANCE;
        }
    }

    @Override
    public void onInitializeClient() {
        SurveyorClientNetworking.init();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(ClientExploration::onLoad));
        ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> ClientExploration.onUnload()));
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (WorldSummary.of(world).isClient()) {
                WorldTerrainSummary.onChunkLoad(world, chunk);
                ClientExploration.INSTANCE.addChunk(world.getRegistryKey(), chunk.getPos());
            }
        });
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (WorldSummary.of(world).isClient()) WorldTerrainSummary.onChunkUnload(world, chunk);
        });
    }

    private record ClientExploration(Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain, Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures) implements SurveyorExploration {
        public static final String KEY_SHARED = "shared";
        public static final ClientExploration INSTANCE = new ClientExploration(new HashMap<>(), new HashMap<>());
        public static final ClientExploration SHARED = new ClientExploration(new HashMap<>(), new HashMap<>());
        public static File saveFile = null;

        public static void onLoad() {
            if (WorldSummary.of(MinecraftClient.getInstance().world).isClient()) {
                saveFile = getSavePath(INSTANCE.getWorld()).toPath().resolve(Uuids.getUuidFromProfile(MinecraftClient.getInstance().getSession().getProfile()).toString() + ".dat").toFile();
                NbtCompound explorationNbt = new NbtCompound();
                if (saveFile.exists()) {
                    try {
                        explorationNbt = NbtIo.readCompressed(saveFile);
                    } catch (IOException e) {
                        Surveyor.LOGGER.error("[Surveyor] Error loading client exploration file.", e);
                    }
                }
                ClientExploration.INSTANCE.read(explorationNbt);
                ClientExploration.SHARED.read(explorationNbt.getCompound(KEY_SHARED));
            }
        }

        public static void onUnload() {
            if (saveFile != null) {
                try {
                    NbtCompound nbt = ClientExploration.INSTANCE.write(new NbtCompound());
                    NbtCompound sharedNbt = ClientExploration.SHARED.write(new NbtCompound());
                    nbt.put(KEY_SHARED, sharedNbt);
                    NbtIo.writeCompressed(nbt, saveFile);
                } catch (IOException e) {
                    Surveyor.LOGGER.error("[Surveyor] Error saving client exploration file.", e);
                }
                saveFile = null;
            }
            ClientExploration.INSTANCE.terrain().clear();
            ClientExploration.INSTANCE.structures.clear();
            ClientExploration.SHARED.terrain.clear();
            ClientExploration.SHARED.structures.clear();
        }

        @Override
        public Set<UUID> sharedPlayers() {
            return Set.of(Uuids.getUuidFromProfile(MinecraftClient.getInstance().getSession().getProfile()));
        }

        @Override
        public World getWorld() {
            return MinecraftClient.getInstance().world;
        }

        @Override
        public @Nullable ServerPlayerEntity getServerPlayer() {
            return null;
        }

        @Override
        public int getViewDistance() {
            return MinecraftClient.getInstance().options.getViewDistance().getValue();
        }

        @Override
        public void addStructure(RegistryKey<World> worldKey, RegistryKey<Structure> structureKey, ChunkPos pos) {
            SurveyorExploration.super.addStructure(worldKey, structureKey, pos);
            SurveyorClientEvents.Invoke.structuresAdded(getWorld(), WorldSummary.of(getWorld()).structures(), structureKey, pos);
        }

        @Override
        public void addChunk(RegistryKey<World> worldKey, ChunkPos pos) {
            SurveyorExploration.super.addChunk(worldKey, pos);
            SurveyorClientEvents.Invoke.terrainUpdated(getWorld(), WorldSummary.of(getWorld()).terrain(), pos);
            WorldSummary.of(getWorld()).landmarks().asMap(this).forEach((type, map) -> map.forEach((lPos, landmark) -> {
                if (new ChunkPos(lPos).equals(pos)) {
                    if (exploredLandmark(getWorld().getRegistryKey(), landmark)) SurveyorClientEvents.Invoke.landmarksAdded(getWorld(), WorldSummary.of(getWorld()).landmarks(), landmark);
                }
            }));
        }
    }
}
