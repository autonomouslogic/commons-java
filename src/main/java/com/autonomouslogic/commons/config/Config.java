package com.autonomouslogic.commons.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * A builder-based tool for defining and reading configuration from environment variables with automatic type parsing.
 *
 * <p>Configurations are typically defined as static constants in a dedicated class, then accessed throughout the
 * application. Config handles parsing environment variables into typed values (Integer, Boolean, Duration, etc.)
 * and provides fallback defaults.
 *
 * <p><b>Basic usage:</b>
 * <pre>{@code
 * public class AppConfig {
 *     public static final Config<String> ENVIRONMENT = Config.<String>builder()
 *         .name("ENVIRONMENT")
 *         .type(String.class)
 *         .defaultValue("dev")
 *         .build();
 *
 *     public static final Config<Integer> PORT = Config.<Integer>builder()
 *         .name("PORT")
 *         .type(Integer.class)
 *         .defaultValue(8080)
 *         .build();
 * }
 *
 * // Read values
 * Optional<String> env = AppConfig.ENVIRONMENT.get();   // with default
 * Integer port = AppConfig.PORT.getRequired();           // throws if not set
 * }</pre>
 *
 * <p><b>Supported types:</b> String, Integer, Long, Float, Double, Boolean, BigInteger, BigDecimal,
 * LocalDate, Duration, Period, URI. Custom types can be supported by providing a custom {@link ConfigParser}.
 *
 * <p><b>File-based secrets:</b> Environment variables can reference files via a {@code _FILE} suffix.
 * This is useful for storing sensitive values without exposing them in the environment:
 * <pre>{@code
 * // Instead of: export DATABASE_PASSWORD=secret123
 * // Use: export DATABASE_PASSWORD_FILE=/run/secrets/db_password
 *
 * Config<String> dbPassword = Config.<String>builder()
 *     .name("DATABASE_PASSWORD")
 *     .type(String.class)
 *     .build();
 *
 * // Reading dbPassword.get() will read from /run/secrets/db_password
 * }</pre>
 *
 * <p><b>Default values:</b> Use {@code defaultValue()} for static defaults or {@code defaultMethod()}
 * for computed defaults. Cannot specify both.
 *
 * <p><b>Error handling:</b>
 * <ul>
 * <li>{@link #get()} returns {@link Optional#empty()} if the variable is not set and no default is provided
 * <li>{@link #getRequired()} throws {@link IllegalArgumentException} if the variable is missing
 * <li>Parsing errors throw {@link IllegalArgumentException} with details about the type and variable name
 * </ul>
 */
@Builder
public class Config<T> {
	@NonNull
	@Getter
	String name;

	@NonNull
	Class<T> type;

	ConfigParser<T> parser;

	T defaultValue;

	Supplier<Optional<T>> defaultMethod;

	/**
	 * Reads the configuration value from the environment or returns a default.
	 *
	 * <p>Resolution order:
	 * <ol>
	 * <li>Environment variable (parsed to type)
	 * <li>File at path specified by {@code <NAME>_FILE} environment variable
	 * <li>Default value (if configured)
	 * <li>Default method result (if configured)
	 * <li>{@link Optional#empty()}
	 * </ol>
	 *
	 * @return an Optional containing the parsed value, or empty if not found and no default configured
	 * @throws IllegalArgumentException if parsing fails or both {@code <NAME>} and {@code <NAME>_FILE} are set
	 */
	public Optional<T> get() {
		return getSetValue().or(this::getDefaultValue);
	}

	/**
	 * Reads the configuration value from the environment or returns a default.
	 *
	 * <p>Same as {@link #get()}, but throws if the value is not found.
	 *
	 * @return the parsed value
	 * @throws IllegalArgumentException if the value is not set and no default is configured, or if parsing fails,
	 *         or if both {@code <NAME>} and {@code <NAME>_FILE} are set
	 */
	public T getRequired() {
		return get().orElseThrow(() -> new IllegalArgumentException(String.format("No value for %s", name)));
	}

	private Optional<T> getSetValue() {
		var fileName = name + "_FILE";
		var env = System.getenv();
		if (env.containsKey(name) && env.containsKey(fileName)) {
			throw new IllegalArgumentException(
					String.format("Both %s and %s cannot be set at the same time", name, fileName));
		}
		if (!env.containsKey(name) && !env.containsKey(fileName)) {
			return Optional.empty();
		}
		return getFromEnv(name).or(() -> getFromFile(fileName));
	}

	private Optional<T> getFromEnv(String env) {
		var value = System.getenv(env);
		if (value == null || value.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(parse(env, value));
	}

	@SneakyThrows
	private Optional<T> getFromFile(String env) {
		var filename = System.getenv(env);
		if (filename == null || filename.isEmpty()) {
			return Optional.empty();
		}
		var file = new File(filename);
		if (!file.exists()) {
			throw new FileNotFoundException(filename);
		}
		var value = readFile(file);
		if (value.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(parse(env, value));
	}

	@SneakyThrows
	private String readFile(File file) {
		var builder = new StringBuilder();
		var buffer = new char[1024];
		try (var reader = new FileReader(file, StandardCharsets.UTF_8)) {
			int n;
			while (-1 != (n = reader.read(buffer))) {
				builder.append(buffer, 0, n);
			}
		}
		return builder.toString().trim();
	}

	private T parse(String env, String value) {
		try {
			return getParser(env).parse(value);
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Unable to parse value in %s as type %s", env, type));
		}
	}

	private ConfigParser<T> getParser(String env) {
		return Optional.ofNullable(parser)
				.or(() -> DefaultConfigParsers.getParser(type))
				.orElseThrow(
						() -> new IllegalArgumentException(String.format("Unsupported type %s for %s", type, env)));
	}

	private Optional<T> getDefaultValue() {
		if (defaultValue != null && defaultMethod != null) {
			throw new IllegalArgumentException(
					String.format("Both default value and default method specified for %s", name));
		}
		if (defaultValue != null) {
			return Optional.of(defaultValue);
		}
		if (defaultMethod != null) {
			return defaultMethod.get();
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "Config{" + "name='" + name + '\'' + '}';
	}
}
