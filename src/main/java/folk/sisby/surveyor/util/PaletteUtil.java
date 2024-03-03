package folk.sisby.surveyor.util;

import net.minecraft.registry.Registry;
import net.minecraft.util.collection.Int2ObjectBiMap;

public class PaletteUtil {
    public static <T> int idOrAdd(Int2ObjectBiMap<T> palette, Int2ObjectBiMap<Integer> rawPalette, T value, Registry<T> registry) {
        int id = palette.getRawId(value);
        if (id == -1) {
            rawPalette.add(registry.getRawId(value));
            return palette.add(value);
        }
        return id;
    }

    public static <T> int rawIdOrAdd(Int2ObjectBiMap<T> palette, Int2ObjectBiMap<Integer> rawPalette, int value, Registry<T> registry) {
        int id = rawPalette.getRawId(value);
        if (id == -1) {
            palette.add(registry.get(value));
            return rawPalette.add(value);
        }
        return id;
    }
}
