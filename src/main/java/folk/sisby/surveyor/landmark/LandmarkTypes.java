package folk.sisby.surveyor.landmark;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class LandmarkTypes {
    private static final Map<Identifier, LandmarkType<?>> TYPES = new HashMap<>();

    public static boolean containsType(Identifier id) {
        return TYPES.containsKey(id);
    }

    public static LandmarkType<?> getType(Identifier id) {
        return TYPES.get(id);
    }

    public static void register(LandmarkType<?> type) {
        if (containsType(type.getId())) {
            throw new IllegalArgumentException("Multiple landmark types registered to the same ID: %s".formatted(type.getId()));
        }
        TYPES.put(type.getId(), type);
    }

    static {
        register(SimpleLandmark.TYPE);
    }
}
