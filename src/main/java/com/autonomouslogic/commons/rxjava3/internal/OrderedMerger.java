package com.autonomouslogic.commons.rxjava3.internal;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
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
						mergeState -> mergeState.dispose())
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation());
	}

	private static class MergeState<P> {
		private final int n;
		private final Comparator<P> comparator;
		private final Iterator<P>[] iterators;
		private final Object[] current;
		private final List<Entry> entries;
		private final Comparator<Entry> entryComparator;

		@SuppressWarnings("unchecked")
		MergeState(Comparator<P> comparator, Publisher<P>[] sources) {
			this.comparator = comparator;
			n = sources.length;
			iterators = new Iterator[n];
			entries = new ArrayList<>(n);
			current = new Object[n];
			for (int i = 0; i < n; i++) {
				iterators[i] =
						Flowable.fromPublisher(sources[i]).blockingIterable().iterator();
			}
			entryComparator = Comparator.comparing(entry -> (P) entry.getObj(), comparator);
			fill();
		}

		private void fill() {
			var changed = false;
			for (int i = 0; i < n; i++) {
				if (current[i] == null && iterators[i].hasNext()) {
					current[i] = iterators[i].next();
					entries.add(new Entry(i, current[i]));
					changed = true;
				}
			}
			if (changed) {
				entries.sort(entryComparator);
			}
		}

		@SuppressWarnings("unchecked")
		private P remove(int i) {
			var obj = current[i];
			current[i] = null;
			entries.remove(new Entry(i, null));
			return (P) obj;
		}

		private int nextIndex() {
			fill();
			if (entries.isEmpty()) {
				return -1;
			}
			return entries.get(0).getIndex();
		}

		protected void next(Emitter<P> emitter) {
			var i = nextIndex();
			if (i == -1) {
				emitter.onComplete();
			}
			emitter.onNext(remove(i));
		}

		protected void dispose() {
			// @todo not implemented
		}
	}

	@Value
	@EqualsAndHashCode(onlyExplicitlyIncluded = true)
	@ToString
	private static class Entry {
		@EqualsAndHashCode.Include
		int index;

		Object obj;
	}
}
