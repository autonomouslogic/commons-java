package com.autonomouslogic.commons.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

public class ConcatList<E> extends ConcatCollection<E> implements List<E> {
	private final List<List<E>> lists;

	public ConcatList(List<List<E>> lists) {
		super(new ArrayList<>(lists));
		this.lists = new ArrayList<>(lists);
	}

	@Override
	public E get(int i) {
		validateIndex(i);
		var min = 0;
		for (List<E> list : lists) {
			var s = list.size();
			var max = min + s;
			if (i < max) {
				return list.get(i - min);
			}
			min = max;
		}
		throw new IllegalStateException();
	}

	@Override
	public E set(int i, E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int i, E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E remove(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
		var offset = 0;
		for (List<E> list : lists) {
			var i = list.indexOf(o);
			if (i >= 0) {
				return offset + i;
			}
			offset += list.size();
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		var n = lists.size();
		for (int l = n - 1; l >= 0; l--) {
			var list = lists.get(l);
			var i = list.lastIndexOf(o);
			if (i >= 0) {
				for (int r = l - 1; r >= 0; r--) {
					i += lists.get(r).size();
				}
				return i;
			}
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<E> listIterator(int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<E> subList(int i, int i1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sort(Comparator<? super E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int i, Collection<? extends E> collection) {
		throw new UnsupportedOperationException();
	}

	private void validateIndex(int index) {
		var size = size();
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
	}
}
