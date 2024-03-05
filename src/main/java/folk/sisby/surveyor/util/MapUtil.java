package folk.sisby.surveyor.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

public class MapUtil {
    public static <K, V> Multimap<K, V> hashMultiMapOf(Map<K, Collection<V>> asMap) {
        Multimap<K, V> map = HashMultimap.create();
        asMap.forEach(map::putAll);
        return map;
    }
}
