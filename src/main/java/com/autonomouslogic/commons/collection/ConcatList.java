package com.autonomouslogic.commons.collection;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ConcatList<E> extends ConcatCollection<E> implements List<E> {
	private final List<List<E>> lists;

	public ConcatList(List<List<E>> lists) {
		super(new ArrayList<>(lists));
		this.lists = new ArrayList<>(lists);
	}

	@Override
	public E get(int i) {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException();
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
}
