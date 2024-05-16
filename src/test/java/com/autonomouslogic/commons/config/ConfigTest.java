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
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "TEST_ENV_VAR_STRING", value = "test-value")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_INTEGER", value = "12345")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_FLOAT", value = "12345.6789")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_BOOL_TRUE", value = "true")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_BOOL_FALSE", value = "false")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_LOCAL_DATE", value = "2021-06-27")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_DURATION", value = "PT3H7M13.334S")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_PERIOD", value = "P7M5D")
@SetEnvironmentVariable(key = "TEST_ENV_VAR_URI", value = "http://example.com/page")
public class ConfigTest {
	@ParameterizedTest
	@MethodSource("parseProvider")
	void shouldReadValues(Config<?> config, Object expected) {
		assertEquals(Optional.of(expected), config.get());
		assertEquals(expected, config.getRequired());
	}

	public static Stream<Arguments> parseProvider() {
		return Stream.of(
				Arguments.of(stringConfig, "test-value"),
				Arguments.of(longConfig, 12345L),
				Arguments.of(floatConfig, 12345.6789f),
				Arguments.of(doubleConfig, 12345.6789d),
				Arguments.of(booleanConfigTrue, true),
				Arguments.of(booleanConfigFalse, false),
				Arguments.of(localDateConfig, LocalDate.parse("2021-06-27")),
				Arguments.of(
						durationConfig,
						Duration.ofHours(3).plusMinutes(7).plusSeconds(13).plusMillis(334)),
				Arguments.of(periodConfig, Period.ofDays(7).plusDays(5)),
				Arguments.of(uriConfig, URI.create("http://example.com/page")));
	}

	static Config<String> stringConfig = Config.<String>builder()
			.name("TEST_ENV_VAR_STRING")
			.type(String.class)
			.build();

	static Config<Long> longConfig =
			Config.<Long>builder().name("TEST_ENV_VAR_INTEGER").type(Long.class).build();

	static Config<Integer> integerConfig = Config.<Integer>builder()
			.name("TEST_ENV_VAR_INTEGER")
			.type(Integer.class)
			.build();

	static Config<Float> floatConfig =
			Config.<Float>builder().name("TEST_ENV_VAR_FLOAT").type(Float.class).build();

	static Config<Double> doubleConfig = Config.<Double>builder()
			.name("TEST_ENV_VAR_FLOAT")
			.type(Double.class)
			.build();

	static Config<Boolean> booleanConfigTrue = Config.<Boolean>builder()
			.name("TEST_ENV_VAR_BOOL_TRUE")
			.type(Boolean.class)
			.build();

	static Config<Boolean> booleanConfigFalse = Config.<Boolean>builder()
			.name("TEST_ENV_VAR_BOOL_FALSE")
			.type(Boolean.class)
			.build();

	static Config<LocalDate> localDateConfig = Config.<LocalDate>builder()
			.name("TEST_ENV_VAR_LOCAL_DATE")
			.type(LocalDate.class)
			.build();

	static Config<Duration> durationConfig = Config.<Duration>builder()
			.name("TEST_ENV_VAR_DURATION")
			.type(Duration.class)
			.build();

	static Config<Period> periodConfig = Config.<Period>builder()
			.name("TEST_ENV_VAR_PERIOD")
			.type(Period.class)
			.build();

	static Config<URI> uriConfig =
			Config.<URI>builder().name("TEST_ENV_VAR_URI").type(URI.class).build();

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
