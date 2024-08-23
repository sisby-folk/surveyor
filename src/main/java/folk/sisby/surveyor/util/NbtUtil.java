package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.Collection;

public class NbtUtil {
	public static void removeRecursive(NbtCompound nbt, Collection<String> keys) {
		keys.forEach(nbt::remove);
		for (String key : nbt.getKeys()) {
			if (nbt.contains(key, NbtElement.COMPOUND_TYPE)) {
				removeRecursive(nbt.getCompound(key), keys);
			} else if (nbt.contains(key, NbtElement.LIST_TYPE)) {
				for (NbtElement listNbt : nbt.getList(key, NbtElement.COMPOUND_TYPE)) {
					removeRecursive((NbtCompound) listNbt, keys);
				}
			}
		}
	}
}
