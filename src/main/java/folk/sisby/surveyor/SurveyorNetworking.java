package folk.sisby.surveyor;

import folk.sisby.surveyor.packet.C2SPacket;
import folk.sisby.surveyor.packet.WorldLoadedC2SPacket;
import folk.sisby.surveyor.packet.LandmarksAddedPacket;
import folk.sisby.surveyor.packet.LandmarksRemovedPacket;
import folk.sisby.surveyor.packet.StructuresAddedS2CPacket;
import folk.sisby.surveyor.packet.TerrainAddedS2CPacket;
import folk.sisby.surveyor.structure.StructurePieceSummary;
import folk.sisby.surveyor.structure.StructureSummary;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SurveyorNetworking {
    public static final Identifier LANDMARKS_ADDED = new Identifier(Surveyor.ID, "landmarks_added");
    public static final Identifier LANDMARKS_REMOVED = new Identifier(Surveyor.ID, "landmarks_removed");

    public static final Identifier S2C_STRUCTURES_ADDED = new Identifier(Surveyor.ID, "s2c_structures_added");
    public static final Identifier S2C_TERRAIN_ADDED = new Identifier(Surveyor.ID, "s2c_terrain_added");

    public static final Identifier C2S_WORLD_LOADED = new Identifier(Surveyor.ID, "c2s_world_loaded");

    public static Consumer<C2SPacket> C2S_SENDER = p -> {
    };

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_WORLD_LOADED, (sv, p, h, b, se) -> handleServer(p, b, WorldLoadedC2SPacket::read, SurveyorNetworking::handleWorldLoaded));
        ServerPlayNetworking.registerGlobalReceiver(LANDMARKS_ADDED, (sv, p, h, b, se) -> handleServer(p, b, LandmarksAddedPacket::read, SurveyorNetworking::handleLandmarksAdded));
        ServerPlayNetworking.registerGlobalReceiver(LANDMARKS_REMOVED, (sv, p, h, b, se) -> handleServer(p, b, LandmarksRemovedPacket::read, SurveyorNetworking::handleLandmarksRemoved));
    }

    private static void handleWorldLoaded(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, WorldLoadedC2SPacket packet) {
        Registry<Biome> biomeRegistry = world.getRegistryManager().get(RegistryKeys.BIOME);
        Registry<Block> blockRegistry = world.getRegistryManager().get(RegistryKeys.BLOCK);
        Set<ChunkPos> serverChunkKeys = summary.terrain().keySet();
        serverChunkKeys.removeAll(packet.terrainKeys());
        Map<ChunkPos, Map<ChunkPos, ChunkSummary>> regionMap = new HashMap<>();
        serverChunkKeys.forEach(pos -> regionMap.computeIfAbsent(new ChunkPos(pos.getRegionX(), pos.getRegionZ()), rPos -> new HashMap<>()).put(pos, summary.terrain().get(pos)));
        regionMap.forEach((rPos, map) -> {
            ChunkPos cPos = new ChunkPos(rPos.x << RegionSummary.REGION_POWER, rPos.z << RegionSummary.REGION_POWER);
            new TerrainAddedS2CPacket(Map.of(Pair.of(
                RegionSummary.mapPalette(summary.terrain().getBiomePalette(cPos), biomeRegistry::getRawId),
                RegionSummary.mapPalette(summary.terrain().getBlockPalette(cPos), blockRegistry::getRawId)
            ), map)).send(player);
        });

        Collection<StructureSummary> serverStructures = summary.structures().values().stream().filter(s -> !packet.structureKeys().containsKey(s.getKey()) || !packet.structureKeys().get(s.getKey()).contains(s.getPos())).collect(Collectors.toSet());
        Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures = new HashMap<>();
        serverStructures.forEach(s -> {
            structures.computeIfAbsent(s.getPos(), p -> new HashMap<>()).put(s.getKey(), Pair.of(s.getType(), s.getChildren()));
        });
        new StructuresAddedS2CPacket(structures).send(player);
    }

    private static void handleLandmarksAdded(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, LandmarksAddedPacket packet) {
        packet.landmarks().forEach((type, map) -> map.forEach(((pos, landmark) -> summary.landmarks().put(player, world, landmark))));
    }

    private static void handleLandmarksRemoved(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, LandmarksRemovedPacket packet) {
        packet.landmarks().forEach((type, positions) -> positions.forEach((pos -> summary.landmarks().remove(player, world, type, pos))));
    }

    private static <T extends C2SPacket> void handleServer(ServerPlayerEntity player, PacketByteBuf buf, Function<PacketByteBuf, T> reader, ServerPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        handler.handle(player, player.getServerWorld(), ((SurveyorWorld) player.getServerWorld()).surveyor$getWorldSummary(), packet);
    }

    public interface ServerPacketHandler<T extends C2SPacket> {
        void handle(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, T packet);
    }
}
