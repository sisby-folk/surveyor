package folk.sisby.surveyor.chunk;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorWorld;
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChunkSummaryState {
    public enum Type {
        SERVER,
        CLIENT
    }

    public final Type type;
    protected final Map<ChunkPos, RegionSummary> regions;
    protected final DynamicRegistryManager manager;

    public static ChunkPos getRegionPos(Chunk chunk) {
        return new ChunkPos(chunk.getPos().x >> RegionSummary.REGION_POWER, chunk.getPos().z >> RegionSummary.REGION_POWER);
    }

    public ChunkSummaryState(Type type, Map<ChunkPos, RegionSummary> regions, DynamicRegistryManager manager) {
        this.type = type;
        this.regions = regions;
        this.manager = manager;
    }

    public boolean contains(Chunk chunk) {
        ChunkPos regionPos = getRegionPos(chunk);
        return regions.containsKey(regionPos) && regions.get(regionPos).contains(chunk);
    }

    public void putChunk(World world, Chunk chunk) {
        regions.computeIfAbsent(getRegionPos(chunk), k -> new RegionSummary()).putChunk(world, chunk);
    }

    public static File getClientDirectory(ClientWorld world) {
        ServerInfo info = MinecraftClient.getInstance().getCurrentServerEntry();
        String serverFolder = "%s_%s".formatted(info.name, info.address).replaceAll("[^A-Za-z0-9_.]", "_");
        String saveFolder = String.valueOf(world.getBiomeAccess().seed);
        String dimNamespace = world.getRegistryKey().getValue().getNamespace();
        String dimPath = world.getRegistryKey().getValue().getPath();
        return FabricLoader.getInstance().getGameDir().resolve(Surveyor.ID).resolve(serverFolder).resolve(saveFolder).resolve(dimNamespace).resolve(dimPath).toFile();
    }

    public void save(World world, File dataFolder) {
        Surveyor.LOGGER.info("[Surveyor] Saving summaries for {}", world.getRegistryKey().getValue());
        File surveyorFolder = new File(dataFolder, Surveyor.ID);
        surveyorFolder.mkdirs();
        regions.forEach((pos, summary) -> {
            if (!summary.isDirty()) return;
            NbtCompound regionCompound = summary.writeNbt(manager, new NbtCompound(), pos);
            File regionFile = new File(surveyorFolder, "c.%d.%d.dat".formatted(pos.x, pos.z));
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
        save(world, world.getPersistentStateManager().directory);
    }

    public void save(ClientWorld world) {
        if (type != Type.CLIENT) return;
        save(world, getClientDirectory(world));
    }

    public static ChunkSummaryState load(Type type, World world, File dataFolder) {
        File surveyorFolder = new File(dataFolder, Surveyor.ID);
        surveyorFolder.mkdirs();
        File[] chunkFiles = surveyorFolder.listFiles((file, name) -> {
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
        Map<ChunkPos, RegionSummary> regions; regions = new HashMap<>();
        if (chunkFiles != null) {
            for (File regionFile : chunkFiles) {
                ChunkPos regionPos = new ChunkPos(Integer.parseInt(regionFile.getName().split("\\.")[1]), Integer.parseInt(regionFile.getName().split("\\.")[2]));
                NbtCompound regionCompound = null;
                try {
                    regionCompound = NbtIo.readCompressed(regionFile);
                } catch (IOException e) {
                    Surveyor.LOGGER.error("[Surveyor] Error loading region summary file {}.", regionFile.getName(), e);
                }
                if (regionCompound != null) regions.put(regionPos, new RegionSummary().readNbt(regionCompound, world.getRegistryManager()));
            }
        }
        return new ChunkSummaryState(type, regions, world.getRegistryManager());
    }

    public static ChunkSummaryState load(ServerWorld world) {
        return load(Type.SERVER, world, world.getPersistentStateManager().directory);
    }

    public static ChunkSummaryState load(ClientWorld world) {
        return load(Type.CLIENT, world, getClientDirectory(world));
    }

    public static void onChunkLoad(World world, Chunk chunk) {
        Type type = world instanceof ServerWorld ? Type.SERVER : Type.CLIENT;
        ChunkSummaryState state = ((SurveyorWorld) world).surveyor$getChunkSummaryState();
        if (state.type == type && !state.contains(chunk)) state.putChunk(world, chunk);
    }

    public static void onChunkUnload(World world, WorldChunk chunk) {
        Type type = world instanceof ServerWorld ? Type.SERVER : Type.CLIENT;
        ChunkSummaryState state = ((SurveyorWorld) world).surveyor$getChunkSummaryState();
        if (state.type == type && chunk.needsSaving()) state.putChunk(world, chunk);
    }
}
