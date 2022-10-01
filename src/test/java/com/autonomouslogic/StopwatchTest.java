package com.autonomouslogic;

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
}
