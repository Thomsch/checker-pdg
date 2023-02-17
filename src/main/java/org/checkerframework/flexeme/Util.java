package org.checkerframework.flexeme;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Util {
    /**
     * Merge two Maps.
     * @param a The first Map
     * @param b The second Map
     * @param mergeValues Function that defines how to merge two of the values together.
     * @return A new Map instance containing the keys and values of a and b maps.
     * @param <K> Generic type for map keys
     * @param <V> Generic type for map values
     */
    static public <K, V> Map<K, V> mergeHashMaps(Map<K, V> a, Map<K, V> b, BiFunction<V, V, V> mergeValues) {
        Map<K, V> result = new HashMap<>(a.size() + b.size());

        // Optimize merge performance loss in big hashmaps (e.g., after a long branch).
        Map<K, V> big;
        Map<K, V> small;
        if (a.size() > b.size()) {
            big = a;
            small = b;
        } else {
            big = b;
            small = a;
        }

        result.putAll(big);
        small.forEach((key, value) -> result.merge(key, value, mergeValues));
        return result;
    }
}
