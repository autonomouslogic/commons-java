package com.autonomouslogic.commons.rxjava3;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import java.util.ArrayList;
import java.util.Comparator;
import org.reactivestreams.Publisher;

public class WindowSort<T> implements FlowableTransformer<T, T> {
	@lombok.NonNull
	private final Comparator<T> comparator;

	private final int minWindowSize;

	public WindowSort(@lombok.NonNull Comparator<T> comparator, int minWindowSize) {
		if (minWindowSize < 1) {
			throw new IllegalArgumentException("minWindowSize must be at least 1");
		}
		this.comparator = comparator;
		this.minWindowSize = minWindowSize;
	}

	@Override
	public @NonNull Publisher<T> apply(@NonNull Flowable<T> upstream) {
		return Flowable.defer(() -> {
			final var window = new ArrayList<T>(minWindowSize * 2);
			final var tmp = new ArrayList<T>(minWindowSize);
			var sorted = upstream.buffer(minWindowSize)
					.flatMap(
							buffer -> {
								window.addAll(buffer);
								window.sort(comparator);
								var len = window.size() - minWindowSize;
								if (len <= 0) {
									return Flowable.empty();
								}
								var result = new ArrayList<>(window.subList(0, len));
								tmp.addAll(window.subList(len, window.size()));
								window.clear();
								window.addAll(tmp);
								tmp.clear();
								return Flowable.fromIterable(result);
							},
							1);
			var remaining = Flowable.defer(() -> Flowable.fromIterable(window));
			return Flowable.concat(sorted, remaining);
		});
	}
}
