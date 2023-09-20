package com.autonomouslogic.commons.updater;

import java.time.Clock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "of")
public class UpdateItem<T, M> {
	T item;
	UpdateMeta<M> updateMeta;

	public static <T, M> UpdateItem<T, M> from(T item, M meta) {
		return of(item, UpdateMeta.from(meta));
	}

	public static <T, M> UpdateItem<T, M> from(T item, M meta, Instant time) {
		return of(item, UpdateMeta.of(meta, time));
	}

	public static <T, M> UpdateItem<T, M> from(T item, M meta, Clock clock) {
		return of(item, UpdateMeta.from(meta, clock));
	}
}
