package org.checkerframework.flexeme;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Util {
    /**
     * Merge two maps where the values are {@link Set}.
     * @param left The first Map
     * @param right The second Map
     * @return A new Map instance containing the keys and values of left and right maps.
     * @param <K> Generic type for map keys
     * @param <V> Generic type for set values
     */
    public static <K, V> Map<K, Set<V>> mergeSetMaps(Map<K, Set<V>> left, Map<K, Set<V>> right) {
        Map<K, Set<V>> result = new HashMap<>(left.size() + right.size());

        // Optimize merge performance loss in big hashmaps (e.g., after left long branch).
        Map<K, Set<V>> big;
        Map<K, Set<V>> small;
        if (left.size() > right.size()) {
            big = left;
            small = right;
        } else {
            big = right;
            small = left;
        }

        result.putAll(big); // May be faster to not copy and add directly to largest map. Verify that not copying is safe.
        small.forEach((key, value) -> result.merge(key, value, Util::mergeSets));
        return result;
    }

    /**
     * Merge two sets.
     * @param left The first set.
     * @param right The second set.
     * @return A new set containing the elements of both sets.
     * @param <V> The type of the set.
     */
    private static <V> Set<V> mergeSets(final Set<V> left, final Set<V> right) {
        HashSet<V> result = new HashSet<>(left.size() + right.size());
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    /**
     * Create a new mutable set. This is a helper method to avoid having to write
     * {@code new HashSet<>()} everywhere.
     * @param value The value to put in the set.
     * @return A new mutable set.
     * @param <V> The type of the set.
     */
    public static <V> Set<V> newSet(V value) {
        Set<V> set = new HashSet<>();
        set.add(value);
        return set;
    }
}
