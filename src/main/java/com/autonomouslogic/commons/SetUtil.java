package com.autonomouslogic.commons;

import java.util.LinkedHashSet;
import java.util.Set;

public class SetUtil {
	public static <E> Set<E> mergeCopy(Set<E>... sets) {
		return addAll(new LinkedHashSet<>(), sets);
	}

	public static <E> Set<E> addAll(Set<E> target, Set<E>... sets) {
		for (Set<E> set : sets) {
			target.addAll(set);
		}
		return target;
	}
}
