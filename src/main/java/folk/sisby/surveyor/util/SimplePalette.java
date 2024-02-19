package folk.sisby.surveyor.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimplePalette<T, K> {
    private final BiMap<T, Integer> values = HashBiMap.create();
    private final Map<K, Multiset<Integer>> occurrences = new HashMap<>();

    public List<T> getValues() {
        return values.keySet().stream().sorted(Comparator.comparingInt(values::get)).toList();
    }

    public T get(int index) {
        return values.inverse().get(index);
    }

    public void add(T value) {
        if (!values.containsKey(value)) values.put(value, values.size());
    }

    public int addOccurrence(K key, T value) {
        add(value);
        int index = values.get(value);
        occurrences.computeIfAbsent(key, k -> HashMultiset.create()).add(index);
        return index;
    }

    public void clearOccurrences(K key) {
        occurrences.remove(key);
    }

    public Map<Integer, Integer> remap() {
        Map<Integer, Integer> remap = new Int2IntArrayMap();
        remap.put(-1, -1);
        Map<Integer, T> oldMap = new HashMap<>(values.inverse());
        values.clear();
        Multiset<Integer> combinedOccurrences = HashMultiset.create();
        occurrences.values().forEach(m -> m.elementSet().forEach(i -> combinedOccurrences.add(i, m.count(i))));
        oldMap.keySet().stream().sorted(Comparator.comparingInt(combinedOccurrences::count).reversed()).forEach(index -> {
            remap.put(index, values.size());
            values.put(oldMap.get(index), values.size());
        });
        return remap;
    }
}
