package com.autonomouslogic.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class ResourceUtilTest {
	@Test
	@SneakyThrows
	void shouldLoadResourcesFromRoot() {
		var in = ResourceUtil.loadResource("/test.txt");
		var content = IOUtils.toString(in, Charset.defaultCharset());
		assertEquals("Test content\n", content);
	}

	@Test
	@SneakyThrows
	void shouldLoadContextualResources() {
		var in = ResourceUtil.loadContextual(ResourceUtilTest.class, "/test.txt");
		var content = IOUtils.toString(in, Charset.defaultCharset());
		assertEquals("Contextual content\n", content);
	}

	@Test
	@SneakyThrows
	void shouldThrowWhenFileNotFound() {
		assertThrows(FileNotFoundException.class, () -> ResourceUtil.loadResource("/unknown"));
	}
}
