package com.autonomouslogic.commons.cache;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Caches the result of a supplier, ensuring the underlying delegate is only called once.
 * @param <T> the type
 */
@RequiredArgsConstructor
public class CachedSupplier<T> implements Supplier<T> {
	private final Supplier<T> delegate;
	private boolean fetched;
	private T value;

	/**
	 * Returns the result of the delegate supplier, or the cached value if present.
	 * @return the supplier value
	 */
	@Override
	public T get() {
		if (!fetched) {
			value = delegate.get();
			fetched = true;
		}
		return value;
	}
}
