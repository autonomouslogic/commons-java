package com.autonomouslogic.com.autonomouslogic.commons.collection;

import com.autonomouslogic.commons.ListUtil;
import com.autonomouslogic.commons.collection.ConcatList;
import org.junit.jupiter.api.Test;

import javax.swing.plaf.ListUI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcatListTest {
	ConcatList<String> concat = ListUtil.concat(List.of("a", "b"), List.of("c", "d"));

	@Test
	public void testEquals() {
		assertEquals(List.of("a", "b", "c", "d"), concat);
	}

	@Test
	public void testSize() {
		assertEquals(4, concat.size());
	}

	@Test
	public void testGet() {
		assertEquals("a", concat.get(0));
		assertEquals("b", concat.get(1));
		assertEquals("c", concat.get(2));
		assertEquals("d", concat.get(3));
	}

	@Test
	public void testContains() {
		assertTrue(concat.contains("a"));
		assertTrue(concat.contains("b"));
		assertTrue(concat.contains("c"));
		assertTrue(concat.contains("d"));
		assertFalse(concat.contains("other"));
	}

	@Test
	public void testIterator() {
		var iterator = concat.iterator();
		assertTrue(iterator.hasNext());
		assertEquals("a", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("b", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("c", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("d", iterator.next());
		assertFalse(iterator.hasNext());
	}
}
