package folk.sisby.surveyor;

import folk.sisby.surveyor.packet.S2CStructuresAddedPacket;
import folk.sisby.surveyor.packet.S2CUpdateRegionPacket;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.util.ArrayUtil;
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
import java.util.HashSet;
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
        public OfflinePlayerSummary(UUID uuid, NbtCompound nbt) {
            this(
                OfflinePlayerExploration.from(uuid, nbt.getCompound(KEY_DATA)),
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

        public record OfflinePlayerExploration(Set<UUID> sharedPlayers, Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain, Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures) implements SurveyorExploration {
            public static OfflinePlayerExploration ofMerged(Set<SurveyorExploration> explorations) {
                Set<UUID> sharedPlayers = new HashSet<>();
                Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain = new HashMap<>();
                Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures = new HashMap<>();
                OfflinePlayerExploration outExploration = new OfflinePlayerExploration(sharedPlayers, terrain, structures);
                for (SurveyorExploration exploration : explorations) {
                    sharedPlayers.addAll(exploration.sharedPlayers());
                    exploration.terrain().forEach((wKey, map) -> map.forEach((rPos, bits) -> outExploration.mergeRegion(wKey, rPos, bits)));
                    exploration.structures().forEach((wKey, map) -> map.forEach((sKey, longs) -> outExploration.mergeStructures(wKey, sKey, longs)));
                }
                return outExploration;
            }

            public static SurveyorExploration from(UUID uuid, NbtCompound nbt) {
                OfflinePlayerExploration mutable = new OfflinePlayerExploration(new HashSet<>(Set.of(uuid)), new HashMap<>(), new HashMap<>());
                mutable.read(nbt);
                return mutable;
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


        public record ServerPlayerExploration(ServerPlayerEntity player, Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain, Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures) implements SurveyorExploration {
            @Override
            public Set<UUID> sharedPlayers() {
                return Set.of(player.getUuid());
            }

            @Override
            public void mergeRegion(RegistryKey<World> worldKey, ChunkPos regionPos, BitSet bitSet) { // This method is currently unused for server players, but its implemented anyway
                SurveyorExploration.super.mergeRegion(worldKey, regionPos, bitSet);
                if (player.getServer().isHost(player.getGameProfile())) updateClientForMergeRegion(player.getServerWorld(), regionPos, bitSet);
                for (ServerPlayerEntity friend : ServerSummary.of(player.getServer()).groupOtherServerPlayers(player.getUuid(), player.getServer())) {
                    if (friend.getWorld().getRegistryKey().equals(worldKey)) {
                        SurveyorExploration friendExploration = SurveyorExploration.of(friend);
                        BitSet sendSet = (BitSet) bitSet.clone();
                        if (friendExploration.terrain().containsKey(worldKey) && friendExploration.terrain().get(worldKey).containsKey(regionPos)) sendSet.andNot(friendExploration.terrain().get(worldKey).get(regionPos));
                        if (!sendSet.isEmpty()) new S2CUpdateRegionPacket(true, regionPos, WorldSummary.of(player.getWorld()).terrain().getRegion(regionPos), sendSet).send(friend);
                    }
                }
            }

            @Override
            public void addChunk(RegistryKey<World> worldKey, ChunkPos pos) {
                SurveyorExploration.super.addChunk(worldKey, pos);
                if (player.getServer().isHost(player.getGameProfile())) updateClientForAddChunk(player.getServerWorld(), pos);
                for (ServerPlayerEntity friend : ServerSummary.of(player.getServer()).groupOtherServerPlayers(player.getUuid(), player.getServer())) {
                    if (friend.getWorld().getRegistryKey().equals(worldKey) && !SurveyorExploration.of(friend).exploredChunk(worldKey, pos)) {
                        ChunkPos regionPos = new ChunkPos(pos.getRegionX(), pos.getRegionZ());
                        RegionSummary region = WorldSummary.of(player.getServer().getWorld(worldKey)).terrain().getRegion(regionPos);
                        BitSet sendSet = new BitSet();
                        sendSet.set(RegionSummary.bitForChunk(pos));
                        new S2CUpdateRegionPacket(true, regionPos, region, sendSet).send(friend);
                    }
                }
            }

            @Override
            public void addStructure(RegistryKey<World> worldKey, RegistryKey<Structure> structureKey, ChunkPos pos) {
                SurveyorExploration.super.addStructure(worldKey, structureKey, pos);
                ServerWorld world = player.getServerWorld();
                if (player.getServer().isHost(player.getGameProfile())) updateClientForAddStructure(world, structureKey, pos);
                WorldStructureSummary summary = WorldSummary.of(world).structures();
                S2CStructuresAddedPacket.of(false, structureKey, pos, summary).send(player);
                for (ServerPlayerEntity friend : ServerSummary.of(player.getServer()).groupOtherServerPlayers(player.getUuid(), player.getServer())) {
                    if (friend.getWorld().getRegistryKey().equals(worldKey) && !SurveyorExploration.of(friend).exploredStructure(worldKey, structureKey, pos)) {
                        S2CStructuresAddedPacket.of(true, structureKey, pos, summary).send(friend);
                    }
                }
            }
        }
    }
}
