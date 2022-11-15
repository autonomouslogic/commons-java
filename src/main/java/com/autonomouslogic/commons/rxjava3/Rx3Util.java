package com.autonomouslogic.commons.rxjava3;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

/**
 * Various helper methods for working with RxJava 3.
 */
public class Rx3Util {
	/**
	 * Converts a {@link CompletionStage} to a {@link Single}.
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
}
