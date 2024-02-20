package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.util.ChunkUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ChunkSummaryState {
    public static final String SERVERS_FILE_NAME = "servers.txt";
    public static final String DATA_SUBFOLDER = "data";

    public enum Type {
        SERVER,
        CLIENT
    }

    public final Type type;
    protected final Map<ChunkPos, RegionSummary> regions;
    protected final DynamicRegistryManager manager;

    public static ChunkPos getRegionPos(ChunkPos pos) {
        return new ChunkPos(pos.x >> RegionSummary.REGION_POWER, pos.z >> RegionSummary.REGION_POWER);
    }

    public ChunkSummaryState(Type type, Map<ChunkPos, RegionSummary> regions, DynamicRegistryManager manager) {
        this.type = type;
        this.regions = regions;
        this.manager = manager;
    }

    public boolean contains(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.containsKey(regionPos) && regions.get(regionPos).contains(pos);
    }

    public ChunkSummary get(ChunkPos pos) {
        ChunkPos regionPos = getRegionPos(pos);
        return regions.get(regionPos).get(pos);
    }

    public void putChunk(World world, Chunk chunk) {
        regions.computeIfAbsent(getRegionPos(chunk.getPos()), k -> new RegionSummary(type)).putChunk(world, chunk);
    }

    public static File getClientDirectory(ClientWorld world) {
        ServerInfo info = MinecraftClient.getInstance().getCurrentServerEntry();
        String saveFolder = String.valueOf(world.getBiomeAccess().seed);
        String dimNamespace = world.getRegistryKey().getValue().getNamespace();
        String dimPath = world.getRegistryKey().getValue().getPath();
        Path savePath = FabricLoader.getInstance().getGameDir().resolve(DATA_SUBFOLDER).resolve(Surveyor.ID).resolve(saveFolder);
        savePath.toFile().mkdirs();
        File serversFile = savePath.resolve(SERVERS_FILE_NAME).toFile();
        try {
            if (!serversFile.exists() || !FileUtils.readFileToString(serversFile, StandardCharsets.UTF_8).contains(info.name + "\n" + info.address)) {
                FileUtils.writeStringToFile(serversFile, info.name + "\n" + info.address + "\n", StandardCharsets.UTF_8, true);
            }
        } catch (IOException e) {
            Surveyor.LOGGER.error("[Surveyor] Error writing servers file for save {}.", savePath, e);
        }
        return savePath.resolve(dimNamespace).resolve(dimPath).toFile();
    }

    public void save(World world, File folder) {
        Surveyor.LOGGER.info("[Surveyor] Saving summaries for {}", world.getRegistryKey().getValue());
        folder.mkdirs();
        regions.forEach((pos, summary) -> {
            if (!summary.isDirty()) return;
            NbtCompound regionCompound = summary.writeNbt(manager, new NbtCompound(), pos);
            File regionFile = new File(folder, "c.%d.%d.dat".formatted(pos.x, pos.z));
            try {
                NbtIo.writeCompressed(regionCompound, regionFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing region summary file {}.", regionFile.getName(), e);
            }
        });
        Surveyor.LOGGER.info("[Surveyor] Finished saving data for {}", world.getRegistryKey().getValue());
    }

    public void save(ServerWorld world) {
        if (type != Type.SERVER) return;
        save(world, new File(world.getPersistentStateManager().directory, Surveyor.ID));
    }

    public void save(ClientWorld world) {
        if (type != Type.CLIENT) return;
        save(world, getClientDirectory(world));
    }

    public static ChunkSummaryState load(Type type, World world, File folder) {
        folder.mkdirs();
        File[] chunkFiles = folder.listFiles((file, name) -> {
            String[] split = name.split("\\.");
            if (split.length == 4 && split[0].equals("c") && split[3].equals("dat")) {
                try {
                    Integer.parseInt(split[1]);
                    Integer.parseInt(split[2]);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        });
        Map<ChunkPos, RegionSummary> regions;
        regions = new HashMap<>();
        if (chunkFiles != null) {
            for (File regionFile : chunkFiles) {
                ChunkPos regionPos = new ChunkPos(Integer.parseInt(regionFile.getName().split("\\.")[1]), Integer.parseInt(regionFile.getName().split("\\.")[2]));
                NbtCompound regionCompound = null;
                try {
                    regionCompound = NbtIo.readCompressed(regionFile);
                } catch (IOException e) {
                    Surveyor.LOGGER.error("[Surveyor] Error loading region summary file {}.", regionFile.getName(), e);
                }
                if (regionCompound != null) regions.put(regionPos, new RegionSummary(type).readNbt(regionCompound, world.getRegistryManager()));
            }
        }
        return new ChunkSummaryState(type, regions, world.getRegistryManager());
    }

    public static ChunkSummaryState load(ServerWorld world) {
        return load(Type.SERVER, world, new File(world.getPersistentStateManager().directory, Surveyor.ID));
    }

    public static ChunkSummaryState load(ClientWorld world) {
        return load(Type.CLIENT, world, getClientDirectory(world));
    }

    public static void onChunkLoad(World world, Chunk chunk) {
        Type type = world instanceof ServerWorld ? Type.SERVER : Type.CLIENT;
        ChunkSummaryState state = ((SurveyorWorld) world).surveyor$getChunkSummaryState();
        if (state.type == type && (!state.contains(chunk.getPos()) || !ChunkUtil.airCount(chunk).equals(state.get(chunk.getPos()).airCount))) state.putChunk(world, chunk);
    }

    public static void onChunkUnload(World world, WorldChunk chunk) {
        Type type = world instanceof ServerWorld ? Type.SERVER : Type.CLIENT;
        ChunkSummaryState state = ((SurveyorWorld) world).surveyor$getChunkSummaryState();
        if (state.type == type && chunk.needsSaving()) state.putChunk(world, chunk);
    }
}
