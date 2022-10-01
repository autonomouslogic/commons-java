package com.autonomouslogic.commons;

import java.time.Duration;
import lombok.Getter;

/**
 * Measures elapsed time.
 * Internally, time measurement is implemented using <code>System.nanoTime()</code>.
 * It will be as accurate as that implementation on whatever JVM it's running on.
 */
public class Stopwatch {
	private long start;

	@Getter
	private long nanos = 0;

	private boolean running = true;

	private Stopwatch(long start) {
		this.start = start;
	}

	/**
	 * Starts a new stopwatch.
	 * @return the stopwatch
	 */
	public static Stopwatch start() {
		return new Stopwatch(System.nanoTime());
	}

	/**
	 * Starts a new measurement, which will be added to the total time when stopped next.
	 */
	public void restart() {
		var now = System.nanoTime();
		if (running) {
			return;
		}
		start = now;
		running = true;
	}

	/**
	 * Stops the current measurement, if one is running.
	 */
	public void stop() {
		var now = System.nanoTime();
		if (!running) {
			return;
		}
		nanos += now - start;
		running = false;
	}

	public Duration getDuration() {
		return Duration.ofNanos(nanos);
	}
}
