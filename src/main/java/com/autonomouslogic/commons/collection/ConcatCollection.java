package com.autonomouslogic.commons.collection;

import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
abstract class ConcatCollection<E> implements Collection<E> {
	private final List<Collection<E>> collections;

	@Override
	public int size() {
		return collections.stream().mapToInt(Collection::size).sum();
	}

	@Override
	public boolean isEmpty() {
		for (Collection<E> collection : collections) {
			if (!collection.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean contains(Object o) {
		for (Collection<E> collection : collections) {
			if (collection.contains(o)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<E> iterator() {
		int n = collections.size();
		var iterators = new Iterator[n];
		for (int i = 0; i < n; i++) {
			iterators[i] = collections.get(i).iterator();
		}
		return Iterators.concat(iterators);
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] ts) {
		throw new UnsupportedOperationException();
	}
}
