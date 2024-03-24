package folk.sisby.surveyor;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface SurveyorPlayer {
    SurveyorExploration surveyor$getExploration();

    class ServerPlayerExploration implements SurveyorExploration {
        public static final String KEY_DATA = "surveyor";
        public final Map<RegistryKey<World>, Map<ChunkPos, BitSet>> surveyor$exploredTerrain = new HashMap<>();
        public final Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> surveyor$exploredStructures = new HashMap<>();
        public int surveyor$playerViewDistance = -1;

        protected final ServerPlayerEntity player;

        public ServerPlayerExploration(ServerPlayerEntity player) {
            this.player = player;
        }

        @Override
        public Map<RegistryKey<World>, Map<ChunkPos, BitSet>> terrain() {
            return surveyor$exploredTerrain;
        }

        @Override
        public Map<RegistryKey<World>, Map<RegistryKey<Structure>, LongSet>> structures() {
            return surveyor$exploredStructures;
        }

        @Override
        public Set<UUID> sharedPlayers() {
            return Set.of(player.getUuid());
        }

        @Override
        public World getWorld() {
            return player.getWorld();
        }

        @Override
        public @Nullable ServerPlayerEntity getServerPlayer() {
            return player;
        }

        @Override
        public int getViewDistance() {
            return surveyor$playerViewDistance == -1 ? player.getServer().getPlayerManager().getViewDistance() : surveyor$playerViewDistance;
        }
    }
}
