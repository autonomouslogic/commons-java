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
 *   <li><b>Eager input materialisation</b> (major): the {@code Iterator}/{@code Iterable} + {@code Function}/
 *       {@code Consumer} overloads drain the entire input into an {@code ArrayList} before executing anything.
 *       This defeats bounded-memory streaming, prevents overlap of input production and task execution, hangs
 *       forever on unbounded inputs, and is inconsistent with the {@code Stream} overloads, which are lazy.
 *       It also means {@code maxConcurrency} validation only happens after the input is fully consumed.</li>
 *   <li><b>In-flight task leak when input iteration fails</b> (major): if {@code hasNext()}/{@code next()} throws,
 *       already-submitted tasks are never interrupted ({@code shutdown()} instead of {@code shutdownNow()}) and
 *       keep running after the method has thrown — contradicting the class javadoc's claim that the executor is
 *       "cleaned up properly in all cases".</li>
 *   <li><b>Hidden 5-second stall on fail-fast</b> (medium): after a task failure, the hard-coded
 *       {@code awaitTermination(5, SECONDS)} delays exception propagation by up to 5 seconds when a task ignores
 *       interruption, and its return value is ignored, so tasks may still be running when the method throws.</li>
 * </ol>
 */
class VirtualThreadsShortcomingsTest {
	/**
	 * Issue 4: when the input iterator itself throws, the pump propagates the exception but only calls
	 * shutdown() (graceful), never shutdownNow(). Already-submitted tasks keep running unobserved after
	 * callAll/runAll has thrown.
	 */
	@Nested
	class InFlightTaskLeakOnInputFailure {
		@Test
		@Timeout(10)
		void inFlightTasksShouldBeCancelledWhenIteratorThrows() throws Exception {
			var taskStarted = new CountDownLatch(1);
			var taskInterrupted = new CountDownLatch(1);
			var blocker = new CountDownLatch(1);

			Callable<Integer> blockingTask = () -> {
				taskStarted.countDown();
				try {
					blocker.await();
					return 1;
				} catch (InterruptedException e) {
					taskInterrupted.countDown();
					throw e;
				}
			};
			// Yields one task, then fails on the next hasNext() call, while the first task is in flight.
			var tasks = new Iterator<Callable<Integer>>() {
				boolean first = true;

				@Override
				public boolean hasNext() {
					if (first) {
						return true;
					}
					throw new IllegalStateException("input iteration failed");
				}

				@Override
				public Callable<Integer> next() {
					first = false;
					return blockingTask;
				}
			};

			try {
				assertThrows(IllegalStateException.class, () -> VirtualThreads.callAll(tasks, 2));
				assertTrue(taskStarted.await(2, TimeUnit.SECONDS), "submitted task should have started");
				assertTrue(
						taskInterrupted.await(500, TimeUnit.MILLISECONDS),
						"in-flight tasks should be cancelled when input iteration fails, but the task was "
								+ "left running after callAll threw");
			} finally {
				blocker.countDown();
			}
		}
	}

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
