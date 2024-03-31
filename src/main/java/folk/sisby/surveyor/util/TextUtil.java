package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;

public class TextUtil {
    public static Text stripInteraction(Text text) {
        NbtCompound nbt = (NbtCompound) Codecs.TEXT.encodeStart(NbtOps.INSTANCE, text).getOrThrow(false, null);
        NbtUtil.removeRecursive(nbt, List.of("hoverEvent", "clickEvent", "insertion"));
        return Codecs.TEXT.decode(NbtOps.INSTANCE, nbt).getOrThrow(false, null).getFirst();
    }
}
