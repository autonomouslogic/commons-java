package com.autonomouslogic.commons.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
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
	 * Issue 3: the Iterator + Function/Consumer overloads materialise all inputs up front instead of
	 * consuming them lazily as concurrency slots free up. The Stream overloads are lazy, so identical
	 * logical calls behave differently depending on the input type.
	 */
	@Nested
	class EagerInputMaterialisation {
		@Test
		@Timeout(10)
		void callAllFunctionIteratorShouldConsumeInputsLazily() throws Exception {
			var inputs = List.of(0, 1, 2, 3, 4);
			var tasksStarted = new AtomicInteger();
			var startedWhenLastInputPulled = new AtomicInteger(-1);
			var lazyIterator = countingIterator(inputs, tasksStarted, startedWhenLastInputPulled);

			Function<Integer, Integer> fn = i -> {
				tasksStarted.incrementAndGet();
				return i * 2;
			};
			var results = VirtualThreads.callAll(lazyIterator, fn, 1);

			assertEquals(List.of(0, 2, 4, 6, 8), results);
			// With maxConcurrency=1 a lazy implementation completes each task before pulling the next
			// input, so by the time the last input is pulled at least one task must have started.
			// The current implementation drains the whole iterator before executing anything.
			assertTrue(
					startedWhenLastInputPulled.get() >= 1,
					"inputs should be consumed lazily, but the entire iterator was drained before any task "
							+ "started (tasks started when last input was pulled: "
							+ startedWhenLastInputPulled.get() + ")");
		}

		@Test
		@Timeout(10)
		void runAllConsumerIteratorShouldConsumeInputsLazily() throws Exception {
			var inputs = List.of(0, 1, 2, 3, 4);
			var tasksStarted = new AtomicInteger();
			var startedWhenLastInputPulled = new AtomicInteger(-1);
			var lazyIterator = countingIterator(inputs, tasksStarted, startedWhenLastInputPulled);

			Consumer<Integer> action = i -> tasksStarted.incrementAndGet();
			VirtualThreads.runAll(lazyIterator, action, 1);

			assertEquals(5, tasksStarted.get());
			assertTrue(
					startedWhenLastInputPulled.get() >= 1,
					"inputs should be consumed lazily, but the entire iterator was drained before any task "
							+ "started (tasks started when last input was pulled: "
							+ startedWhenLastInputPulled.get() + ")");
		}

		@Test
		@Timeout(10)
		void maxConcurrencyShouldBeValidatedBeforeInputsAreConsumed() {
			var pulled = new AtomicInteger();
			var iterator = new Iterator<Integer>() {
				@Override
				public boolean hasNext() {
					return pulled.get() < 5;
				}

				@Override
				public Integer next() {
					return pulled.incrementAndGet();
				}
			};

			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.callAll(iterator, i -> i, 0));
			// The current implementation drains all inputs into a list before delegating to the overload
			// that performs the validation.
			assertEquals(
					0,
					pulled.get(),
					"maxConcurrency should be rejected before any input is consumed, but " + pulled.get()
							+ " inputs were pulled first");
		}

		private static Iterator<Integer> countingIterator(
				List<Integer> inputs, AtomicInteger tasksStarted, AtomicInteger startedWhenLastInputPulled) {
			var delegate = inputs.iterator();
			return new Iterator<>() {
				int pulled = 0;

				@Override
				public boolean hasNext() {
					return delegate.hasNext();
				}

				@Override
				public Integer next() {
					pulled++;
					if (pulled == inputs.size()) {
						startedWhenLastInputPulled.set(tasksStarted.get());
					}
					return delegate.next();
				}
			};
		}
	}

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
