package com.autonomouslogic.commons.cache;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Caches the result of a supplier, ensuring the underlying delegate is only called once.
 * @param <T> the type
 */
@RequiredArgsConstructor
public class CachedSupplier<T> implements Supplier<T> {
	private final Supplier<T> delegate;
	private final AtomicReference<Optional<T>> value = new AtomicReference<>();

	/**
	 * Returns the result of the delegate supplier, or the cached value if present.
	 * @return the supplier value
	 */
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
