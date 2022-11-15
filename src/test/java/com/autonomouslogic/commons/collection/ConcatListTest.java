package com.autonomouslogic.commons.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autonomouslogic.commons.ListUtil;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConcatListTest {
	ConcatList<String> concat = ListUtil.concat(List.of("a", "b"), List.of("c", "d", "a"));

	@Test
	void testEquals() {
		assertEquals(List.of("a", "b", "c", "d", "a"), concat);
	}

	@Test
	void testSize() {
		assertEquals(5, concat.size());
	}

	@Test
	void testGet() {
		assertEquals("a", concat.get(0));
		assertEquals("b", concat.get(1));
		assertEquals("c", concat.get(2));
		assertEquals("d", concat.get(3));
		assertEquals("a", concat.get(4));
		assertThrows(IndexOutOfBoundsException.class, () -> concat.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> concat.get(5));
	}

	@Test
	void testContains() {
		assertTrue(concat.contains("a"));
		assertTrue(concat.contains("b"));
		assertTrue(concat.contains("c"));
		assertTrue(concat.contains("d"));
		assertFalse(concat.contains("other"));
	}

	@Test
	void testIterator() {
		var iterator = concat.iterator();
		assertTrue(iterator.hasNext());
		assertEquals("a", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("b", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("c", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("d", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("a", iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	void testIndexOf() {
		assertEquals(0, concat.indexOf("a"));
		assertEquals(1, concat.indexOf("b"));
		assertEquals(2, concat.indexOf("c"));
		assertEquals(3, concat.indexOf("d"));
		assertEquals(-1, concat.indexOf("other"));
	}

	@Test
	void testLastIndexOf() {
		assertEquals(1, concat.lastIndexOf("b"));
		assertEquals(2, concat.lastIndexOf("c"));
		assertEquals(3, concat.lastIndexOf("d"));
		assertEquals(4, concat.lastIndexOf("a"));
		assertEquals(-1, concat.lastIndexOf("other"));
	}

	@Test
	void testIsEmpty() {
		assertFalse(concat.isEmpty());
	}

	@Test
	void shouldReportEmptyOnNothing() {
		assertTrue(ListUtil.concat().isEmpty());
	}

	@Test
	void shouldReportEmptyOnEmptyDelegates() {
		assertTrue(ListUtil.concat(List.of(), List.of()).isEmpty());
	}
}
