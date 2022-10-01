package com.autonomouslogic.commons.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CachedSupplierTest {
	Supplier<String> supplier;
	CachedSupplier<String> cachedSupplier;

	@BeforeEach
	public void setup() {
		supplier = spy(new Supplier<String>() {
			@Override
			public String get() {
				return "result";
			}
		});
		cachedSupplier = new CachedSupplier<>(supplier);
	}

	@Test
	public void shouldDelegate() {
		assertEquals("result", cachedSupplier.get());
	}

	@Test
	public void shouldCacheValues() {
		assertSame(cachedSupplier.get(), cachedSupplier.get());
	}

	@Test
	public void shouldOnlyCallSupplierOnce() {
		assertEquals("result", cachedSupplier.get());
		verify(supplier).get();
		assertEquals("result", cachedSupplier.get());
		verifyNoMoreInteractions(supplier);
	}

	@Test
	public void shouldCacheNullValues() {
		supplier = spy(new Supplier<String>() {
			@Override
			public String get() {
				return null;
			}
		});
		cachedSupplier = new CachedSupplier<>(supplier);

		assertNull(cachedSupplier.get());
		verify(supplier).get();
		assertNull(cachedSupplier.get());
		verifyNoMoreInteractions(supplier);
	}
}
