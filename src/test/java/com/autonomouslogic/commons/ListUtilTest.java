package com.autonomouslogic.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListUtilTest {
	@Test
	void shouldCopyLists() {
		var concat = ListUtil.concatCopy(List.of("a", "b"), List.of("b", "c"), List.of());
		assertEquals(List.of("a", "b", "b", "c"), concat);
	}

	@Test
	void shouldNotFailOnNoArgs() {
		var concat = ListUtil.concatCopy();
		assertEquals(List.of(), concat);
	}
}
