package com.autonomouslogic.commons;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Utility for loading resources (files) from the classpath, with clear error handling.
 *
 * <p>Standard Java resource loading via {@link Class#getResourceAsStream(String)} returns null if a resource
 * is not found, which can lead to hard-to-debug NullPointerExceptions. ResourceUtil instead throws
 * {@link FileNotFoundException} with the missing path, making issues obvious.
 *
 * <p><b>Basic usage:</b>
 * <pre>{@code
 * // Load from classpath root
 * try (var in = ResourceUtil.loadResource("/config.json")) {
 *     var config = new String(in.readAllBytes());
 * }
 * }</pre>
 *
 * <p><b>Contextual loading (great for tests):</b>
 * <pre>{@code
 * // For class com.example.MyTest, loads from /com/example/MyTest/data.json
 * try (var in = ResourceUtil.loadContextual(MyTest.class, "/data.json")) {
 *     var data = new String(in.readAllBytes());
 * }
 * }</pre>
 *
 * <p>Contextual loading is especially useful for organizing test fixtures: store each test's resources
 * in a directory named after the test class within your test resources directory.
 */
public class ResourceUtil {
	private static final char RESOURCE_SEPARATOR = '/';

	private ResourceUtil() {}

	/**
	 * Loads a resource from the classpath root as an InputStream.
	 *
	 * <p>Unlike {@link Class#getResourceAsStream(String)}, this method throws {@link FileNotFoundException}
	 * if the resource does not exist, rather than returning null.
	 *
	 * <p>Example: {@code loadResource("/config.json")} loads {@code src/main/resources/config.json}
	 *
	 * @param path the absolute path to the resource (must start with {@code /})
	 * @return the InputStream for the resource
	 * @throws FileNotFoundException if the resource does not exist
	 */
	public static InputStream loadResource(String path) throws FileNotFoundException {
		return loadResource(ResourceUtil.class, path);
	}

	/**
	 * Loads a resource as an InputStream, resolved relative to a given class.
	 *
	 * <p>Unlike {@link Class#getResourceAsStream(String)}, this method throws {@link FileNotFoundException}
	 * if the resource does not exist, rather than returning null.
	 *
	 * <p>Example: For class {@code com.example.Foo}, {@code loadResource(Foo.class, "/data.json")}
	 * loads from {@code /com/example/data.json} on the classpath.
	 *
	 * @param clazz the class to use for resource resolution
	 * @param path the path to the resource, relative to the class's package
	 * @return the InputStream for the resource
	 * @throws FileNotFoundException if the resource does not exist
	 */
	public static InputStream loadResource(Class<?> clazz, String path) throws FileNotFoundException {
		var in = clazz.getResourceAsStream(path);
		if (in == null) {
			throw new FileNotFoundException(path);
		}
		return in;
	}

	/**
	 * Loads a resource relative to the given class's package directory.
	 *
	 * <p>The path is resolved relative to a directory named after the class's fully-qualified name.
	 * This is particularly useful for organizing test fixtures by test class.
	 *
	 * <p>Example: {@code loadContextual(com.example.MyTest.class, "/data.json")} loads from
	 * {@code /com/example/MyTest/data.json} on the classpath. This corresponds to a file at
	 * {@code src/test/resources/com/example/MyTest/data.json} in your project.
	 *
	 * @param clazz the class to use for resource resolution and contextual path construction
	 * @param path the path relative to the class's contextual directory (typically starts with {@code /})
	 * @return the InputStream for the resource
	 * @throws FileNotFoundException if the resource does not exist
	 */
	public static InputStream loadContextual(Class<?> clazz, String path) throws FileNotFoundException {
		var fullPath = RESOURCE_SEPARATOR + clazz.getCanonicalName().replace('.', RESOURCE_SEPARATOR) + path;
		return loadResource(clazz, fullPath);
	}
}
