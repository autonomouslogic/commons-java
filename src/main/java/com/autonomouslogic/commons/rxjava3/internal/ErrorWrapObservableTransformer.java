package com.autonomouslogic.commons.rxjava3.internal;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ErrorWrapObservableTransformer<U, D> implements ObservableTransformer<U, D> {
	private final String message;
	private final ObservableTransformer<U, D> transformer;
	private boolean upstreamError = false;

	@Override
	public @NonNull ObservableSource<D> apply(@NonNull Observable<U> upstream) {
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
