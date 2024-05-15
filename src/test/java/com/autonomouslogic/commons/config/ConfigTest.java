package com.autonomouslogic.commons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

public class ConfigTest {
	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_STRING", value = "test-value")
	void shouldGetStringValues() {
		var config = Config.<String>builder()
				.name("TEST_ENV_VAR_STRING")
				.type(String.class)
				.build();
		assertEquals(Optional.of("test-value"), config.get());
		assertEquals("test-value", config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_LONG", value = "12345")
	void shouldGetLongValues() {
		var config = Config.<Long>builder()
				.name("TEST_ENV_VAR_LONG")
				.type(Long.class)
				.build();
		assertEquals(Optional.of(12345L), config.get());
		assertEquals(12345L, config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_FLOAT", value = "12345.6789")
	void shouldGetFloatValues() {
		var config = Config.<Float>builder()
				.name("TEST_ENV_VAR_FLOAT")
				.type(Float.class)
				.build();
		assertEquals(Optional.of(12345.6789f), config.get());
		assertEquals(12345.6789f, config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_DOUBLE", value = "12345.6789")
	void shouldGetDoubleValues() {
		var config = Config.<Double>builder()
				.name("TEST_ENV_VAR_DOUBLE")
				.type(Double.class)
				.build();
		assertEquals(Optional.of(12345.6789d), config.get());
		assertEquals(12345.6789d, config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_INTEGER", value = "12345")
	void shouldGetIntegerValues() {
		var config = Config.<Integer>builder()
				.name("TEST_ENV_VAR_INTEGER")
				.type(Integer.class)
				.build();
		assertEquals(Optional.of(12345), config.get());
		assertEquals(12345, config.getRequired());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_BOOL_TRUE", value = "true")
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_BOOL_FALSE", value = "false")
	void shouldGetBooleanValues(boolean val) {
		var config = Config.<Boolean>builder()
				.name("TEST_ENV_VAR_BOOL_" + (val ? "TRUE" : "FALSE"))
				.type(Boolean.class)
				.build();
		assertEquals(Optional.of(val), config.get());
		assertEquals(val, config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_LOCAL_DATE", value = "2021-06-27")
	void shouldGetLocalDateValues() {
		var config = Config.<LocalDate>builder()
				.name("TEST_ENV_VAR_LOCAL_DATE")
				.type(LocalDate.class)
				.build();
		assertEquals(Optional.of(LocalDate.parse("2021-06-27")), config.get());
		assertEquals(LocalDate.parse("2021-06-27"), config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_DURATION", value = "PT3H7M13.334S")
	void shouldGetDurationValues() {
		var config = Config.<Duration>builder()
				.name("TEST_ENV_VAR_DURATION")
				.type(Duration.class)
				.build();
		assertEquals(Optional.of(Duration.parse("PT3H7M13.334S")), config.get());
		assertEquals(Duration.parse("PT3H7M13.334S"), config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_PERIOD", value = "P7M5D")
	void shouldGetPeriodValues() {
		var config = Config.<Period>builder()
				.name("TEST_ENV_VAR_PERIOD")
				.type(Period.class)
				.build();
		assertEquals(Optional.of(Period.parse("P7M5D")), config.get());
		assertEquals(Period.parse("P7M5D"), config.getRequired());
	}

	@Test
	@SetEnvironmentVariable(key = "TEST_ENV_VAR_URI", value = "http://example.com/page")
	void shouldGetUriValues() {
		var config =
				Config.<URI>builder().name("TEST_ENV_VAR_URI").type(URI.class).build();
		assertEquals(Optional.of(URI.create("http://example.com/page")), config.get());
		assertEquals(URI.create("http://example.com/page"), config.getRequired());
	}

	@Test
	void shouldGetUnknownValues() {
		var config =
				Config.<String>builder().name("UNKNOWN_VAR").type(String.class).build();
		assertEquals(Optional.empty(), config.get());
		var e = assertThrows(IllegalArgumentException.class, config::getRequired);
		assertEquals("No value for UNKNOWN_VAR", e.getMessage());
	}

	@Test
	void shouldGetDefaultValues() {
		var config = Config.<String>builder()
				.name("UNKNOWN_VAR")
				.type(String.class)
				.defaultValue("default-value")
				.build();
		assertEquals(Optional.of("default-value"), config.get());
		assertEquals("default-value", config.getRequired());
	}

	@Test
	void shouldGetDefaultMethods() {
		var config = Config.<String>builder()
				.name("UNKNOWN_VAR")
				.type(String.class)
				.defaultMethod(() -> Optional.of("default-value"))
				.build();
		assertEquals(Optional.of("default-value"), config.get());
		assertEquals("default-value", config.getRequired());
	}

	@Test
	void shouldErrorIfRequiredValuesArentSet() {
		var config =
				Config.<String>builder().name("UNKNOWN_VAR").type(String.class).build();
		assertEquals(Optional.empty(), config.get());
		var e = assertThrows(IllegalArgumentException.class, config::getRequired);
		assertEquals("No value for UNKNOWN_VAR", e.getMessage());
	}

	@Test
	void shouldErrorIfBothDefaultValueAndMethodAreSet() {
		var config = Config.<String>builder()
				.name("UNKNOWN_VAR")
				.type(String.class)
				.defaultValue("default-value")
				.defaultMethod(() -> Optional.of("default-value"))
				.build();
		var e = assertThrows(IllegalArgumentException.class, config::get);
		assertEquals("Both default value and default method specified for UNKNOWN_VAR", e.getMessage());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "TEST_VAR_FILE", value = "/tmp/test-var-file-78676f75")
	void shouldLoadVariablesFromFiles() {
		var file = new File("/tmp/test-var-file-78676f75");
		assertFalse(file.exists());
		file.deleteOnExit();
		FileUtils.write(file, "test-value\n", StandardCharsets.UTF_8);

		var config =
				Config.<String>builder().name("TEST_VAR").type(String.class).build();
		assertEquals(Optional.of("test-value"), config.get());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "TEST_VAR_FILE", value = "/tmp/test-var-file-78676f75")
	@SetEnvironmentVariable(key = "TEST_VAR", value = "test-values")
	void shouldErrorIfBothFileAndNonFileArePresent() {
		var config =
				Config.<String>builder().name("TEST_VAR").type(String.class).build();
		var e = assertThrows(IllegalArgumentException.class, config::get);
		assertEquals("Both TEST_VAR and TEST_VAR_FILE cannot be set at the same time", e.getMessage());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "TEST_VAR_FILE", value = "/tmp/test-var-file-78676f75")
	void shouldErrorIfFileNotFound() {
		var config =
				Config.<String>builder().name("TEST_VAR").type(String.class).build();
		var e = assertThrows(FileNotFoundException.class, config::get);
		assertEquals("/tmp/test-var-file-78676f75", e.getMessage());
	}

	@Test
	@SneakyThrows
	@SetEnvironmentVariable(key = "TEST_VAR", value = "not an integer")
	void shouldErrorOnFailedTypeCasting() {
		var config =
				Config.<Integer>builder().name("TEST_VAR").type(Integer.class).build();
		var e = assertThrows(IllegalArgumentException.class, config::get);
		assertEquals("Unable to parse value in TEST_VAR as type class java.lang.Integer", e.getMessage());
	}
}
