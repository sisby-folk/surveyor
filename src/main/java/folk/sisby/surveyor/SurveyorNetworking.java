package folk.sisby.surveyor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.packet.C2SKnownLandmarksPacket;
import folk.sisby.surveyor.packet.C2SKnownStructuresPacket;
import folk.sisby.surveyor.packet.C2SKnownTerrainPacket;
import folk.sisby.surveyor.packet.C2SPacket;
import folk.sisby.surveyor.packet.SyncLandmarksAddedPacket;
import folk.sisby.surveyor.packet.SyncLandmarksRemovedPacket;
import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.packet.S2CUpdateRegionPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
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
        ServerPlayNetworking.registerGlobalReceiver(SyncLandmarksAddedPacket.ID, (sv, p, h, b, se) -> handleServerUnparsed(p, b, SurveyorNetworking::handleLandmarksAdded));
        ServerPlayNetworking.registerGlobalReceiver(SyncLandmarksRemovedPacket.ID, (sv, p, h, b, se) -> handleServer(p, b, SyncLandmarksRemovedPacket::read, SurveyorNetworking::handleLandmarksRemoved));
    }

    private static void handleKnownTerrain(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, C2SKnownTerrainPacket packet) {
        Map<ChunkPos, BitSet> serverBits = summary.terrain().bitSet(SurveyorExploration.ofShared(player));
        Map<ChunkPos, BitSet> clientBits = packet.regionBits();
        serverBits.forEach((rPos, set) -> {
            if (clientBits.containsKey(rPos)) set.andNot(clientBits.get(rPos));
            if (!set.isEmpty()) {
                SurveyorExploration personalExploration = SurveyorExploration.of(player);
                BitSet personalSet = personalExploration.limitTerrainBitset(world.getRegistryKey(), rPos, (BitSet) set.clone());
                if (!personalSet.isEmpty()) new S2CUpdateRegionPacket(false, rPos, summary.terrain().getRegion(rPos), personalSet).send(player);
                set.andNot(personalSet);
                if (!set.isEmpty()) new S2CUpdateRegionPacket(true, rPos, summary.terrain().getRegion(rPos), set).send(player);
            }
        });
    }

    private static void handleKnownStructures(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, C2SKnownStructuresPacket packet) {
        Multimap<RegistryKey<Structure>, ChunkPos> structures = summary.structures().keySet(SurveyorExploration.ofShared(player));
        packet.structureKeys().forEach(structures::remove);
        if (structures.isEmpty()) return;
        SurveyorExploration personalExploration = SurveyorExploration.of(player);
        Multimap<RegistryKey<Structure>, ChunkPos> personalStructures = personalExploration.limitStructureKeySet(world.getRegistryKey(), HashMultimap.create(structures));
        if (!personalStructures.isEmpty()) new S2CStructuresAddedPacket(false, personalStructures, summary.structures()).send(player);
        personalStructures.forEach(structures::remove);
        if (!structures.isEmpty()) new S2CStructuresAddedPacket(true, structures, summary.structures()).send(player);
    }

    private static void handleKnownLandmarks(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, C2SKnownLandmarksPacket packet) {
        Multimap<LandmarkType<?>, BlockPos> landmarks = summary.landmarks().keySet(SurveyorExploration.ofShared(player));
        packet.landmarks().forEach(landmarks::remove);
        if (!landmarks.isEmpty()) new SyncLandmarksAddedPacket(landmarks, summary.landmarks()).send(player);
    }

    private static void handleLandmarksAdded(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, PacketByteBuf buf) {
        SyncLandmarksAddedPacket packet = SyncLandmarksAddedPacket.handle(buf, world, summary, player);
    }

    private static void handleLandmarksRemoved(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, SyncLandmarksRemovedPacket packet) {
        Multimap<LandmarkType<?>, BlockPos> changed = HashMultimap.create();
        packet.landmarks().forEach((type, pos) -> {
            if (summary.landmarks().contains(type, pos) && (world.getServer().isHost(player.getGameProfile()) || player.getUuid().equals(summary.landmarks().get(type, pos).owner()))) summary.landmarks().removeForBatch(changed, type, pos);
        });
        if (!changed.isEmpty()) summary.landmarks().handleChanged(world, changed, false, player);
    }

    private static <T extends C2SPacket> void handleServer(ServerPlayerEntity player, PacketByteBuf buf, Function<PacketByteBuf, T> reader, ServerPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        handler.handle(player, player.getServerWorld(), WorldSummary.of(player.getServerWorld()), packet);
    }

    private static void handleServerUnparsed(ServerPlayerEntity player, PacketByteBuf buf, ServerPacketHandler<PacketByteBuf> handler) {
        handler.handle(player, player.getServerWorld(), WorldSummary.of(player.getWorld()), buf);
    }

    public interface ServerPacketHandler<T> {
        void handle(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, T packet);
    }
}
