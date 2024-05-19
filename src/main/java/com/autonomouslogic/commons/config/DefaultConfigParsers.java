package com.autonomouslogic.commons.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

public class DefaultConfigParsers {
	public static <T> Optional<ConfigParser<T>> getParser(Class<T> type) {
		if (type == String.class) {
			return Optional.of(type::cast);
		} else if (type == Integer.class) {
			return Optional.of(value -> type.cast(Integer.parseInt(value)));
		} else if (type == Long.class) {
			return Optional.of(value -> type.cast(Long.parseLong(value)));
		} else if (type == Float.class) {
			return Optional.of(value -> type.cast(Float.parseFloat(value)));
		} else if (type == Double.class) {
			return Optional.of(value -> type.cast(Double.parseDouble(value)));
		} else if (type == BigInteger.class) {
			return Optional.of(value -> type.cast(new BigInteger(value)));
		} else if (type == BigDecimal.class) {
			return Optional.of(value -> type.cast(new BigDecimal(value)));
		} else if (type == Boolean.class) {
			return Optional.of(value -> type.cast(Boolean.parseBoolean(value)));
		} else if (type == LocalDate.class) {
			return Optional.of(value -> type.cast(LocalDate.parse(value)));
		} else if (type == Duration.class) {
			return Optional.of(value -> type.cast(Duration.parse(value)));
		} else if (type == Period.class) {
			return Optional.of(value -> type.cast(Period.parse(value)));
		} else if (type == URI.class) {
			return Optional.of(value -> type.cast(new URI(value)));
		}
		return Optional.empty();
	}
}
