package com.autonomouslogic.commons.rxjava3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public class Rx3UtilTest {
	RuntimeException textEx = new RuntimeException("test error");

	@Test
	public void shouldConvertCompletionStageToSingle() {
		var future = CompletableFuture.completedStage("test");
		var result = Rx3Util.toSingle(future).blockingGet();
		assertEquals("test", result);
	}

	@Test
	public void shouldCatchCompletionStageErrorsToSingle() {
		var future = CompletableFuture.failedStage(textEx);
		var ex = assertThrows(
				RuntimeException.class, () -> Rx3Util.toSingle(future).blockingGet());
		assertEquals("java.lang.RuntimeException: test error", ex.getMessage());
	}

	@Test
	public void shouldConvertCompletionStageToMaybe() {
		var future = CompletableFuture.completedStage("test");
		var result = Rx3Util.toMaybe(future).blockingGet();
		assertEquals("test", result);
	}

	@Test
	public void shouldConvertCompletionStageNullResultToMaybe() {
		var future = CompletableFuture.completedStage(null);
		var result = Rx3Util.toMaybe(future).defaultIfEmpty("empty").blockingGet();
		assertEquals("empty", result);
	}

	@Test
	public void shouldCatchCompletionStageErrorsToMaybe() {
		var future = CompletableFuture.failedStage(textEx);
		var ex = assertThrows(
				RuntimeException.class, () -> Rx3Util.toMaybe(future).blockingGet());
		assertEquals("java.lang.RuntimeException: test error", ex.getMessage());
	}

	@Test
	public void shouldConvertCompletionStageToCompletable() {
		var future = CompletableFuture.completedStage((Void) null);
		AtomicBoolean complete = new AtomicBoolean();
		Rx3Util.toCompletable(future).doOnComplete(() -> complete.set(true)).blockingAwait();
		assertTrue(complete.get());
	}

	@Test
	public void shouldCatchCompletionStageErrorsToCompletable() {
		var future = CompletableFuture.supplyAsync((Supplier<Void>) () -> {
			throw textEx;
		});
		var ex = assertThrows(
				RuntimeException.class, () -> Rx3Util.toCompletable(future).blockingAwait());
		assertEquals("java.lang.RuntimeException: test error", ex.getMessage());
	}
}
