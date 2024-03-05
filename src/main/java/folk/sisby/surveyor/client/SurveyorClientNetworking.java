package folk.sisby.surveyor.client;

import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorNetworking;
import folk.sisby.surveyor.SurveyorWorld;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.packet.LandmarksAddedPacket;
import folk.sisby.surveyor.packet.LandmarksRemovedPacket;
import folk.sisby.surveyor.packet.S2CPacket;
import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.packet.S2CUpdateRegionPacket;
import folk.sisby.surveyor.terrain.RegionSummary;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;

import java.util.function.Function;

public class SurveyorClientNetworking {
    public static void init() {
        SurveyorNetworking.C2S_SENDER = p -> {
            p.toBufs().forEach(buf -> ClientPlayNetworking.send(p.getId(), buf));
        };
        ClientPlayNetworking.registerGlobalReceiver(SurveyorNetworking.S2C_STRUCTURES_ADDED, (c, h, b, s) -> handleClient(b, S2CStructuresAddedPacket::read, SurveyorClientNetworking::handleStructuresAdded));
        ClientPlayNetworking.registerGlobalReceiver(SurveyorNetworking.S2C_UPDATE_REGION, (c, h, b, s) -> handleClientUnparsed(b, SurveyorClientNetworking::handleTerrainAdded));
        ClientPlayNetworking.registerGlobalReceiver(SurveyorNetworking.LANDMARKS_ADDED, (c, h, b, s) -> handleClient(b, LandmarksAddedPacket::read, SurveyorClientNetworking::handleLandmarksAdded));
        ClientPlayNetworking.registerGlobalReceiver(SurveyorNetworking.LANDMARKS_REMOVED, (c, h, b, s) -> handleClient(b, LandmarksRemovedPacket::read, SurveyorClientNetworking::handleLandmarksRemoved));
    }

    private static void handleStructuresAdded(ClientWorld world, WorldSummary summary, S2CStructuresAddedPacket packet) {
        packet.structures().forEach((pos, structures) -> structures.forEach((structure, pair) -> summary.structures().put(world, pos, structure, pair.left(), pair.right())));
    }

    private static void handleTerrainAdded(ClientWorld world, WorldSummary summary, PacketByteBuf buf) {
        S2CUpdateRegionPacket packet = S2CUpdateRegionPacket.handle(buf, world.getRegistryManager(), summary);
        SurveyorEvents.Invoke.terrainUpdated(world, summary.terrain(), packet.chunks().stream().mapToObj(i -> new ChunkPos((packet.regionPos().x << RegionSummary.REGION_POWER) + (i / RegionSummary.REGION_SIZE), (packet.regionPos().z << RegionSummary.REGION_POWER) + (i % RegionSummary.REGION_SIZE))).toList());
    }

    private static void handleLandmarksAdded(ClientWorld world, WorldSummary summary, LandmarksAddedPacket packet) {
        packet.landmarks().forEach((type, map) -> map.forEach((pos, landmark) -> summary.landmarks().putLocal(world, landmark)));
    }

    private static void handleLandmarksRemoved(ClientWorld world, WorldSummary summary, LandmarksRemovedPacket packet) {
        packet.landmarks().forEach((type, positions) -> positions.forEach(pos -> summary.landmarks().removeLocal(world, type, pos)));
    }

    private static <T extends S2CPacket> void handleClient(PacketByteBuf buf, Function<PacketByteBuf, T> reader, ClientPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        MinecraftClient.getInstance().execute(() -> handler.handle(MinecraftClient.getInstance().world, MinecraftClient.getInstance().world == null ? null : ((SurveyorWorld) MinecraftClient.getInstance().world).surveyor$getWorldSummary(), packet));
    }

    private static void handleClientUnparsed(PacketByteBuf buf, ClientPacketHandler<PacketByteBuf> handler) {
        ClientWorld world = MinecraftClient.getInstance().world;
        WorldSummary summary = ((SurveyorWorld) MinecraftClient.getInstance().world).surveyor$getWorldSummary();
        handler.handle(world, summary, buf);
    }

    public interface ClientPacketHandler<T> {
        void handle(ClientWorld clientWorld, WorldSummary summary, T packet);
    }
}
