package com.autonomouslogic.commons;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.NonNull;

public class SetUtil {
	/**
	 * Merges the supplied sets into a {@link LinkedHashSet} instance.
	 * @param sets the sets to be merged
	 * @return a set containing all the elements
	 * @param <E> the set element type
	 */
	public static <E> Set<E> mergeCopy(Set<E>... sets) {
		return addAll(new LinkedHashSet<>(), sets);
	}

	/**
	 * Adds all the provided sets to a specific set instance.
	 * @param target the set to copy elements into.
	 * @param sets the sets to be copied
	 * @return the target set is returned
	 * @param <E> the set element type
	 */
	public static <E> Set<E> addAll(@NonNull Set<E> target, Set<E>... sets) {
		for (Set<E> set : sets) {
			target.addAll(set);
		}
		return target;
	}
}
