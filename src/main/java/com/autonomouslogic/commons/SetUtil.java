package com.autonomouslogic.commons;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.NonNull;

/**
 * Utility methods for working with sets.
 *
 * <p>Provides simple operations for merging multiple sets without manually iterating or manually
 * calling {@link Set#addAll(java.util.Collection)}.
 */
public class SetUtil {
	private SetUtil() {}

	/**
	 * Merges multiple sets into a new {@link LinkedHashSet}.
	 *
	 * <p>Creates a new LinkedHashSet containing all unique elements from all input sets.
	 * Iteration order is preserved according to insertion order (LinkedHashSet behavior).
	 * Duplicates across sets are automatically handled by Set semantics.
	 *
	 * <p><b>Example:</b>
	 * <pre>{@code
	 * Set<String> colors1 = Set.of("red", "blue");
	 * Set<String> colors2 = Set.of("blue", "green");
	 * Set<String> merged = SetUtil.mergeCopy(colors1, colors2);
	 * // Result: {red, blue, green} (no duplicate blue)
	 * }</pre>
	 *
	 * @param sets the sets to merge (varargs, can be empty)
	 * @return a new LinkedHashSet containing all unique elements from all input sets
	 * @param <E> the element type
	 */
	public static <E> Set<E> mergeCopy(Set<E>... sets) {
		return addAll(new LinkedHashSet<>(), sets);
	}

	/**
	 * Adds all elements from multiple sets into a target set.
	 *
	 * <p>Modifies the target set in place, adding all elements from each input set.
	 * Useful when you already have a set and want to add elements from other sets without creating
	 * an intermediate collection.
	 *
	 * <p><b>Example:</b>
	 * <pre>{@code
	 * Set<String> existing = new HashSet<>();
	 * existing.add("apple");
	 * SetUtil.addAll(existing, Set.of("banana"), Set.of("cherry"));
	 * // existing now contains: {apple, banana, cherry}
	 * }</pre>
	 *
	 * @param target the set to add elements to (modified in place)
	 * @param sets the sets whose elements should be added (varargs, can be empty)
	 * @return the target set (for chaining)
	 * @param <E> the element type
	 * @throws NullPointerException if target is null
	 */
	public static <E> Set<E> addAll(@NonNull Set<E> target, Set<E>... sets) {
		for (Set<E> set : sets) {
			target.addAll(set);
		}
		return target;
	}
}
