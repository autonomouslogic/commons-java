package com.autonomouslogic.commons.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

/**
 * A read-only view that treats multiple lists as a single list.
 *
 * <p>Combines multiple lists without copying elements. Useful for accessing multiple lists as a unified
 * sequence with indexed access and search operations, without allocating a new collection.
 *
 * <p><b>Usage:</b> Create instances via {@link com.autonomouslogic.commons.ListUtil#concat(List[])}
 * rather than calling this constructor directly.
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * import com.autonomouslogic.commons.ListUtil;
 *
 * List<String> first = List.of("a", "b");
 * List<String> second = List.of("c", "d");
 * ConcatList<String> combined = ListUtil.concat(first, second);
 *
 * combined.size();        // 4
 * combined.get(0);        // "a"
 * combined.get(2);        // "c"
 * combined.contains("b"); // true
 *
 * for (String item : combined) {
 *     System.out.println(item); // prints: a, b, c, d
 * }
 * }</pre>
 *
 * <p><b>Supported operations:</b>
 * <ul>
 * <li>Indexed access: {@code get(index)}, {@code indexOf()}, {@code lastIndexOf()}
 * <li>Iteration: {@code iterator()}, {@code size()}, {@code isEmpty()}
 * <li>Searching: {@code contains()}
 * </ul>
 *
 * <p><b>Unsupported operations:</b> All mutating operations throw {@link UnsupportedOperationException}
 * (e.g., {@code set()}, {@code add()}, {@code remove()}, {@code sort()}).
 *
 * <p><b>Performance:</b> Operations like {@code get()} traverse lists to find the target index, so
 * sequential access via iteration is more efficient than random indexing.
 *
 * @param <E> the type of elements in the lists
 * @see #ConcatList(List) constructor for usage
 */
public class ConcatList<E> extends ConcatCollection<E> implements List<E> {
	private final List<List<E>> lists;

	/**
	 * Creates a view combining multiple lists.
	 *
	 * @param lists a list of lists to combine (the list itself and its contents are defensively copied)
	 * @throws NullPointerException if {@code lists} or any contained list is null
	 */
	public ConcatList(List<List<E>> lists) {
		super(new ArrayList<>(lists));
		this.lists = new ArrayList<>(lists);
	}

	/**
	 * Returns the element at the given index, treating the combined list as a single sequence.
	 *
	 * <p>The index is relative to the start of the concatenated lists. For example, with lists
	 * {@code [a, b]} and {@code [c, d]}, index 2 returns {@code c}.
	 *
	 * @param i the index of the element
	 * @return the element at the specified index
	 * @throws IndexOutOfBoundsException if the index is out of bounds
	 */
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
