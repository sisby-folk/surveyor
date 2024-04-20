package folk.sisby.surveyor;

import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.util.ArrayUtil;
import folk.sisby.surveyor.util.MapUtil;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface PlayerSummary {
    String KEY_DATA = "surveyor";
    String KEY_USERNAME = "username";

    static PlayerSummary of(ServerPlayerEntity player) {
        return ((SurveyorPlayer) player).surveyor$getPlayerSummary();
    }

    static PlayerSummary of(UUID uuid, MinecraftServer server) {
        return ServerSummary.of(server).getPlayer(uuid, server);
    }

    SurveyorExploration exploration();

    String username();

    RegistryKey<World> dimension();

    Vec3d pos();

    float yaw();

    int viewDistance();

    boolean online();

    record OfflinePlayerSummary(SurveyorExploration exploration, String username, RegistryKey<World> dimension, Vec3d pos, float yaw) implements PlayerSummary {
        public OfflinePlayerSummary(NbtCompound nbt) {
            this(
                OfflinePlayerExploration.from(nbt.getCompound(KEY_DATA)),
                nbt.getCompound(KEY_DATA).getString(KEY_USERNAME),
                RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString("dimension"))),
                ArrayUtil.toVec3d(nbt.getList("Pos", NbtElement.DOUBLE_TYPE).stream().mapToDouble(e -> ((NbtDouble) e).doubleValue()).toArray()),
                nbt.getList("Rotation", NbtElement.FLOAT_TYPE).getFloat(0)
            );
        }

        @Override
        public int viewDistance() {
            return 0;
        }

        @Override
        public boolean online() {
            return false;
        }

        record OfflinePlayerExploration(Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain, Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures) implements SurveyorExploration {
            public static SurveyorExploration from(NbtCompound nbt) {
                OfflinePlayerExploration mutable = new OfflinePlayerExploration(new HashMap<>(), new HashMap<>());
                mutable.read(nbt);
                return mutable;
            }

            @Override
            public Set<UUID> sharedPlayers() {
                return Set.of();
            }
        }
    }

    class OnlinePlayerSummary implements PlayerSummary {
        private final ServerPlayerEntity player;
        private int viewDistance;
        private final ServerPlayerExploration exploration;

        public OnlinePlayerSummary(ServerPlayerEntity player) {
            this.player = player;
            this.exploration = new ServerPlayerExploration(player, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        }

        @Override
        public SurveyorExploration exploration() {
            return exploration;
        }

        @Override
        public String username() {
            return player.getGameProfile().getName();
        }

        @Override
        public RegistryKey<World> dimension() {
            return player.getWorld().getRegistryKey();
        }

        @Override
        public Vec3d pos() {
            return player.getPos();
        }

        @Override
        public float yaw() {
            return player.getYaw();
        }

        @Override
        public int viewDistance() {
            return viewDistance;
        }

        public void setViewDistance(int viewDistance) {
            this.viewDistance = viewDistance;
        }

        @Override
        public boolean online() {
            return true;
        }

        public void read(NbtCompound nbt) {
            exploration.read(nbt.getCompound(KEY_DATA));
        }

        public void writeNbt(NbtCompound nbt) {
            NbtCompound surveyorNbt = new NbtCompound();
            exploration.write(surveyorNbt);
            surveyorNbt.putString(PlayerSummary.KEY_USERNAME, username());
            nbt.put(PlayerSummary.KEY_DATA, surveyorNbt);
        }


        record ServerPlayerExploration(ServerPlayerEntity player, Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain, Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures) implements SurveyorExploration {
            @Override
            public Set<UUID> sharedPlayers() {
                return Set.of(player.getUuid());
            }

            @Override
            public void mergeRegion(RegistryKey<World> worldKey, ChunkPos regionPos, BitSet bitSet) {
                SurveyorExploration.super.mergeRegion(worldKey, regionPos, bitSet);
                if (player.getServer().isHost(player.getGameProfile())) updateClientForMergeRegion(player.getServerWorld(), regionPos, bitSet);
                // Sync to shared players if they don't have it
            }

            @Override
            public void addChunk(RegistryKey<World> worldKey, ChunkPos pos) {
                SurveyorExploration.super.addChunk(worldKey, pos);
                if (player.getServer().isHost(player.getGameProfile())) updateClientForAddChunk(player.getServerWorld(), pos);
                // Sync to shared players if its unexplored
            }

            @Override
            public void addStructure(RegistryKey<World> worldKey, RegistryKey<Structure> structureKey, ChunkPos pos) {
                SurveyorExploration.super.addStructure(worldKey, structureKey, pos);
                ServerWorld world = player.getServerWorld();
                if (player.getServer().isHost(player.getGameProfile())) updateClientForAddStructure(world, structureKey, pos);
                WorldStructureSummary summary = WorldSummary.of(world).structures();
                new S2CStructuresAddedPacket(Map.of(structureKey, Map.of(pos, summary.get(structureKey, pos))), Map.of(structureKey, summary.getType(structureKey)), MapUtil.asMultiMap(Map.of(structureKey, summary.getTags(structureKey)))).send(player);
                // Send to shared players if they don't have it
            }
        }
    }
}
