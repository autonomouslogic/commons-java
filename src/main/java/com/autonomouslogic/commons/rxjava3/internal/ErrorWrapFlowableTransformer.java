package com.autonomouslogic.commons.rxjava3.internal;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
public final class ErrorWrapFlowableTransformer<U, D> implements FlowableTransformer<U, D> {
	private final String message;
	private final FlowableTransformer<U, D> transformer;
	private boolean upstreamError = false;

	@Override
	public @NonNull Publisher<D> apply(@NonNull Flowable<U> upstream) {
		return upstream.doOnError(e -> upstreamError = true)
				.compose(transformer)
				.onErrorResumeNext(e -> {
					if (!upstreamError) {
						return Flowable.error(new RuntimeException(message, e));
					}
					return Flowable.error(e);
				});
	}
}
