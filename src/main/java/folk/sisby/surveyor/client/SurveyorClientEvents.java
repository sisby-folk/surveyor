package folk.sisby.surveyor.client;

import folk.sisby.surveyor.WorldSummary;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class SurveyorClientEvents {
    private static final Map<Identifier, ClientWorldLoad> clientWorldLoadHandlers = new HashMap<>();
    private static final Map<Identifier, ClientPlayerLoad> clientPlayerLoadHandlers = new HashMap<>();
    public static boolean INITIALIZING_WORLD = false;

    public static class Invoke {
        public static void clientWorldLoad(ClientWorld world, WorldSummary worldSummary) {
            clientWorldLoadHandlers.forEach((id, handler) -> handler.onClientWorldLoad(world, worldSummary));
        }

        public static void clientPlayerLoad(ClientWorld world, WorldSummary worldSummary, ClientPlayerEntity player) {
            clientPlayerLoadHandlers.forEach((id, handler) -> handler.onClientPlayerLoad(world, worldSummary, player));
        }
    }

    public static class Register {
        public static void clientWorldLoad(Identifier id, ClientWorldLoad handler) {
            clientWorldLoadHandlers.put(id, handler);
        }

        public static void clientPlayerLoad(Identifier id, ClientPlayerLoad handler) {
            clientPlayerLoadHandlers.put(id, handler);
        }
    }

    @FunctionalInterface
    public interface ClientWorldLoad {
        void onClientWorldLoad(ClientWorld world, WorldSummary worldSummary);
    }

    @FunctionalInterface
    public interface ClientPlayerLoad {
        void onClientPlayerLoad(ClientWorld world, WorldSummary worldSummary, ClientPlayerEntity player);
    }
}
