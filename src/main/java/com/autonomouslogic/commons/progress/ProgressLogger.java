package com.autonomouslogic.commons.progress;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.Instant;

/**
 * Simple tool for managing the logging (or any other action) as something completes.
 */
public class ProgressLogger {
	private long progress = 0;
	private long end = -1;
	private final Instant start = Instant.now();

	@Getter
	private long realInterval = -1;
	@Getter
	private double progressInterval = -1;
	@Getter
	private Duration timeInterval;

	protected ProgressLogger(long realInterval, double progressInterval, Duration timeInterval, long end) {
		this.realInterval = realInterval;
		this.progressInterval = progressInterval;
		this.timeInterval = timeInterval;
		this.end = end;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Accessors(fluent = true)
	public static class Builder {
		@Setter
		private long realInterval = -1;

		@Setter
		private double progressInterval = -1;

		@Setter
		private Duration timeInterval;

		@Setter
		private long end;

		protected Builder() {};

		public ProgressLogger build() {
			return new ProgressLogger(realInterval, progressInterval, timeInterval, end);
		}
	}
}
