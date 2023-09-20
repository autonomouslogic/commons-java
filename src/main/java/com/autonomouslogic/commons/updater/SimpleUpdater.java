package com.autonomouslogic.commons.updater;

import io.reactivex.rxjava3.core.Maybe;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor(staticName = "from")
public class SimpleUpdater<T, M, R> {
	@NonNull
	private final UpdateFetcher<T, M> updater;

	@NonNull
	private final UpdateChecker<T, M> checker;

	@NonNull
	private final UpdateTransformer<T, M, R> transformer;

	private volatile UpdateItem<T, M> lastUpdate;

	public Publisher<UpdateItem<R, M>> updateNow() {
		return Maybe.fromPublisher(updater.fetchUpdate(lastUpdate))
				.filter(item -> checker.isNew(lastUpdate, item))
				.map(item -> {
					lastUpdate = item;
					var transformed = UpdateItem.of(transformer.transform(item), item.getUpdateMeta());
					return transformed;
				})
				.toFlowable();
	}
}
