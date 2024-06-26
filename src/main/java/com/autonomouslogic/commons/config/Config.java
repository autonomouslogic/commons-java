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
 * A simple tool for defining and reading configs from environment variables.
 * Configs are defined like this:
 * <pre>
 * {@code
 * public class Configs {
 *     public static final Config<String> VARIABLE_NAME = Config.<String>builder()
 *         .name("VARIABLE_NAME")
 *         .type(String.class)
 *         .defaultValue("dev") // optional
 *         .defaultMethod(() -> "dev") // optional
 *         .build();
 * }
 * }
 * </pre>
 * And can then be read by {@link #get()} which returns an <code>Optional</code>,
 * or by {@link #getRequired()} which throws an exception if not found.
 *
 * A <code>_FILE</code> suffix is also supported for reading config values from files.
 * This is useful for storing secrets to avoid them being present directly in the environment.
 * In the example above, setting the environment variable <code>VARIABLE_NAME_FILE=/tmp/value.secret</code> would cause
 * the contents of <code>/tmp/value.secret</code> to be used instead.
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

	public Optional<T> get() {
		return getSetValue().or(this::getDefaultValue);
	}

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
