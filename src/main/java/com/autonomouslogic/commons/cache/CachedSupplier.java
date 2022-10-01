package com.autonomouslogic.commons.cache;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CachedSupplier<T> implements Supplier<T> {
	private final Supplier<T> delegate;
	private final AtomicReference<Optional<T>> value = new AtomicReference<>();

	@Override
	public T get() {
		var cached = value.get();
		if (cached == null) {
			var v = delegate.get();
			value.set(Optional.ofNullable(v));
			return v;
		}
		return cached.orElse(null);
	}
}
