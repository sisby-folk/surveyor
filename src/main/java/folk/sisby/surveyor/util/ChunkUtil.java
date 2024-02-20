package folk.sisby.surveyor.util;

import net.minecraft.world.chunk.Chunk;

import java.util.Arrays;

public class ChunkUtil {
    public static Integer airCount(Chunk chunk) {
        return Arrays.stream(chunk.getSectionArray()).mapToInt(s -> 4096 - s.nonEmptyBlockCount).sum();
    }
}
