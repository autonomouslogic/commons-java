package com.autonomouslogic.commons.updater;

import org.reactivestreams.Publisher;

public interface UpdateFetcher<T, M> {
	Publisher<UpdateItem<T, M>> fetchUpdate(UpdateItem<T, M> lastUpdate);
}
