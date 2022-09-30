package com.autonomouslogic.commons;

import com.autonomouslogic.commons.collection.ConcatList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtil {
	public static <E> ConcatList<E> concat(List<E>... lists) {
		return new ConcatList<>(List.of(lists));
	}

	public static <E> List<E> concatCopy(List<E>... lists) {
		var n = Arrays.stream(lists)
			.mapToInt(List::size)
			.sum();
		List<E> concat = new ArrayList<>(n);
		for (List<E> list : lists) {
			concat.addAll(list);
		}
		return concat;
	}
}
