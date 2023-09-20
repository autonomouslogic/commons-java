package com.autonomouslogic.commons.updater;

import java.time.Clock;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "of")
public class UpdateMeta<M> {
	M meta;
	Instant lastUpdated;

	static <M> UpdateMeta<M> from(M meta) {
		return from(meta, Clock.systemUTC());
	}

	static <M> UpdateMeta<M> from(M meta, Clock clock) {
		return of(meta, clock.instant());
	}
}
