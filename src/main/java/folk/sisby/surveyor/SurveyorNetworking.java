package folk.sisby.surveyor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.packet.C2SKnownLandmarksPacket;
import folk.sisby.surveyor.packet.C2SKnownStructuresPacket;
import folk.sisby.surveyor.packet.C2SKnownTerrainPacket;
import folk.sisby.surveyor.packet.C2SPacket;
import folk.sisby.surveyor.packet.SyncLandmarksAddedPacket;
import folk.sisby.surveyor.packet.SyncLandmarksRemovedPacket;
import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.packet.S2CUpdateRegionPacket;
import folk.sisby.surveyor.structure.StructureSummary;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class SurveyorNetworking {

    public static Consumer<C2SPacket> C2S_SENDER = p -> {
    };

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(C2SKnownTerrainPacket.ID, (sv, p, h, b, se) -> handleServer(p, b, C2SKnownTerrainPacket::read, SurveyorNetworking::handleKnownTerrain));
        ServerPlayNetworking.registerGlobalReceiver(C2SKnownStructuresPacket.ID, (sv, p, h, b, se) -> handleServer(p, b, C2SKnownStructuresPacket::read, SurveyorNetworking::handleKnownStructures));
        ServerPlayNetworking.registerGlobalReceiver(C2SKnownLandmarksPacket.ID, (sv, p, h, b, se) -> handleServer(p, b, C2SKnownLandmarksPacket::read, SurveyorNetworking::handleKnownLandmarks));
        ServerPlayNetworking.registerGlobalReceiver(SyncLandmarksAddedPacket.ID, (sv, p, h, b, se) -> handleServer(p, b, SyncLandmarksAddedPacket::read, SurveyorNetworking::handleLandmarksAdded));
        ServerPlayNetworking.registerGlobalReceiver(SyncLandmarksRemovedPacket.ID, (sv, p, h, b, se) -> handleServer(p, b, SyncLandmarksRemovedPacket::read, SurveyorNetworking::handleLandmarksRemoved));
    }

    private static void handleKnownTerrain(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, C2SKnownTerrainPacket packet) {
        Map<ChunkPos, BitSet> serverBits = summary.terrain().bitSet();
        Map<ChunkPos, BitSet> clientBits = packet.regionBits();
        serverBits.forEach((rPos, set) -> {
            if (clientBits.containsKey(rPos)) set.andNot(clientBits.get(rPos));
            new S2CUpdateRegionPacket(rPos, summary.terrain().getRegion(rPos), set).send(player);
        });
    }

    private static void handleKnownStructures(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, C2SKnownStructuresPacket packet) {
        Map<RegistryKey<Structure>, Map<ChunkPos, StructureSummary>> structures = summary.structures().asMap();
        packet.structureKeys().forEach((key, starts) -> {
            if (structures.containsKey(key)) {
                starts.forEach(p -> structures.get(key).remove(p));
                if (structures.get(key).isEmpty()) structures.remove(key);
            }
        });
        Map<RegistryKey<Structure>, RegistryKey<StructureType<?>>> structureTypes = new HashMap<>();
        Multimap<RegistryKey<Structure>, TagKey<Structure>> structureTags = HashMultimap.create();
        for (RegistryKey<Structure> key : structures.keySet()) {
            structureTypes.put(key, summary.structures().getType(key));
            structureTags.putAll(key, summary.structures().getTags(key));
        }
        new S2CStructuresAddedPacket(structures, structureTypes, structureTags).send(player);
    }

    private static void handleKnownLandmarks(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, C2SKnownLandmarksPacket packet) {
        Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks = summary.landmarks().asMap();
        packet.landmarks().forEach((type, positions) -> {
            if (landmarks.containsKey(type)) {
                positions.forEach(p -> landmarks.get(type).remove(p));
                if (landmarks.get(type).isEmpty()) landmarks.remove(type);
            }
        });
        new SyncLandmarksAddedPacket(landmarks).send(player);
    }

    private static void handleLandmarksAdded(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, SyncLandmarksAddedPacket packet) {
        packet.landmarks().forEach((type, map) -> map.forEach(((pos, landmark) -> summary.landmarks().put(player, world, landmark))));
    }

    private static void handleLandmarksRemoved(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, SyncLandmarksRemovedPacket packet) {
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
