package folk.sisby.surveyor.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class ListUtil {

    public static <T> List<T> splitSet(List<T> list, BitSet set) {
        List<T> outList = new ArrayList<>();
        set.stream().forEach(i -> outList.add(list.get(i)));
        return outList;
    }
}
