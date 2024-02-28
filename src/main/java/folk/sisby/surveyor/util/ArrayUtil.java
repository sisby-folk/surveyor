package folk.sisby.surveyor.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Arrays;

public class ArrayUtil {
    public static int[] ofSingle(int value, int size) {
        int[] array = new int[size];
        Arrays.fill(array, value);
        return array;
    }

    public static int distinctCount(int[] ints) {
        int count = 0;
        IntSet counted = new IntOpenHashSet();
        for(int i : ints) {
            if (counted.contains(i)) continue;
            counted.add(i);
            count++;
        }
        return count;
    }
}
