package folk.sisby.surveyor.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.packet.S2CPacket;
import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.packet.S2CUpdateRegionPacket;
import folk.sisby.surveyor.packet.SyncLandmarksAddedPacket;
import folk.sisby.surveyor.packet.SyncLandmarksRemovedPacket;
import folk.sisby.surveyor.terrain.RegionSummary;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.function.Function;

public class SurveyorClientNetworking {
    public static void init() {
        SurveyorNetworking.C2S_SENDER = p -> {
            if (!ClientPlayNetworking.canSend(p.getId())) return;
            p.toBufs().forEach(buf -> ClientPlayNetworking.send(p.getId(), buf));
        };
        ClientPlayNetworking.registerGlobalReceiver(S2CStructuresAddedPacket.ID, (c, h, b, s) -> handleClient(b, S2CStructuresAddedPacket::read, SurveyorClientNetworking::handleStructuresAdded));
        ClientPlayNetworking.registerGlobalReceiver(S2CUpdateRegionPacket.ID, (c, h, b, s) -> handleClientUnparsed(b, SurveyorClientNetworking::handleTerrainAdded));
        ClientPlayNetworking.registerGlobalReceiver(SyncLandmarksAddedPacket.ID, (c, h, b, s) -> handleClient(b, SyncLandmarksAddedPacket::read, SurveyorClientNetworking::handleLandmarksAdded));
        ClientPlayNetworking.registerGlobalReceiver(SyncLandmarksRemovedPacket.ID, (c, h, b, s) -> handleClient(b, SyncLandmarksRemovedPacket::read, SurveyorClientNetworking::handleLandmarksRemoved));
    }

    private static void handleStructuresAdded(ClientWorld world, WorldSummary summary, S2CStructuresAddedPacket packet) {
        packet.structures().forEach((key, map) -> map.forEach((pos, start) -> summary.structures().put(world, key, pos, start, packet.structureTypes().get(key), packet.structureTags().get(key))));
        if (MinecraftClient.getInstance().player != null) {
            SurveyorExploration exploration = SurveyorClient.getExploration(null);
            packet.structures().forEach((key, starts) -> starts.forEach((pos, structure) -> exploration.addStructure(world.getRegistryKey(), key, pos)));
        }
    }

    private static void handleTerrainAdded(ClientWorld world, WorldSummary summary, PacketByteBuf buf) {
        S2CUpdateRegionPacket packet = S2CUpdateRegionPacket.handle(buf, world.getRegistryManager(), summary);
        SurveyorClient.getExploration(null).mergeRegion(world.getRegistryKey(), packet.regionPos(), packet.chunks());
        SurveyorEvents.Invoke.terrainUpdated(world, packet.chunks().stream().mapToObj(i -> RegionSummary.chunkForBit(packet.regionPos(), i)).toList());
    }

    private static void handleLandmarksAdded(ClientWorld world, WorldSummary summary, SyncLandmarksAddedPacket packet) {
        Multimap<LandmarkType<?>, BlockPos> changed = HashMultimap.create();
        packet.landmarks().forEach((type, map) -> map.forEach((pos, landmark) -> summary.landmarks().putForBatch(changed, landmark)));
        summary.landmarks().handleChanged(world, changed, true, null);
    }

    private static void handleLandmarksRemoved(ClientWorld world, WorldSummary summary, SyncLandmarksRemovedPacket packet) {
        Multimap<LandmarkType<?>, BlockPos> changed = HashMultimap.create();
        packet.landmarks().forEach((type, pos) -> summary.landmarks().removeForBatch(changed, type, pos));
        summary.landmarks().handleChanged(world, changed, true, null);
    }

    private static <T extends S2CPacket> void handleClient(PacketByteBuf buf, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        WorldSummary summary = WorldSummary.of(MinecraftClient.getInstance().world);
        if (!summary.isClient()) return;
        MinecraftClient.getInstance().execute(() -> handler.handle(MinecraftClient.getInstance().world, summary, packet));
    }

    private static void handleClientUnparsed(PacketByteBuf buf, ClientPacketHandler<PacketByteBuf> handler) {
        ClientWorld world = MinecraftClient.getInstance().world;
        WorldSummary summary = WorldSummary.of(MinecraftClient.getInstance().world);
        if (!summary.isClient()) return;
        handler.handle(world, summary, buf);
    }

    public interface ClientPacketHandler<T> {
        void handle(ClientWorld clientWorld, WorldSummary summary, T packet);
    }
}
