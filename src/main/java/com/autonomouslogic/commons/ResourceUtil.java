package com.autonomouslogic.commons;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Loads class resources.
 */
public class ResourceUtil {
	/**
	 * Loads a resource as an {@link InputStream}, throwing an exception if not found rather than simply returning null.
	 * @param path the path to load
	 * @return the input stream for the resource
	 * @throws FileNotFoundException if not found
	 */
	public static InputStream loadResource(String path) throws FileNotFoundException {
		return loadResource(ResourceUtil.class, path);
	}

	/**
	 * Loads a resource as an {@link InputStream}, throwing an exception if not found rather than simply returning null.
	 * @param clazz the class to use for resource loading
	 * @param path the path to load
	 * @return the input stream for the resource
	 * @throws FileNotFoundException if not found
	 */
	public static InputStream loadResource(Class clazz, String path) throws FileNotFoundException {
		var in = clazz.getResourceAsStream(path);
		if (in == null) {
			throw new FileNotFoundException(path);
		}
		return in;
	}

	/**
	 * Loads a resource with the assumption that the path will be relative to one constructed from the class package and
	 * name.
	 * For instance, is the class <code>com.autonomouslogic.SomeClass</code> is provided, the path loaded will be
	 * relative to <code>/com/autonomouslogic/SomeClass/{path}</path></code>.
	 * This is useful for loading resources during unit tests where you store resources for each test in a directory
	 * named after the test class.
	 * @param clazz the class to use for resource loading and contextual path
	 * @param path the path to load
	 * @return the input stream for the resource
	 * @throws FileNotFoundException if not found
	 */
	public static InputStream loadContextual(Class clazz, String path) throws FileNotFoundException {
		var fullPath = "/" + clazz.getCanonicalName().replace('.', '/') + path;
		return loadResource(clazz, fullPath);
	}
}
