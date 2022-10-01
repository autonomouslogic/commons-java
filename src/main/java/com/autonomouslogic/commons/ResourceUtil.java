package com.autonomouslogic.commons;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class ResourceUtil {
	public static InputStream loadResource(String path) throws FileNotFoundException {
		return loadResource(ResourceUtil.class, path);
	}

	public static InputStream loadResource(Class clazz, String path) throws FileNotFoundException {
		var in = clazz.getResourceAsStream(path);
		if (in == null) {
			throw new FileNotFoundException(path);
		}
		return in;
	}

	public static InputStream loadContextual(Class clazz, String path) throws FileNotFoundException {
		var fullPath = "/" + clazz.getCanonicalName().replace('.', '/') + path;
		return loadResource(clazz, fullPath);
	}
}
