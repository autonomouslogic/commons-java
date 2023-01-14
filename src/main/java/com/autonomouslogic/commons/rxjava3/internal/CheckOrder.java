package com.autonomouslogic.commons.rxjava3.internal;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
public class CheckOrder<T> implements FlowableTransformer<T, T> {
	private static final int MAX_CONCURRENCY = 1;

	@lombok.NonNull
	private final Comparator<T> comparator;

	@Override
	public @NonNull Publisher<T> apply(@NonNull @lombok.NonNull Flowable<T> upstream) {
		final var last = new AtomicReference<T>();
		return upstream.flatMap(
				current -> {
					if (last.get() != null && comparator.compare(last.get(), current) > 0) {
						return Flowable.error(new RuntimeException(
								String.format("Stream isn't ordered - last: %s, current: %s", last, current)));
					}
					last.set(current);
					return Flowable.just(current);
				},
				MAX_CONCURRENCY);
	}
}
