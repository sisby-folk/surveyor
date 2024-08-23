package folk.sisby.surveyor.landmark;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.ServerSummary;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.SurveyorExploration;
import folk.sisby.surveyor.config.NetworkMode;
import folk.sisby.surveyor.config.SystemMode;
import folk.sisby.surveyor.packet.SyncLandmarksAddedPacket;
import folk.sisby.surveyor.packet.SyncLandmarksRemovedPacket;
import folk.sisby.surveyor.util.MapUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldLandmarks {
	protected final RegistryKey<World> worldKey;
	protected final Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks = new ConcurrentHashMap<>();
	protected boolean dirty = false;

	public WorldLandmarks(RegistryKey<World> worldKey, Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks) {
		this.worldKey = worldKey;
		this.landmarks.putAll(landmarks);
	}

	public static WorldLandmarks load(World world, File folder) {
		NbtCompound landmarkNbt = new NbtCompound();
		File landmarksFile = new File(folder, "landmarks.dat");
		if (landmarksFile.exists()) {
			try {
				landmarkNbt = NbtIo.readCompressed(landmarksFile);
			} catch (IOException e) {
				Surveyor.LOGGER.error("[Surveyor] Error loading landmarks file for {}.", world.getRegistryKey().getValue(), e);
			}
		}
		var landmarks = Landmarks.fromNbt(landmarkNbt);
		if (Surveyor.CONFIG.migrateSingleplayerLandmarksFrom05to06 && world instanceof ServerWorld sw && sw.getServer().isSingleplayer()) { // TODO: Remove in next release
			for (Landmark<?> landmark : new HashSet<>(landmarks.values().stream().flatMap(m -> m.values().stream()).toList())) {
				if (landmark instanceof SimplePointLandmark spl && spl.owner() != null) {
					landmarks.computeIfAbsent(spl.type(), type -> new HashMap<>()).put(spl.pos(), new SimplePointLandmark(spl.pos(), ServerSummary.HOST, spl.color(), spl.name(), spl.texture()));
				}
				if (landmark instanceof PlayerDeathLandmark pdl && pdl.owner() != null) {
					landmarks.computeIfAbsent(pdl.type(), type -> new HashMap<>()).put(pdl.pos(), new PlayerDeathLandmark(pdl.pos(), ServerSummary.HOST, pdl.name(), pdl.created(), pdl.seed()));
				}
			}
		}
		return new WorldLandmarks(world.getRegistryKey(), landmarks);
	}

	public boolean contains(LandmarkType<?> type, BlockPos pos) {
		return landmarks.containsKey(type) && landmarks.get(type).containsKey(pos);
	}

	@SuppressWarnings("unchecked")
	public <T extends Landmark<T>> Landmark<T> get(LandmarkType<T> type, BlockPos pos) {
		return (Landmark<T>) landmarks.get(type).get(pos);
	}

	@SuppressWarnings("unchecked")
	public <T extends Landmark<T>> Map<BlockPos, T> asMap(LandmarkType<T> type, SurveyorExploration exploration) {
		Map<BlockPos, T> outMap = new HashMap<>();
		if (landmarks.containsKey(type)) landmarks.get(type).forEach((pos, landmark) -> {
			if (exploration == null || exploration.exploredLandmark(worldKey, landmark)) outMap.put(pos, (T) landmark);
		});
		return outMap;
	}

	public Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> asMap(SurveyorExploration exploration) {
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> outmap = new HashMap<>();
		landmarks.forEach((type, map) -> map.forEach((pos, landmark) -> {
			if (exploration == null || exploration.exploredLandmark(worldKey, landmark)) outmap.computeIfAbsent(type, t -> new HashMap<>()).put(pos, landmark);
		}));
		return outmap;
	}

	public Multimap<LandmarkType<?>, BlockPos> keySet(SurveyorExploration exploration) {
		Multimap<LandmarkType<?>, BlockPos> outMap = HashMultimap.create();
		landmarks.forEach((type, map) -> map.forEach((pos, landmark) -> {
			if (exploration == null || exploration.exploredLandmark(worldKey, landmark)) outMap.put(type, pos);
		}));
		return outMap;
	}

	public void handleChanged(World world, Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed, boolean local, @Nullable ServerPlayerEntity sender) {
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarksAddedChanged = new HashMap<>();
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarksRemoved = new HashMap<>();
		changed.forEach((type, map) -> map.forEach((pos, mark) -> {
			if (contains(type, pos)) {
				landmarksAddedChanged.computeIfAbsent(type, t -> new HashMap<>()).put(pos, mark);
			} else {
				landmarksRemoved.computeIfAbsent(type, t -> new HashMap<>()).put(pos, mark);
			}
		}));
		if (!landmarksRemoved.isEmpty()) SurveyorEvents.Invoke.landmarksRemoved(world, MapUtil.keyMultiMap(landmarksRemoved));
		if (!landmarksAddedChanged.isEmpty()) SurveyorEvents.Invoke.landmarksAdded(world, MapUtil.keyMultiMap(landmarksAddedChanged));
		if (!local) {
			Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> waypointsRemoved = new HashMap<>();
			Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> waypointsAddedChanged = new HashMap<>();
			landmarksRemoved.forEach((type, map) -> map.forEach((pos, mark) -> {
				if (mark.owner() != null) waypointsRemoved.computeIfAbsent(type, t -> new HashMap<>()).put(pos, mark);
			}));
			waypointsRemoved.forEach((type, map) -> map.forEach((pos, mark) -> {
				landmarksRemoved.get(type).remove(pos);
				if (landmarksRemoved.get(type).isEmpty()) landmarksRemoved.remove(type);
			}));
			landmarksAddedChanged.forEach((type, map) -> map.forEach((pos, mark) -> {
				if (mark.owner() != null) waypointsAddedChanged.computeIfAbsent(type, t -> new HashMap<>()).put(pos, mark);
			}));
			waypointsAddedChanged.forEach((type, map) -> map.forEach((pos, mark) -> {
				landmarksAddedChanged.get(type).remove(pos);
				if (landmarksAddedChanged.get(type).isEmpty()) landmarksAddedChanged.remove(type);
			}));

			if (!landmarksRemoved.isEmpty()) new SyncLandmarksRemovedPacket(MapUtil.keyMultiMap(landmarksRemoved)).send(sender, world, Surveyor.CONFIG.networking.landmarks);
			if (!landmarksAddedChanged.isEmpty()) new SyncLandmarksAddedPacket(landmarksAddedChanged).send(sender, world, Surveyor.CONFIG.networking.landmarks);
			if (!waypointsRemoved.isEmpty()) new SyncLandmarksRemovedPacket(MapUtil.keyMultiMap(waypointsRemoved)).send(sender, world, Surveyor.CONFIG.networking.waypoints);
			if (!waypointsAddedChanged.isEmpty()) new SyncLandmarksAddedPacket(waypointsAddedChanged).send(sender, world, Surveyor.CONFIG.networking.waypoints);
		}
	}

	public Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> putForBatch(Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed, Landmark<?> landmark) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return changed;
		landmarks.computeIfAbsent(landmark.type(), t -> new ConcurrentHashMap<>()).put(landmark.pos(), landmark);
		dirty();
		changed.computeIfAbsent(landmark.type(), t -> new HashMap<>()).put(landmark.pos(), landmark);
		return changed;
	}

	public void putLocal(World world, Landmark<?> landmark) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return;
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = landmark.put(new HashMap<>(), world, this);
		handleChanged(world, changed, true, null);
	}

	public void put(World world, Landmark<?> landmark) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return;
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = landmark.put(new HashMap<>(), world, this);
		handleChanged(world, changed, false, null);
	}

	public void put(ServerPlayerEntity sender, ServerWorld world, Landmark<?> landmark) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return;
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = landmark.put(new HashMap<>(), world, this);
		handleChanged(world, changed, false, sender);
	}

	public Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> removeForBatch(Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed, LandmarkType<?> type, BlockPos pos) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return changed;
		if (!landmarks.containsKey(type) || !landmarks.get(type).containsKey(pos)) return changed;
		Landmark<?> landmark = landmarks.get(type).remove(pos);
		if (landmarks.get(type).isEmpty()) landmarks.remove(type);
		dirty();
		changed.computeIfAbsent(type, t -> new HashMap<>()).put(pos, landmark);
		return changed;
	}

	public void removeLocal(World world, LandmarkType<?> type, BlockPos pos) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return;
		if (!landmarks.containsKey(type) || !landmarks.get(type).containsKey(pos)) return;
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = landmarks.get(type).get(pos).remove(new HashMap<>(), world, this);
		handleChanged(world, changed, true, null);
	}

	public void remove(World world, LandmarkType<?> type, BlockPos pos) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return;
		if (!landmarks.containsKey(type) || !landmarks.get(type).containsKey(pos)) return;
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = landmarks.get(type).get(pos).remove(new HashMap<>(), world, this);
		handleChanged(world, changed, false, null);
	}

	public void remove(ServerPlayerEntity sender, ServerWorld world, LandmarkType<?> type, BlockPos pos) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return;
		if (!landmarks.containsKey(type) || !landmarks.get(type).containsKey(pos)) return;
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = landmarks.get(type).get(pos).remove(new HashMap<>(), world, this);
		handleChanged(world, changed, false, sender);
	}

	public void removeAll(World world, Class<?> clazz, BlockPos pos) {
		if (Surveyor.CONFIG.landmarks == SystemMode.FROZEN) return;
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = new HashMap<>();
		landmarks.forEach((type, map) -> {
			if (map.containsKey(pos)) {
				Landmark<?> landmark = map.get(pos);
				if (clazz.isAssignableFrom(landmark.getClass())) {
					landmark.remove(changed, world, this);
				}
			}
		});
		handleChanged(world, changed, false, null);
	}

	public int save(World world, File folder) {
		if (isDirty()) {
			File landmarksFile = new File(folder, "landmarks.dat");
			try {
				NbtIo.writeCompressed(Landmarks.writeNbt(landmarks, new NbtCompound()), landmarksFile);
				dirty = false;
			} catch (IOException e) {
				Surveyor.LOGGER.error("[Surveyor] Error writing landmarks file for {}.", world.getRegistryKey().getValue(), e);
			}
			return landmarks.values().stream().mapToInt(Map::size).sum();
		}
		return 0;
	}

	public void readUpdatePacket(World world, SyncLandmarksAddedPacket packet, @Nullable ServerPlayerEntity sender) {
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = new HashMap<>();
		packet.landmarks().forEach((type, map) -> map.forEach((pos, landmark) -> {
			boolean waypoint = landmark.owner() != null;
			boolean owned = sender == null || Surveyor.getUuid(sender).equals(landmark.owner());
			if (owned && (waypoint && Surveyor.CONFIG.networking.waypoints.atLeast(NetworkMode.SOLO) || !waypoint && Surveyor.CONFIG.networking.landmarks.atLeast(NetworkMode.SOLO))) {
				putForBatch(changed, landmark);
			}
		}));
		if (!changed.isEmpty()) handleChanged(world, changed, sender == null, sender);
	}

	public void readUpdatePacket(World world, SyncLandmarksRemovedPacket packet, @Nullable ServerPlayerEntity sender) {
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> changed = new HashMap<>();
		packet.landmarks().forEach((type, pos) -> {
			if (!contains(type, pos)) return;
			Landmark<?> landmark = get(type, pos);
			boolean waypoint = landmark.owner() != null;
			boolean owned = sender == null || Surveyor.getUuid(sender).equals(landmark.owner());
			if (owned && (waypoint && Surveyor.CONFIG.networking.waypoints.atLeast(NetworkMode.SOLO) || !waypoint && Surveyor.CONFIG.networking.landmarks.atLeast(NetworkMode.SOLO))) {
				removeForBatch(changed, type, pos);
			}
		});
		if (!changed.isEmpty()) handleChanged(world, changed, sender == null, sender);
	}

	public SyncLandmarksAddedPacket createUpdatePacket(Multimap<LandmarkType<?>, BlockPos> keySet) {
		Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks = new HashMap<>();
		keySet.forEach((type, pos) -> landmarks.computeIfAbsent(type, k -> new HashMap<>()).put(pos, get(type, pos)));
		return new SyncLandmarksAddedPacket(landmarks);
	}

	public boolean isDirty() {
		return dirty && Surveyor.CONFIG.landmarks != SystemMode.FROZEN;
	}

	private void dirty() {
		dirty = true;
	}
}
