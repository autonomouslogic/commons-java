package com.autonomouslogic.commons.concurrent;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Demonstrates known shortcomings in {@link VirtualThreads}.
 *
 * <p>Every test in this class asserts the <b>desired</b> behaviour and therefore <b>fails</b> against the current
 * implementation. Each failure is a reproducible demonstration of a specific defect. Once the implementation is
 * fixed, these tests should pass and can be folded into {@link VirtualThreadsTest}.
 *
 * <p>Issues demonstrated:
 * <ol>
 *   <li><b>Hidden 5-second stall on fail-fast</b> (medium): after a task failure, the hard-coded
 *       {@code awaitTermination(5, SECONDS)} delays exception propagation by up to 5 seconds when a task ignores
 *       interruption, and its return value is ignored, so tasks may still be running when the method throws.</li>
 * </ol>
 */
class VirtualThreadsShortcomingsTest {

	/**
	 * Issue 5: the fail-fast path blocks in a hard-coded awaitTermination(5, SECONDS) before rethrowing.
	 * A task that does not respond to interruption delays failure propagation by 5 seconds, and because
	 * the awaitTermination result is ignored, the method then throws while the task is still running.
	 */
	@Nested
	class FailFastStall {
		@Test
		@Timeout(30)
		void failFastShouldNotStallOnTasksIgnoringInterrupts() {
			var release = new CountDownLatch(1);
			var uncooperativeStarted = new CountDownLatch(1);

			Callable<Integer> failing = () -> {
				uncooperativeStarted.await();
				throw new RuntimeException("boom");
			};
			Callable<Integer> uncooperative = () -> {
				uncooperativeStarted.countDown();
				while (true) {
					try {
						release.await(15, TimeUnit.SECONDS);
						break;
					} catch (InterruptedException e) {
						// Simulates a task which does not respond to interruption.
					}
				}
				return 2;
			};

			var start = System.nanoTime();
			try {
				assertThrows(
						ExecutionException.class, () -> VirtualThreads.callAll(List.of(failing, uncooperative), 2));
			} finally {
				release.countDown();
			}
			var elapsedMs = (System.nanoTime() - start) / 1_000_000;

			assertTrue(
					elapsedMs < 2000,
					"task failure should propagate promptly, but took " + elapsedMs
							+ "ms because of the hard-coded 5s awaitTermination on an uncooperative task "
							+ "(whose continued execution after the throw is also not reported)");
		}
	}
}
