package com.autonomouslogic.commons.cache;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Wraps a {@link Supplier} and caches its result, ensuring the underlying delegate is only called once.
 *
 * <p>This is useful for lazy initialization of expensive resources. The first call to {@link #get()} invokes the
 * delegate, subsequent calls return the cached value without re-executing the supplier logic.
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * // Without caching: expensive operation runs every time
 * Supplier<DatabaseConnection> supplier = () -> {
 *     System.out.println("Creating connection...");
 *     return new DatabaseConnection();
 * };
 * supplier.get(); // prints "Creating connection..."
 * supplier.get(); // prints "Creating connection..." again
 *
 * // With caching: expensive operation runs only once
 * CachedSupplier<DatabaseConnection> cached = new CachedSupplier<>(supplier);
 * cached.get(); // prints "Creating connection..."
 * cached.get(); // returns cached value, no output
 * }</pre>
 *
 * <p><b>Features:</b>
 * <ul>
 * <li>Thread-unsafe: intended for single-threaded use or when synchronization is handled externally
 * <li>Caches null values: if the delegate returns null, that null is cached and returned on future calls
 * <li>Simple: no overhead beyond a boolean flag and a field reference
 * </ul>
 *
 * @param <T> the type of value supplied
 */
@RequiredArgsConstructor
public class CachedSupplier<T> implements Supplier<T> {
	private final Supplier<T> delegate;
	private boolean fetched;
	private T value;

	/**
	 * Returns the cached value, or calls the delegate supplier on the first invocation.
	 *
	 * <p>First call: invokes the delegate, caches the result (including null), and returns it.
	 * Subsequent calls: returns the cached value without invoking the delegate.
	 *
	 * @return the cached supplier value, or null if the delegate returns null
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
