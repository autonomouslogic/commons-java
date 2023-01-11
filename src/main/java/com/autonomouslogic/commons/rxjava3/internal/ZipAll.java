package com.autonomouslogic.commons.rxjava3.internal;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class ZipAll<T, R> {
	@NonNull
	private final Function<? super Object[], ? extends R> zipper;

	private final boolean delayError;
	private final int bufferSize;

	@NonNull
	private final Publisher[] sources;

	public Flowable<R> createFlowable() {
		return Flowable.zipArray(v -> v, delayError, bufferSize, padSources())
				.takeWhile(predicate())
				.map(zipper);
	}

	private Publisher[] padSources() {
		var padded = new Publisher[sources.length];
		for (int i = 0; i < sources.length; i++) {
			padded[i] = Flowable.concat(map(sources[i]), pad());
		}
		return padded;
	}

	private Publisher<Optional<?>> map(Publisher<?> source) {
		return Flowable.fromPublisher(source).map(Optional::of);
	}

	private Publisher<Optional<?>> pad() {
		return Flowable.generate(emitter -> emitter.onNext(Optional.empty()));
	}

	private Predicate<?> predicate() {
		return in -> {
			var objs = (Object[]) in;
			for (var obj : objs) {
				if (((Optional) obj).isPresent()) {
					return true;
				}
			}
			return false;
		};
	}
}
