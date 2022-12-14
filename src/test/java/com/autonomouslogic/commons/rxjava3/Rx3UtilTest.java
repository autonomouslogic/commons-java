package com.autonomouslogic.commons.rxjava3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import lombok.SneakyThrows;
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

	@Test
	void shouldWrapTransformerErrors() {
		var observable = Observable.just("result")
				.observeOn(Schedulers.computation())
				.compose(Rx3Util.wrapTransformerErrors(
						"wrapped error", upstream -> Observable.error(new RuntimeException("inner error"))));
		var ex = assertThrows(RuntimeException.class, observable::blockingFirst);
		ex.printStackTrace();
		assertEquals("wrapped error", ex.getMessage());
		assertEquals("inner error", ex.getCause().getMessage());
	}

	@Test
	void shouldNotAffectErrorsBeforeWrappingTransformer() {
		Observable<String> observable = Observable.just("result")
				.observeOn(Schedulers.computation())
				.compose((ObservableTransformer<String, String>)
						upstream -> Observable.error(new RuntimeException("before error")))
				.observeOn(Schedulers.computation())
				.compose(Rx3Util.wrapTransformerErrors("wrapped error", upstream -> upstream));
		var ex = assertThrows(RuntimeException.class, observable::blockingFirst);
		ex.printStackTrace();
		assertEquals("before error", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void shouldNotAffectErrorsAfterWrappingTransformer() {
		Observable<String> observable = Observable.just("result")
				.observeOn(Schedulers.computation())
				.compose(Rx3Util.wrapTransformerErrors("wrapped error", upstream -> upstream))
				.observeOn(Schedulers.computation())
				.compose(upstream -> Observable.error(new RuntimeException("after error")));
		var ex = assertThrows(RuntimeException.class, observable::blockingFirst);
		ex.printStackTrace();
		assertEquals("after error", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void shouldNotBlockResultsWhenWrappingTransformer() {
		Observable<String> observable = Observable.just("result")
				.observeOn(Schedulers.computation())
				.compose(Rx3Util.wrapTransformerErrors("wrapped error", upstream -> upstream));
		var result = observable.blockingFirst();
		assertEquals("result", result);
	}

	@Test
	@SneakyThrows
	void shouldZipAll() {
		var sub = new TestSubscriber<Object[]>();
		Rx3Util.zipAllFlowable(v -> v, Flowable.just(1, 2), Flowable.just(3, 4, 5), Flowable.just(6, 7, 8, 9))
				.subscribe(sub);
		var values = sub.await()
				.assertNoErrors()
				.assertComplete()
				.assertValueCount(4)
				.values();
		// First row.
		assertEquals(Optional.of(1), values.get(0)[0]);
		assertEquals(Optional.of(3), values.get(0)[1]);
		assertEquals(Optional.of(6), values.get(0)[2]);
		assertEquals(3, values.get(0).length);
		// Second row.
		assertEquals(Optional.of(2), values.get(1)[0]);
		assertEquals(Optional.of(4), values.get(1)[1]);
		assertEquals(Optional.of(7), values.get(1)[2]);
		assertEquals(3, values.get(0).length);
		// Third row.
		assertEquals(Optional.empty(), values.get(2)[0]);
		assertEquals(Optional.of(5), values.get(2)[1]);
		assertEquals(Optional.of(8), values.get(2)[2]);
		assertEquals(3, values.get(0).length);
		// Fourth row.
		assertEquals(Optional.empty(), values.get(3)[0]);
		assertEquals(Optional.empty(), values.get(3)[1]);
		assertEquals(Optional.of(9), values.get(3)[2]);
		assertEquals(3, values.get(0).length);
	}
}
