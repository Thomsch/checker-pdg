package org.checkerframework.flexeme;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collection of utility methods for the dataflow analyses.
 */
public class Util {
    /**
     * Merge two maps where the values are {@link Set}.
     *
     * @param left  the first Map
     * @param right the second Map
     * @param <K>   type for map keys
     * @param <V>   type for set values
     * @return a new Map instance containing the keys and values of left and right maps
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
     *
     * @param left  the first set
     * @param right the second set
     * @param <V>   the type of the set elements
     * @return a new set containing the elements of both sets
     */
    private static <V> Set<V> mergeSets(final Set<V> left, final Set<V> right) {
        HashSet<V> result = new HashSet<>(left.size() + right.size());
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    /**
     * Create a new mutable set containing one element. This is a helper method to avoid having to write
     * {@code new HashSet<>(Collections.singleton(value))} everywhere.
     *
     * @param value the value to put in the set
     * @param <V>   the type of the set elements
     * @return a new mutable set containing one element
     */
    public static <V> Set<V> newSet(V value) {
        Set<V> set = new HashSet<>();
        set.add(value);
        return set;
    }
}
