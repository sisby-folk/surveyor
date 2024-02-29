package folk.sisby.surveyor.terrain;

import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.util.ChunkUtil;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WorldTerrainSummary {
    protected final Map<ChunkPos, RegionSummary> regions;

    public WorldTerrainSummary(Map<ChunkPos, RegionSummary> regions) {
        this.regions = regions;
    }

    protected static ChunkPos regionPosOf(ChunkPos pos) {
        return new ChunkPos(pos.x >> RegionSummary.REGION_POWER, pos.z >> RegionSummary.REGION_POWER);
    }

    public boolean contains(ChunkPos pos) {
        ChunkPos regionPos = regionPosOf(pos);
        return regions.containsKey(regionPos) && regions.get(regionPos).contains(pos);
    }

    public ChunkSummary get(ChunkPos pos) {
        ChunkPos regionPos = regionPosOf(pos);
        return regions.get(regionPos).get(pos);
    }

    public IndexedIterable<Biome> getBiomePalette(ChunkPos pos) {
        ChunkPos regionPos = regionPosOf(pos);
        return regions.get(regionPos).getBiomePalette();
    }

    public IndexedIterable<Block> getBlockPalette(ChunkPos pos) {
        ChunkPos regionPos = regionPosOf(pos);
        return regions.get(regionPos).getBlockPalette();
    }

    public Set<ChunkPos> keySet() {
        Set<ChunkPos> chunkPosCollection = new HashSet<>();
        regions.forEach((p, r) -> chunkPosCollection.addAll(r.getChunks(p)));
        return chunkPosCollection;
    }

    public void put(World world, Chunk chunk) {
        regions.computeIfAbsent(regionPosOf(chunk.getPos()), k -> new RegionSummary()).putChunk(world, chunk);
        SurveyorEvents.Invoke.chunkAdded(world, this, chunk.getPos(), get(chunk.getPos()));
    }

    public void save(World world, File folder) {
        regions.forEach((pos, summary) -> {
            if (!summary.isDirty()) return;
            NbtCompound regionCompound = summary.writeNbt(world.getRegistryManager(), new NbtCompound(), pos);
            File regionFile = new File(folder, "c.%d.%d.dat".formatted(pos.x, pos.z));
            try {
                NbtIo.writeCompressed(regionCompound, regionFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing region summary file {}.", regionFile.getName(), e);
            }
        });
    }

    public static WorldTerrainSummary load(World world, File folder) {
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
                if (regionCompound != null) regions.put(regionPos, new RegionSummary().readNbt(regionCompound, world.getRegistryManager()));
            }
        }
        return new WorldTerrainSummary(regions);
    }

    public static void onChunkLoad(World world, Chunk chunk) {
        WorldSummary summary = ((SurveyorWorld) world).surveyor$getWorldSummary();
        if ((!summary.terrain().contains(chunk.getPos()) || !ChunkUtil.airCount(chunk).equals(summary.terrain().get(chunk.getPos()).getAirCount()))){
            summary.terrain().put(world, chunk);
        }
    }

    public static void onChunkUnload(World world, WorldChunk chunk) {
        WorldSummary summary = ((SurveyorWorld) world).surveyor$getWorldSummary();
        if (chunk.needsSaving()) {
            summary.terrain().put(world, chunk);
        }
    }
}
