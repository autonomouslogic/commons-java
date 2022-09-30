package com.autonomouslogic.commons;

import com.autonomouslogic.commons.collection.ConcatList;

import java.util.List;

public class ListUtil {
	public static <E> ConcatList<E> concat(List<E>... lists) {
		return new ConcatList<>(List.of(lists));
	}
}
