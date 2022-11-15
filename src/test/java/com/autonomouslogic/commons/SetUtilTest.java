package com.autonomouslogic.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SetUtilTest {
	Set<String> a = Set.of("x", "b");
	Set<String> b = Set.of("b", "c", "q");

	@Test
	void shouldMergeMultipleSets() {
		var merged = SetUtil.mergeCopy(a, b);
		assertEquals(Set.of("x", "b", "c", "q"), merged);
	}

	@Test
	void shouldMergeWhilePreservingIterationOrder() {
		var merged = SetUtil.mergeCopy(Set.of("a"), Set.of("b"), Set.of("c")).stream()
				.collect(Collectors.toList());
		assertEquals(List.of("a", "b", "c"), merged);
	}

	@Test
	void shouldAddSetsToTarget() {
		var existing = new HashSet<String>();
		existing.add("_");
		SetUtil.addAll(existing, a, b);
		assertEquals(Set.of("_", "x", "b", "c", "q"), existing);
	}
}
