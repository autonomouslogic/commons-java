package com.autonomouslogic.commons;

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

	public static Stopwatch start() {
		return new Stopwatch(System.nanoTime());
	}

	public void stop() {
		var now = System.nanoTime();
		if (!running) {
			return;
		}
		nanos += now - start;
		running = false;
	}
}
