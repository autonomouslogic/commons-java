package com.autonomouslogic.commons.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
 *   <li><b>Error/Throwable swallowing in {@code onVirtualThread}</b> (major): when called from a platform thread,
 *       only {@code RuntimeException} (Runnable variant) or {@code Exception} (Callable variant) is captured.
 *       {@code Error}s and sneakily-thrown checked exceptions escape to the virtual thread's uncaught handler and
 *       the method returns normally (the Callable variant returns {@code null}), silently reporting success.
 *       When already on a virtual thread the same throwable propagates, so behaviour depends on the calling
 *       context.</li>
 *   <li><b>Task leak on caller interruption in {@code onVirtualThread}</b> (major): if the caller is interrupted
 *       while joining, the spawned virtual thread is never interrupted and keeps running unobserved.</li>
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
 *   <li><b>Concurrent failures dropped</b> (medium): only the first observed {@code ExecutionException}
 *       propagates; failures of other in-flight tasks are silently discarded instead of being attached as
 *       suppressed exceptions.</li>
 * </ol>
 */
class VirtualThreadsShortcomingsTest {
	/**
	 * Issue 1: onVirtualThread only captures RuntimeException/Exception from the spawned virtual thread.
	 * Errors and sneaky checked throwables are lost and the method reports success.
	 */
	@Nested
	class OnVirtualThreadThrowableSwallowing {
		@Test
		@Timeout(10)
		void runnableErrorsShouldPropagateFromPlatformThreads() {
			// On a virtual thread, task.run() is inlined and this AssertionError propagates to the caller.
			// On a platform thread, it is silently dropped and onVirtualThread returns normally.
			Runnable task = () -> {
				throw new AssertionError("boom");
			};
			assertThrows(AssertionError.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		@Timeout(10)
		void callableErrorsShouldPropagateFromPlatformThreads() {
			// Currently returns null instead of throwing: the AssertionError goes to the uncaught
			// exception handler and the result AtomicReference is never set.
			Callable<Integer> task = () -> {
				throw new AssertionError("boom");
			};
			assertThrows(AssertionError.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		@Timeout(10)
		void runnableSneakyCheckedExceptionsShouldPropagateFromPlatformThreads() {
			// Runnables can throw checked exceptions via sneaky-throw (e.g. Lombok's @SneakyThrows).
			// catch (RuntimeException) misses them, so the failure is silently dropped.
			Runnable task = () -> sneakyThrow(new IOException("boom"));
			assertThrows(Throwable.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		@Timeout(10)
		void runnableLombokSneakyThrowsShouldPropagateFromPlatformThreads() {
			// @SneakyThrows bytecode-rewrites the throw so the JVM sees an unchecked throw at runtime,
			// but the caller's catch (RuntimeException) still misses it — IOException is not a RuntimeException.
			// The result is the same silent drop as the manual sneaky-throw above, but this test uses the
			// real Lombok annotation to confirm the behaviour is not an artifact of the manual helper.
			Runnable task = OnVirtualThreadThrowableSwallowing::throwSneakyCheckedException;
			assertThrows(IOException.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		@Timeout(10)
		void runAllShouldCaptureAssertionError() throws Exception {
			Runnable task = () -> {
				throw new AssertionError("boom");
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(List.of(task), 1));
			assertInstanceOf(AssertionError.class, ex.getCause());
		}

		@Test
		@Timeout(10)
		void callAllShouldCaptureAssertionError() throws Exception {
			Callable<Void> task = () -> {
				throw new AssertionError("boom");
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.callAll(List.of(task), 1));
			assertInstanceOf(AssertionError.class, ex.getCause());
		}

		@Test
		@Timeout(10)
		void runAllShouldCaptureManualSneakyCheckedException() throws Exception {
			Runnable task = () -> sneakyThrow(new IOException("boom"));
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
		}

		@Test
		@Timeout(10)
		void callAllShouldCaptureManualSneakyCheckedException() throws Exception {
			Callable<Void> task = () -> {
				sneakyThrow(new IOException("boom"));
				return null;
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.callAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
		}

		@Test
		@Timeout(10)
		void runAllShouldCaptureLombokSneakyThrows() throws Exception {
			Runnable task = OnVirtualThreadThrowableSwallowing::throwSneakyCheckedException;
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
		}

		@Test
		@Timeout(10)
		void callAllShouldCaptureLombokSneakyThrows() throws Exception {
			Callable<Void> task = () -> {
				throwSneakyCheckedException();
				return null;
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.callAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
		}

		@lombok.SneakyThrows
		private static void throwSneakyCheckedException() {
			throw new IOException("sneaky checked via @SneakyThrows");
		}

		@SuppressWarnings("unchecked")
		private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
			throw (E) e;
		}
	}

	/**
	 * Issue 2: when the caller of onVirtualThread is interrupted while waiting in join(), the spawned
	 * virtual thread is left running with nobody observing its outcome.
	 */
	@Nested
	class OnVirtualThreadInterruptLeak {
		@Test
		@Timeout(10)
		void interruptedCallerShouldCancelSpawnedTask() throws Exception {
			var taskStarted = new CountDownLatch(1);
			var taskInterrupted = new CountDownLatch(1);
			var blocker = new CountDownLatch(1);
			var callerInterrupted = new AtomicBoolean();

			Runnable task = () -> {
				taskStarted.countDown();
				try {
					blocker.await();
				} catch (InterruptedException e) {
					taskInterrupted.countDown();
				}
			};
			var caller = new Thread(() -> {
				try {
					VirtualThreads.onVirtualThread(task);
				} catch (InterruptedException e) {
					callerInterrupted.set(true);
				}
			});

			try {
				caller.start();
				assertTrue(taskStarted.await(2, TimeUnit.SECONDS), "task should have started");
				caller.interrupt();
				caller.join(2000);
				assertTrue(callerInterrupted.get(), "caller should observe the InterruptedException");
				assertTrue(
						taskInterrupted.await(500, TimeUnit.MILLISECONDS),
						"spawned virtual thread should be interrupted when the waiting caller is interrupted, "
								+ "but it was left running");
			} finally {
				blocker.countDown();
			}
		}
	}

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

	/**
	 * Issue 6: when several in-flight tasks fail, only the first failure taken from the completion queue
	 * propagates. The other failures are dropped instead of being attached as suppressed exceptions.
	 */
	@Nested
	class ConcurrentFailuresDropped {
		@Test
		@Timeout(10)
		void concurrentFailuresShouldBeRetainedAsSuppressed() {
			var secondaryStarted = new CountDownLatch(1);

			Callable<Integer> primary = () -> {
				secondaryStarted.await();
				throw new IllegalStateException("primary failure");
			};
			Callable<Integer> secondary = () -> {
				secondaryStarted.countDown();
				try {
					Thread.sleep(30_000);
					return 2;
				} catch (InterruptedException e) {
					throw new IllegalStateException("secondary failure");
				}
			};

			var thrown = assertThrows(
					ExecutionException.class, () -> VirtualThreads.callAll(List.of(primary, secondary), 2));

			assertEquals("primary failure", thrown.getCause().getMessage());
			assertTrue(
					Arrays.stream(thrown.getSuppressed())
							.anyMatch(s -> String.valueOf(s.getMessage()).contains("secondary failure")
									|| String.valueOf(s.getCause()).contains("secondary failure")),
					"failures of other in-flight tasks should be retained as suppressed exceptions, "
							+ "but were dropped: " + Arrays.toString(thrown.getSuppressed()));
		}
	}
}
