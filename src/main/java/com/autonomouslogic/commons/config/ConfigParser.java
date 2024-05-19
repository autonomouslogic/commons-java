package com.autonomouslogic.commons.config;

public interface ConfigParser<T> {
	T parse(String value) throws Exception;
}
