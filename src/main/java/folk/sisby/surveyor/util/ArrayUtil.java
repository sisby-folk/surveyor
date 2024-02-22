package folk.sisby.surveyor.util;

import java.util.Arrays;

public class ArrayUtil {
    public static int[] ofSingle(int value, int size) {
        int[] array = new int[size];
        Arrays.fill(array, value);
        return array;
    }
}
