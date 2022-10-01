package com.autonomouslogic.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.Stopwatch;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class StopwatchTest {
	@Test
	@SneakyThrows
	public void shouldMeasureTime() {
		var start = System.nanoTime();
		var watch = Stopwatch.start();
		Thread.sleep(500);
		var finish = System.nanoTime();
		watch.stop();
		double time = finish - start;
		assertEquals(time, watch.getNanos(), 10e6); // within 10 ms to allow for GC.
	}

	@Test
	@SneakyThrows
	public void shouldContinueMeasuringTime() {
		// First run.
		var start1 = System.nanoTime();
		var watch = Stopwatch.start();
		Thread.sleep(500);
		var finish1 = System.nanoTime();
		watch.stop();

		Thread.sleep(500);

		// Second run.
		var start2 = System.nanoTime();
		watch.restart();
		Thread.sleep(500);
		var finish2 = System.nanoTime();
		watch.stop();

		double time = finish1 - start1 + finish2 - start2;
		assertEquals(time, watch.getNanos(), 10e6); // within 10 ms to allow for GC.
	}
}
