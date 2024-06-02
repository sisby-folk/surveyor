package folk.sisby.surveyor.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtil {
    public static <K, V> Multimap<K, V> asMultiMap(Map<K, ? extends Collection<V>> asMap) {
        Multimap<K, V> map = HashMultimap.create();
        asMap.forEach(map::putAll);
        return map;
    }

    public static <K, V> Multimap<K, V> keyMultiMap(Map<K, ? extends Map<V, ?>> asMap) {
        Multimap<K, V> map = HashMultimap.create();
        asMap.forEach((key, innerMap) -> map.putAll(key, new HashSet<>(innerMap.keySet())));
        return map;
    }

    public static <K, V> Map<K, List<V>> asListMap(Multimap<K, V> multimap) {
        return multimap.asMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }
}
