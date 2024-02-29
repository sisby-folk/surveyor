package folk.sisby.surveyor;

import folk.sisby.surveyor.packet.c2s.C2SPacket;
import folk.sisby.surveyor.packet.c2s.OnJoinWorldC2SPacket;
import folk.sisby.surveyor.packet.c2s.OnLandmarkAddedC2SPacket;
import folk.sisby.surveyor.packet.c2s.OnLandmarkRemovedC2SPacket;
import folk.sisby.surveyor.packet.s2c.OnJoinWorldS2CPacket;
import folk.sisby.surveyor.structure.StructurePieceSummary;
import folk.sisby.surveyor.structure.StructureSummary;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SurveyorNetworking {
    public static final Identifier C2S_ON_JOIN_WORLD = new Identifier(Surveyor.ID, "c2s_on_join_world");
    public static final Identifier S2C_ON_JOIN_WORLD = new Identifier(Surveyor.ID, "s2c_on_join_world");
    public static final Identifier S2C_ON_STRUCTURE_ADDED = new Identifier(Surveyor.ID, "s2c_on_structure_added");
    public static final Identifier S2C_ON_LANDMARK_ADDED = new Identifier(Surveyor.ID, "s2c_on_landmark_added");
    public static final Identifier C2S_ON_LANDMARK_ADDED = new Identifier(Surveyor.ID, "c2s_on_landmark_added");
    public static final Identifier S2C_ON_LANDMARK_REMOVED = new Identifier(Surveyor.ID, "s2c_on_landmark_removed");
    public static final Identifier C2S_ON_LANDMARK_REMOVED = new Identifier(Surveyor.ID, "c2s_on_landmark_removed");
    public static Consumer<C2SPacket> C2S_SENDER = p -> {};

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_ON_JOIN_WORLD, (sv, p, h, b, se) -> handleServer(p, b, OnJoinWorldC2SPacket::new, SurveyorNetworking::handleOnJoinWorld));
        ServerPlayNetworking.registerGlobalReceiver(C2S_ON_LANDMARK_ADDED, (sv, p, h, b, se) -> handleServer(p, b, OnLandmarkAddedC2SPacket::new, SurveyorNetworking::handleOnLandmarkAdded));
        ServerPlayNetworking.registerGlobalReceiver(C2S_ON_LANDMARK_REMOVED, (sv, p, h, b, se) -> handleServer(p, b, OnLandmarkRemovedC2SPacket::new, SurveyorNetworking::handleOnLandmarkRemoved));
    }

    private static void handleOnJoinWorld(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, OnJoinWorldC2SPacket packet) {
        Set<ChunkPos> serverChunkKeys = summary.terrain().keySet();
        serverChunkKeys.removeAll(packet.terrainKeys());
        serverChunkKeys.clear();
        Collection<StructureSummary> serverStructures = summary.structures().values().stream().filter(s -> !packet.structureKeys().containsKey(s.getKey()) || !packet.structureKeys().get(s.getKey()).contains(s.getPos())).collect(Collectors.toSet());
        Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures = new HashMap<>();
        serverStructures.forEach(s -> {
            structures.computeIfAbsent(s.getPos(), p -> new HashMap<>()).put(s.getKey(), Pair.of(s.getType(), s.getChildren()));
        });

        new OnJoinWorldS2CPacket(serverChunkKeys.stream().collect(Collectors.toMap(p -> p, p -> summary.terrain().get(p))), structures).send(player);
    }

    private static void handleOnLandmarkAdded(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, OnLandmarkAddedC2SPacket packet) {
        summary.landmarks().put(player, world, packet.landmark());
    }

    private static void handleOnLandmarkRemoved(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, OnLandmarkRemovedC2SPacket packet) {
        summary.landmarks().remove(player, world, packet.type(), packet.pos());
    }

    private static <T extends C2SPacket> void handleServer(ServerPlayerEntity player, PacketByteBuf buf, Function<PacketByteBuf, T> reader, ServerPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        handler.handle(player, player.getServerWorld(), ((SurveyorWorld) player.getServerWorld()).surveyor$getWorldSummary(), packet);
    }

    public interface ServerPacketHandler<T extends C2SPacket> {
        void handle(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, T packet);
    }
}
