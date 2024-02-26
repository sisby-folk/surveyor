package folk.sisby.surveyor.landmark;

import com.mojang.serialization.Codec;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Landmarks {
    public static final String KEY_LANDMARKS = "landmarks";

    private static final Map<Identifier, Codec<? extends Landmark<?>>> CODECS = new HashMap<>();

    public static boolean containsType(Identifier id) {
        return CODECS.containsKey(id);
    }

    public static Codec<? extends Landmark<?>> getCodec(Identifier id) {
        return CODECS.get(id);
    }

    public static NbtCompound writeNbt(Set<Landmark<?>> landmarks, NbtCompound nbt) {
        NbtList landmarkList = new NbtList();
        for (Landmark<?> landmark : landmarks) {
            // TODO landmarkList.add(landmark.codec().encodeStart(NbtOps.INSTANCE, landmark).getOrThrow(false, Surveyor.LOGGER::error));
        }
        nbt.put(KEY_LANDMARKS, landmarkList);
        return nbt;
    }

    public static Landmark<?> landmarkFromNbt(NbtCompound nbt) {
        Identifier type = new Identifier(nbt.getString("type"));
        return getCodec(type).decode(NbtOps.INSTANCE, nbt).getOrThrow(false, Surveyor.LOGGER::error).getFirst();
    }

    public static Set<Landmark<?>> fromNbt(NbtCompound nbt) {
        Set<Landmark<?>> outSet = new HashSet<>();
        for (NbtElement landmarkElement : nbt.getList(KEY_LANDMARKS, NbtElement.COMPOUND_TYPE)) {
            outSet.add(landmarkFromNbt((NbtCompound) landmarkElement));
        }
        return outSet;
    }

    public static void register(Identifier id, Codec<? extends Landmark<?>> codec) {
        if (containsType(id)) {
            throw new IllegalArgumentException("Multiple landmark types registered to the same ID: %s".formatted(id));
        }
        CODECS.put(id, codec);
    }

    static {
        register(SimplePointLandmark.ID, SimplePointLandmark.CODEC);
    }
}
