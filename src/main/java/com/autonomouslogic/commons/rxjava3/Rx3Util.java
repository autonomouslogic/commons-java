package com.autonomouslogic.commons.rxjava3;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Various helper methods for working with RxJava 3.
 */
public class Rx3Util {
	private Rx3Util() {}

	/**
	 * Converts a {@link CompletionStage} to a {@link Single}.
	 *
	 * Null return values will result in an error from RxJava, as those aren't allowed.
	 * Use {@link #toMaybe(CompletionStage)} instead to handle null values properly.
	 *
	 * {@link Single#fromFuture(Future)} works in a blocking fashion, whereas {@link CompletionStage} can be utilised to avoid blocking calls.
	 *
	 * @param future the completion stage
	 * @return the Single
	 * @param <T> the return parameter of the future
	 */
	public static <T> Single<T> toSingle(CompletionStage<T> future) {
		return Single.create(subscriber -> {
			future.thenAccept(result -> {
						subscriber.onSuccess(result);
					})
					.exceptionally(e -> {
						subscriber.onError(e);
						return null;
					});
		});
	}

	/**
	 * Converts a {@link CompletionStage} to a {@link Maybe}.
	 *
	 * Null return values will result in an empty Maybe.
	 *
	 * {@link Maybe#fromFuture(Future)} works in a blocking fashion, whereas {@link CompletionStage} can be utilised to avoid blocking calls.
	 *
	 * @param future the completion stage
	 * @return the Maybe
	 * @param <T> the return parameter of the future
	 */
	public static <T> Maybe<T> toMaybe(CompletionStage<T> future) {
		return Maybe.create(subscriber -> {
			future.thenAccept(result -> {
						if (result == null) {
							subscriber.onComplete();
						} else {
							subscriber.onSuccess(result);
						}
					})
					.exceptionally(e -> {
						subscriber.onError(e);
						return null;
					});
		});
	}

	/**
	 * Converts a {@link CompletionStage} to a {@link Completable}.
	 *
	 * {@link Completable#fromFuture(Future)} works in a blocking fashion, whereas {@link CompletionStage} can be utilised to avoid blocking calls.
	 *
	 * @param future the completion stage
	 * @return the Completable
	 */
	public static Completable toCompletable(CompletionStage<Void> future) {
		return Completable.create(subscriber -> {
			future.thenAccept(ignore -> {
						subscriber.onComplete();
					})
					.exceptionally(e -> {
						subscriber.onError(e);
						return null;
					});
		});
	}

	/**
	 * Wraps an {@link ObservableTransformer} such that any errors thrown from anything done by the
	 * <code>transformer</code> will be wrapped in a {@link RuntimeException} with the provided message.
	 * Any errors occurring before or after the <code>transformer</code> is applied are not subject to exception
	 * wrapping.
	 * This is useful when debugging multithreaded asynchronous code where the relevant stack trace can get lost.
	 *
	 * @param message message for the exception
	 * @param transformer transformer used for composition
	 * @return a new transformer
	 * @param <Upstream> see ObservableTransformer
	 * @param <Downstream> see ObservableTransformer
	 */
	public static <Upstream, Downstream> ObservableTransformer<Upstream, Downstream> wrapTransformerErrors(
			String message, ObservableTransformer<Upstream, Downstream> transformer) {
		return new ErrorWrapObservableTransformer<>(message, transformer);
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class ErrorWrapObservableTransformer<Upstream, Downstream>
			implements ObservableTransformer<Upstream, Downstream> {
		private final String message;
		private final ObservableTransformer<Upstream, Downstream> transformer;
		private boolean upstreamError = false;

		@Override
		public @NonNull ObservableSource<Downstream> apply(@NonNull Observable<Upstream> upstream) {
			return upstream.doOnError(e -> upstreamError = true)
					.compose(transformer)
					.onErrorResumeNext(e -> {
						if (!upstreamError) {
							return Observable.error(new RuntimeException(message, e));
						}
						return Observable.error(e);
					});
		}
	}
}
