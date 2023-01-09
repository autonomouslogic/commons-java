package com.autonomouslogic.commons.rxjava3.internal;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.operators.flowable.BlockingFlowableIterable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

/**
 * Implementation for the sorted merge operation.
 * The current implementation naively blocks on an IO thread.
 * This is far from ideal and commits are welcome.
 * @param <T>
 */
@RequiredArgsConstructor
public class OrderedMerger<T> {
	@NonNull
	private final Comparator<T> comparator;
	@NonNull
	private final Publisher<T>[] sources;

	public Publisher<T> createPublisher() {
		if (sources.length == 0) {
			return Flowable.empty();
		}
		return Flowable.generate(
				() -> new MergeState<>(comparator, sources),
				(BiFunction<MergeState<T>, Emitter<T>, MergeState<T>>) (state, emitter) -> {
					state.next(emitter);
					return state;
				},
			mergeState -> mergeState.dispose()
		)
			.subscribeOn(Schedulers.io())
			.observeOn(Schedulers.computation());
	}

	private static class MergeState<P> {
		private final int n;
		private final Comparator<P> comparator;
		private final Iterator<P>[] iterators;
		private final Object[] current;

		MergeState(Comparator<P> comparator, Publisher<P>[] sources) {
			this.comparator = comparator;
			n = sources.length;
			iterators = new Iterator[n];
			current = new Object[n];
			for (int i = 0; i < n; i++) {
				iterators[i] = Flowable.fromPublisher(sources[i]).blockingIterable().iterator();
			}
			fill();
		}

		private void fill() {
			for (int i = 0; i < n; i++) {
				if (current[i] == null && iterators[i].hasNext()) {
					current[i] = iterators[i].next();
				}
			}
		}

		@SuppressWarnings("unchecked")
		private int nextIndex() {
			fill();
			var j = -1;
			var c = 0;
			for (int i = 1; i < n; i++) {
				if (current[c] == null) {
					c = i;
				}
				else if (current[i] != null) {
					if (comparator.compare((P) current[c], (P) current[i]) < 0) {
						j = c;
					}
					else {
						j = i;
					}
				}
				else {
					j = c;
				}
			}
			return j;
		}

		@SuppressWarnings("unchecked")
		protected void next(Emitter<P> emitter) {
			var i = nextIndex();
			if (i == -1) {
				emitter.onComplete();
			}
			emitter.onNext((P) current[i]);
			current[i] = null;
		}

		protected void dispose() {
			// @todo not implemented
		}
	}
}
