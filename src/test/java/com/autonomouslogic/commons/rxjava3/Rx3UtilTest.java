package com.autonomouslogic.commons.rxjava3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class Rx3UtilTest {
	RuntimeException textEx = new RuntimeException("test error");

	@Test
	void shouldConvertCompletionStageToSingle() {
		var future = CompletableFuture.completedStage("test");
		var result = Rx3Util.toSingle(future).blockingGet();
		assertEquals("test", result);
	}

	@Test
	void shouldCatchCompletionStageErrorsToSingle() {
		var future = CompletableFuture.failedStage(textEx);
		var single = Rx3Util.toSingle(future);
		var ex = assertThrows(RuntimeException.class, single::blockingGet);
		assertEquals("java.lang.RuntimeException: test error", ex.getMessage());
	}

	@Test
	void shouldConvertCompletionStageToMaybe() {
		var future = CompletableFuture.completedStage("test");
		var result = Rx3Util.toMaybe(future).blockingGet();
		assertEquals("test", result);
	}

	@Test
	void shouldConvertCompletionStageNullResultToMaybe() {
		var future = CompletableFuture.completedStage(null);
		var result = Rx3Util.toMaybe(future).defaultIfEmpty("empty").blockingGet();
		assertEquals("empty", result);
	}

	@Test
	void shouldCatchCompletionStageErrorsToMaybe() {
		var future = CompletableFuture.failedStage(textEx);
		var maybe = Rx3Util.toMaybe(future);
		var ex = assertThrows(RuntimeException.class, maybe::blockingGet);
		assertEquals("java.lang.RuntimeException: test error", ex.getMessage());
	}

	@Test
	void shouldConvertCompletionStageToCompletable() {
		var future = CompletableFuture.completedStage((Void) null);
		AtomicBoolean complete = new AtomicBoolean();
		Rx3Util.toCompletable(future).doOnComplete(() -> complete.set(true)).blockingAwait();
		assertTrue(complete.get());
	}

	@Test
	void shouldCatchCompletionStageErrorsToCompletable() {
		var future = CompletableFuture.supplyAsync((Supplier<Void>) () -> {
			throw textEx;
		});
		var completable = Rx3Util.toCompletable(future);
		var ex = assertThrows(RuntimeException.class, completable::blockingAwait);
		assertEquals("java.lang.RuntimeException: test error", ex.getMessage());
	}
}
