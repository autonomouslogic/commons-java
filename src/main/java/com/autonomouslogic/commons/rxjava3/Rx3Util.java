package com.autonomouslogic.commons.rxjava3;

import com.autonomouslogic.commons.rxjava3.internal.CheckOrder;
import com.autonomouslogic.commons.rxjava3.internal.ErrorWrapFlowableTransformer;
import com.autonomouslogic.commons.rxjava3.internal.ErrorWrapObservableTransformer;
import com.autonomouslogic.commons.rxjava3.internal.OrderedMerger;
import com.autonomouslogic.commons.rxjava3.internal.ZipAll;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;

/**
 * Various helper methods for working with RxJava 3.
 */
public class Rx3Util {
	private Rx3Util() {}

	/**
	 * Converts a {@link CompletionStage} to a {@link Single}.
	 *
	 * <p>Unlike {@link Single#fromFuture(Future)}, this works non-blocking with CompletionStage callbacks.
	 * Null return values will result in an error, as RxJava Single doesn't allow null. Use {@link #toMaybe(CompletionStage)}
	 * for null-safe conversions.
	 *
	 * <p><b>Example:</b>
	 * <pre>{@code
	 * CompletableFuture<String> future = asyncOperation();
	 * Single<String> single = Rx3Util.toSingle(future);
	 *
	 * single.subscribe(
	 *     value -> System.out.println("Got: " + value),
	 *     error -> System.err.println("Error: " + error)
	 * );
	 * }</pre>
	 *
	 * @param future the completion stage to convert
	 * @return a Single that emits the completion stage's value
	 * @param <T> the type of value the completion stage will produce
	 * @throws NullPointerException if the completion stage completes with a null value
	 */
	public static <T> Single<T> toSingle(CompletionStage<T> future) {
		return Single.create(emitter -> {
			future.whenComplete((result, throwable) -> {
				if (!emitter.isDisposed()) {
					if (throwable != null) {
						emitter.onError(new ExecutionException(throwable));
					} else if (result != null) {
						emitter.onSuccess(result);
					} else {
						emitter.onError(new NullPointerException("CompletionStage completed with a null result"));
					}
				}
			});
			emitter.setCancellable(() -> {
				if (future instanceof CompletableFuture<?>) {
					((CompletableFuture<?>) future).cancel(false);
				}
			});
		});
	}

	/**
	 * Converts a {@link CompletionStage} to a {@link Maybe}.
	 *
	 * <p>Unlike {@link Maybe#fromFuture(Future)}, this works non-blocking with CompletionStage callbacks.
	 * Null return values result in an empty Maybe (completion without a value).
	 *
	 * <p><b>Example:</b>
	 * <pre>{@code
	 * CompletableFuture<String> future = asyncOperation();
	 * Maybe<String> maybe = Rx3Util.toMaybe(future);
	 *
	 * maybe.subscribe(
	 *     value -> System.out.println("Got: " + value),
	 *     error -> System.err.println("Error: " + error),
	 *     () -> System.out.println("Completed without a value")
	 * );
	 * }</pre>
	 *
	 * @param future the completion stage to convert
	 * @return a Maybe that emits the completion stage's value, or empty if it completes with null
	 * @param <T> the type of value the completion stage will produce
	 */
	public static <T> Maybe<T> toMaybe(CompletionStage<T> future) {
		return Maybe.create(emitter -> {
			future.whenComplete((result, throwable) -> {
				if (!emitter.isDisposed()) {
					if (throwable != null) {
						emitter.onError(new ExecutionException(throwable));
					} else if (result != null) {
						emitter.onSuccess(result);
					} else {
						emitter.onComplete();
					}
				}
			});
			emitter.setCancellable(() -> {
				if (future instanceof CompletableFuture<?>) {
					((CompletableFuture<?>) future).cancel(false);
				}
			});
		});
	}

	/**
	 * Converts a {@link CompletionStage} to a {@link Completable}.
	 *
	 * <p>Unlike {@link Completable#fromFuture(Future)}, this works non-blocking with CompletionStage callbacks.
	 *
	 * <p><b>Example:</b>
	 * <pre>{@code
	 * CompletableFuture<Void> future = asyncOperation();
	 * Completable completable = Rx3Util.toCompletable(future);
	 *
	 * completable.subscribe(
	 *     () -> System.out.println("Operation completed"),
	 *     error -> System.err.println("Error: " + error)
	 * );
	 * }</pre>
	 *
	 * @param future the completion stage to convert
	 * @return a Completable that completes or errors based on the completion stage
	 */
	public static Completable toCompletable(CompletionStage<Void> future) {
		return Completable.create(emitter -> {
			future.whenComplete((result, throwable) -> {
				if (!emitter.isDisposed()) {
					if (throwable != null) {
						// Emit only the cause message
						if (throwable.getCause() != null) {
							emitter.onError(new ExecutionException(throwable));
						} else {
							emitter.onError(throwable);
						}
					} else {
						emitter.onComplete();
					}
				}
			});

			emitter.setCancellable(() -> {
				if (future instanceof CompletableFuture<?>) {
					((CompletableFuture<?>) future).cancel(false);
				}
			});
		});
	}

	/**
	 * Wraps an {@link ObservableTransformer} such that any errors thrown from anything done by the
	 * <code>transformer</code> will be wrapped in a {@link RuntimeException} with the provided message.
	 * Any errors occurring before or after the <code>transformer</code> is applied are not subject to exception
	 * wrapping.
	 * This is useful when debugging multi-threaded asynchronous code where the relevant stack trace can get lost.
	 *
	 * @param message message for the exception
	 * @param transformer transformer used for composition
	 * @return a new transformer
	 * @param <U> see ObservableTransformer
	 * @param <D> see ObservableTransformer
	 */
	public static <U, D> ObservableTransformer<U, D> wrapTransformerErrors(
			String message, ObservableTransformer<U, D> transformer) {
		return new ErrorWrapObservableTransformer<>(message, transformer);
	}

	/**
	 * Wraps an {@link FlowableTransformer} such that any errors thrown from anything done by the
	 * <code>transformer</code> will be wrapped in a {@link RuntimeException} with the provided message.
	 * Any errors occurring before or after the <code>transformer</code> is applied are not subject to exception
	 * wrapping.
	 * This is useful when debugging multi-threaded asynchronous code where the relevant stack trace can get lost.
	 *
	 * @param message message for the exception
	 * @param transformer transformer used for composition
	 * @return a new transformer
	 * @param <U> see FlowableTransformer
	 * @param <D> see FlowableTransformer
	 */
	public static <U, D> FlowableTransformer<U, D> wrapTransformerErrors(
			String message, FlowableTransformer<U, D> transformer) {
		return new ErrorWrapFlowableTransformer<>(message, transformer);
	}

	/**
	 * Merges sorted sources into a single stream while maintaining order.
	 *
	 * <p>Emits items from whichever source has the lowest item according to the comparator, continuing until all
	 * sources are exhausted. This assumes all input sources are already sorted according to the same comparator.
	 *
	 * @param comparator the comparator to determine which source provides the next item
	 * @param sources the sorted publishers to merge
	 * @return a Publisher emitting items in sorted order
	 * @param <T> the type of items emitted by the publishers
	 */
	public static <T> Publisher<T> orderedMerge(Comparator<T> comparator, Publisher<T>... sources) {
		return new OrderedMerger<>(comparator, sources).createPublisher();
	}

	/**
	 * Zips publishers until all sources have emitted and completed, unlike standard Flowable zip operations
	 * which stop when the shortest source completes.
	 *
	 * <p>Values are wrapped in {@link Optional} to distinguish between "no value emitted" and "null value".
	 * Once a source completes, it continues emitting empty Optionals to allow other sources to catch up.
	 *
	 * <p>This is useful when you want to track the latest value from each source even after some have completed.
	 *
	 * @param zipper function to combine values from all sources (receives Object array of Optional values)
	 * @param delayError if true, errors are delayed until all sources complete; if false, errors terminate immediately
	 * @param bufferSize the buffer size for each source
	 * @param sources the publishers to zip
	 * @return a Flowable emitting combined values from all sources
	 * @param <T> the type of items emitted by the publishers
	 * @param <R> the type of items emitted by the resulting Flowable
	 */
	public static <@NonNull T, @NonNull R> Flowable<R> zipAllFlowable(
			@NonNull Function<? super Object[], ? extends R> zipper,
			boolean delayError,
			int bufferSize,
			@NonNull Publisher<? extends T>... sources) {
		return new ZipAll<T, R>(zipper, delayError, bufferSize, sources).createFlowable();
	}

	/**
	 * Convenience overload of {@link #zipAllFlowable(Function, boolean, int, Publisher[])} with default settings
	 * (delayError=false, bufferSize=Flowable.bufferSize()).
	 *
	 * @param zipper function to combine values from all sources
	 * @param sources the publishers to zip
	 * @return a Flowable emitting combined values from all sources
	 * @param <T> the type of items emitted by the publishers
	 * @param <R> the type of items emitted by the resulting Flowable
	 */
	public static <@NonNull T, @NonNull R> Flowable<R> zipAllFlowable(
			@NonNull Function<? super Object[], ? extends R> zipper, @NonNull Publisher<? extends T>... sources) {
		return zipAllFlowable(zipper, false, Flowable.bufferSize(), sources);
	}

	/**
	 * Sorts a stream within a sliding window.
	 * @param comparator the comparator
	 * @param minWindowSize the minimum window size. The actual sorting window will be larger.
	 */
	public static <@NonNull T> WindowSort<T> windowSort(Comparator<T> comparator, int minWindowSize) {
		return new WindowSort<>(comparator, minWindowSize);
	}

	/**
	 * Creates a transformer which will error if the stream isn't strictly ordered.
	 * @param comparator the comparator
	 */
	public static <@NonNull T> CheckOrder<T> checkOrder(Comparator<T> comparator) {
		return new CheckOrder<T>(comparator);
	}

	/**
	 * Creates a transformer that retries a Flowable a specified number of times with a delay between attempts.
	 *
	 * <p>This is a convenience overload that retries on all errors. Use {@link #retryWithDelayFlowable(int, Duration, Predicate)}
	 * to selectively retry only certain errors.
	 *
	 * <p><b>Example:</b>
	 * <pre>{@code
	 * Flowable<String> source = apiCall();
	 * source.compose(Rx3Util.retryWithDelayFlowable(3, Duration.ofSeconds(1)))
	 *     .subscribe(
	 *         item -> System.out.println(item),
	 *         error -> System.err.println("Failed after retries: " + error)
	 *     );
	 * }</pre>
	 *
	 * @param times the maximum number of retry attempts (0 means no retries)
	 * @param delay the delay between retry attempts
	 * @return a FlowableTransformer that retries with delay on error
	 * @param <T> the type of items in the Flowable
	 */
	public static <T> FlowableTransformer<T, T> retryWithDelayFlowable(int times, Duration delay) {
		return retryWithDelayFlowable(times, delay, e -> true);
	}

	/**
	 * Creates a transformer that retries a Flowable a specified number of times with a delay between attempts,
	 * only for errors matching a predicate.
	 *
	 * <p><b>Example:</b>
	 * <pre>{@code
	 * Flowable<String> source = apiCall();
	 * source.compose(Rx3Util.retryWithDelayFlowable(3, Duration.ofSeconds(1), error -> error instanceof TimeoutException))
	 *     .subscribe(
	 *         item -> System.out.println(item),
	 *         error -> System.err.println("Failed: " + error)
	 *     );
	 * }</pre>
	 *
	 * @param times the maximum number of retry attempts (0 means no retries)
	 * @param delay the delay between retry attempts
	 * @param predicate determines whether to retry for a given error
	 * @return a FlowableTransformer that retries with delay on matching errors
	 * @param <T> the type of items in the Flowable
	 * @throws IllegalArgumentException if times is negative or delay is negative
	 */
	public static <T> FlowableTransformer<T, T> retryWithDelayFlowable(
			int times, Duration delay, Predicate<? super Throwable> predicate) {
		if (times < 0) {
			throw new IllegalArgumentException("times >= 0 required but it was " + times);
		}
		var delayNs = delay.toNanos();
		if (delayNs < 0) {
			throw new IllegalArgumentException("delay must be zero or more");
		}
		return upstream -> upstream.retryWhen(e -> {
			var t = new AtomicInteger();
			return e.flatMap(err -> {
				int i = t.incrementAndGet();
				if (i <= times && predicate.test(err)) {
					return Flowable.timer(delayNs, TimeUnit.NANOSECONDS);
				}
				return Flowable.error(err);
			});
		});
	}
}
