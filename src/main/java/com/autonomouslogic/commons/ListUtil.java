package com.autonomouslogic.commons;

import com.autonomouslogic.commons.collection.ConcatList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtil {
	private ListUtil() {
	}

	/**
	 * Concatenates the supplied list without explicitly copying every single element.
	 * Limited functionality is provided for interacting with the underlying lists.
	 * @param lists the lists to concatenate
	 * @return a concatenated list instance
	 * @param <E> the list element type
	 */
	public static <E> ConcatList<E> concat(List<E>... lists) {
		return new ConcatList<>(List.of(lists));
	}

	/**
	 * Concatenates the supplied lists into an {@link ArrayList} by copying every element.
	 * @param lists the lists to concatenate
	 * @return a concatenated list copy
	 * @param <E> the list element type
	 */
	public static <E> List<E> concatCopy(List<E>... lists) {
		var n = Arrays.stream(lists).mapToInt(List::size).sum();
		List<E> concat = new ArrayList<>(n);
		for (List<E> list : lists) {
			concat.addAll(list);
		}
		return concat;
	}
}
