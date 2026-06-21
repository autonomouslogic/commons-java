package com.autonomouslogic.commons;

import java.time.Duration;
import lombok.Getter;

/**
 * A simple stopwatch for measuring elapsed time with nanosecond precision.
 *
 * <p>Uses {@link System#nanoTime()} internally for high-resolution timing. Accuracy depends on the JVM
 * and underlying OS, but is generally better than millisecond precision. Suitable for benchmarking,
 * performance monitoring, and operation timing within your application.
 *
 * <p><b>Basic usage:</b>
 * <pre>{@code
 * Stopwatch watch = Stopwatch.start();
 * performSomeWork();
 * watch.stop();
 *
 * System.out.println("Elapsed: " + watch.getDuration());
 * System.out.println("Elapsed nanos: " + watch.getNanos());
 * }</pre>
 *
 * <p><b>Multiple measurements:</b>
 * <pre>{@code
 * Stopwatch watch = Stopwatch.start();
 * processFirstBatch();
 * watch.stop();
 *
 * // Do other work...
 *
 * watch.restart();  // Resume measuring
 * processSecondBatch();
 * watch.stop();
 *
 * // Total time from both batches
 * System.out.println("Total: " + watch.getDuration());
 * }</pre>
 *
 * <p><b>Note:</b> Calling {@link #stop()} multiple times or {@link #restart()} while already running
 * is safe — the stopwatch is idempotent and won't double-count time.
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
	 * Creates and starts a new stopwatch.
	 *
	 * <p>The stopwatch begins measuring immediately upon creation.
	 *
	 * @return a new running stopwatch
	 */
	public static Stopwatch start() {
		return new Stopwatch(System.nanoTime());
	}

	/**
	 * Resumes measurement by starting a new measurement cycle.
	 *
	 * <p>The new measurement time will be added to the accumulated total when stopped.
	 * If the stopwatch is already running, this call has no effect (idempotent).
	 *
	 * @see #stop()
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
	 * Stops the current measurement and accumulates the elapsed time.
	 *
	 * <p>If the stopwatch is not running, this call has no effect (idempotent).
	 * The total accumulated time can be retrieved via {@link #getNanos()} or {@link #getDuration()}.
	 *
	 * @see #restart()
	 * @see #getNanos()
	 * @see #getDuration()
	 */
	public void stop() {
		var now = System.nanoTime();
		if (!running) {
			return;
		}
		nanos += now - start;
		running = false;
	}

	/**
	 * Returns the total accumulated time as a {@link Duration}.
	 *
	 * @return the accumulated elapsed time
	 */
	public Duration getDuration() {
		return Duration.ofNanos(nanos);
	}
}
