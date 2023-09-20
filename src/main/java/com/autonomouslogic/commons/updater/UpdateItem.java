package com.autonomouslogic.commons.updater;

import java.time.Clock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "of")
public class UpdateItem<T, M> {
	UpdateMeta<M> updateMeta;
	T item;

	public static <T, M> UpdateItem<T, M> from(T item, M meta) {
		return of(UpdateMeta.of(meta), item);
	}

	public static <T, M> UpdateItem<T, M> from(T item, M meta, Instant time) {
		return of(UpdateMeta.of(meta, time), item);
	}

	public static <T, M> UpdateItem<T, M> from(T item, M meta, Clock clock) {
		return of(UpdateMeta.of(meta, clock), item);
	}
}
