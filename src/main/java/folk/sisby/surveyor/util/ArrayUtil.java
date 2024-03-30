package folk.sisby.surveyor.util;

import java.util.Arrays;

public class ArrayUtil {
    public static int[] ofSingle(int value, int size) {
        int[] array = new int[size];
        Arrays.fill(array, value);
        return array;
    }

    public static boolean isSingle(int[] ints) {
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        for(int i : ints) {
            if (i > min) min = i;
            if (i < max) max = i;
        }
        return max == min;
    }
}
