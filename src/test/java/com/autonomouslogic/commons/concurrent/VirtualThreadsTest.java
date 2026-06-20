package com.autonomouslogic.commons.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VirtualThreadsTest {
	@Nested
	class CallAllTests {
		@Test
		void shouldHandleSingleTaskUsingIterator() throws Exception {
			var results = VirtualThreads.callAll(
					List.of((Callable<String>) () -> "single-result").iterator(), 1);

			assertNotNull(results);
			assertEquals(1, results.size());
			assertEquals("single-result", results.get(0));
		}

		@Test
		void shouldHandleSingleTask() throws Exception {
			var results = VirtualThreads.callAll(Stream.of((Callable<String>) () -> "single-result"), 1);

			assertNotNull(results);
			assertEquals(1, results.size());
			assertEquals("single-result", results.get(0));
		}

		@Test
		void shouldExecuteAllTasksAndReturnResultsInOrder() throws Exception {
			testGeneric(50, 5);
		}

		@Test
		void shouldMaintainOrderWithLongTasks() throws Exception {
			var rng = SecureRandom.getInstanceStrong();
			var taskCount = 50;
			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Callable<Integer>) () -> {
				Thread.sleep(rng.nextInt(200) + 200);
				return i;
			});

			var results = VirtualThreads.callAll(tasks, 5);

			assertNotNull(results);
			assertEquals(taskCount, results.size());
			for (var i = 0; i < taskCount; i++) {
				assertEquals(i, results.get(i));
			}
		}

		@Test
		void shouldMaintainMaxConcurrency() throws Exception {
			var taskCount = 50;
			var concurrency = 5;
			var currentConcurrency = new AtomicInteger();
			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Callable<Integer>) () -> {
				var c = currentConcurrency.incrementAndGet();
				if (i >= taskCount && i < (taskCount - concurrency)) {
					assertEquals(concurrency, c);
				}
				Thread.sleep(100);
				return i;
			});

			var results = VirtualThreads.callAll(tasks, concurrency);

			assertNotNull(results);
			assertEquals(taskCount, results.size());
			for (var i = 0; i < taskCount; i++) {
				assertEquals(i, results.get(i));
			}
		}

		@Test
		void shouldHandleEmptyStream() throws Exception {
			var results = VirtualThreads.callAll(Stream.empty(), 5);

			assertNotNull(results);
			assertTrue(results.isEmpty());
		}

		@Test
		void shouldHandleHighConcurrencyWithFewTasks() throws Exception {
			testGeneric(5, 50);
		}

		@Test
		void shouldHandleConcurrencyLimitOfOne() throws Exception {
			testGeneric(5, 1);
		}

		@Test
		void shouldFailFastWhenTaskThrows() throws Exception {
			var taskCount = 100;
			var concurrency = 5;
			var tasksRun = new AtomicInteger();
			var failureMessage = "Task 0 failed";

			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Callable<Integer>) () -> {
				tasksRun.incrementAndGet();
				if (i == 0) {
					throw new RuntimeException(failureMessage);
				}
				Thread.sleep(50);
				return i;
			});

			var exception = assertThrows(ExecutionException.class, () -> VirtualThreads.callAll(tasks, concurrency));

			assertEquals(failureMessage, exception.getCause().getMessage());
			assertTrue(
					tasksRun.get() <= concurrency + 1,
					"Expected at most concurrency + 1 tasks to run, but " + tasksRun.get() + " tasks ran");
		}

		private static void testGeneric(int taskCount, int concurrency)
				throws InterruptedException, ExecutionException {
			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Callable<Integer>) () -> i);

			var results = VirtualThreads.callAll(tasks, concurrency);

			assertNotNull(results);
			assertEquals(taskCount, results.size());
			for (var i = 0; i < taskCount; i++) {
				assertEquals(i, results.get(i));
			}
		}

		@Test
		void shouldHandleIterableOfCallables() throws Exception {
			var tasks = new ArrayList<Callable<Integer>>();
			for (int i = 0; i < 10; i++) {
				int index = i;
				tasks.add(() -> index);
			}

			var results = VirtualThreads.callAll(tasks, 5);

			assertNotNull(results);
			assertEquals(10, results.size());
			for (int i = 0; i < 10; i++) {
				assertEquals(i, results.get(i));
			}
		}
	}

	@Nested
	class RunAllTests {
		@Test
		void shouldHandleSingleTaskUsingIterator() throws Exception {
			VirtualThreads.runAll(List.of((Runnable) () -> {}).iterator(), 1);
		}

		@Test
		void shouldHandleSingleTask() throws Exception {
			VirtualThreads.runAll(Stream.of((Runnable) () -> {}), 1);
		}

		@Test
		void shouldExecuteAllTasks() throws Exception {
			testGeneric(50, 5);
		}

		@Test
		void shouldExecuteAllTasksWithLongDuration() throws Exception {
			var rng = SecureRandom.getInstanceStrong();
			var taskCount = 50;
			var tasksRun = new AtomicInteger();

			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Runnable) () -> {
				try {
					Thread.sleep(rng.nextInt(200) + 200);
					tasksRun.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			VirtualThreads.runAll(tasks, 5);

			assertEquals(taskCount, tasksRun.get());
		}

		@Test
		void shouldMaintainMaxConcurrency() throws Exception {
			var taskCount = 50;
			var concurrency = 5;
			var currentConcurrency = new AtomicInteger();
			var maxObserved = new AtomicInteger();

			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Runnable) () -> {
				var c = currentConcurrency.incrementAndGet();
				maxObserved.accumulateAndGet(c, Math::max);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				currentConcurrency.decrementAndGet();
			});

			VirtualThreads.runAll(tasks, concurrency);

			assertTrue(
					maxObserved.get() <= concurrency,
					"Max concurrency was " + maxObserved.get() + ", expected at most " + concurrency);
		}

		@Test
		void shouldHandleEmptyStream() throws Exception {
			VirtualThreads.runAll(Stream.empty(), 5);
		}

		@Test
		void shouldExecuteAllTasksWithHighConcurrencyAndFewTasks() throws Exception {
			testGeneric(5, 50);
		}

		@Test
		void shouldHandleConcurrencyLimitOfOne() throws Exception {
			testGeneric(5, 1);
		}

		@Test
		void shouldFailFastWhenTaskThrows() throws Exception {
			var taskCount = 100;
			var concurrency = 5;
			var tasksRun = new AtomicInteger();
			var failureMessage = "Task 0 failed";

			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Runnable) () -> {
				tasksRun.incrementAndGet();
				if (i == 0) {
					throw new RuntimeException(failureMessage);
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			var exception = assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(tasks, concurrency));

			assertEquals(failureMessage, exception.getCause().getMessage());
			assertTrue(
					tasksRun.get() <= concurrency + 1,
					"Expected at most concurrency + 1 tasks to run, but " + tasksRun.get() + " tasks ran");
		}

		private static void testGeneric(int taskCount, int concurrency)
				throws InterruptedException, ExecutionException {
			var tasksRun = new AtomicInteger();

			var tasks = IntStream.range(0, taskCount).mapToObj(i -> (Runnable) () -> tasksRun.incrementAndGet());

			VirtualThreads.runAll(tasks, concurrency);

			assertEquals(taskCount, tasksRun.get());
		}

		@Test
		void shouldHandleIterableOfRunnables() throws Exception {
			var tasksRun = new AtomicInteger();
			var tasks = new ArrayList<Runnable>();
			for (int i = 0; i < 10; i++) {
				tasks.add(tasksRun::incrementAndGet);
			}

			VirtualThreads.runAll(tasks, 5);

			assertEquals(10, tasksRun.get());
		}
	}

	@Nested
	class InterruptTests {
		@Test
		void callAllShouldPropagateInterruptAndResetFlag() throws Exception {
			var maxConcurrency = 2;
			var started = new CountDownLatch(maxConcurrency);
			var blocker = new CountDownLatch(1);
			var callingThread = Thread.currentThread();

			var tasks = List.<Callable<Integer>>of(
					() -> {
						started.countDown();
						blocker.await();
						return 1;
					},
					() -> {
						started.countDown();
						blocker.await();
						return 2;
					});

			var interrupter = Thread.ofVirtual().start(() -> {
				try {
					started.await();
					callingThread.interrupt();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			try {
				var ex = assertThrows(InterruptedException.class, () -> VirtualThreads.callAll(tasks, maxConcurrency));
				assertNotNull(ex);
				assertTrue(
						Thread.currentThread().isInterrupted(),
						"Interrupt flag must be re-set after InterruptedException from callAll");
			} finally {
				blocker.countDown();
				Thread.interrupted(); // clear flag so JUnit teardown is unaffected
				interrupter.join(2000);
			}
		}

		@Test
		void runAllShouldPropagateInterruptAndResetFlag() throws Exception {
			var maxConcurrency = 2;
			var started = new CountDownLatch(maxConcurrency);
			var blocker = new CountDownLatch(1);
			var callingThread = Thread.currentThread();

			var tasks = List.<Runnable>of(
					() -> {
						try {
							started.countDown();
							blocker.await();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					},
					() -> {
						try {
							started.countDown();
							blocker.await();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					});

			var interrupter = Thread.ofVirtual().start(() -> {
				try {
					started.await();
					callingThread.interrupt();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			try {
				var ex = assertThrows(InterruptedException.class, () -> VirtualThreads.runAll(tasks, maxConcurrency));
				assertNotNull(ex);
				assertTrue(
						Thread.currentThread().isInterrupted(),
						"Interrupt flag must be re-set after InterruptedException from runAll");
			} finally {
				blocker.countDown();
				Thread.interrupted(); // clear flag so JUnit teardown is unaffected
				interrupter.join(2000);
			}
		}
	}

	@Nested
	class CallAllWithFunctionTests {
		@Test
		void shouldTransformInputsUsingIterator() throws Exception {
			var inputs = List.of(1, 2, 3, 4, 5);

			var results = VirtualThreads.callAll(inputs.iterator(), i -> i * 2, 3);

			assertNotNull(results);
			assertEquals(5, results.size());
			assertEquals(2, results.get(0));
			assertEquals(10, results.get(4));
		}

		@Test
		void shouldTransformInputsUsingStream() throws Exception {
			var inputs = IntStream.range(0, 10).boxed();

			var results = VirtualThreads.callAll(inputs, i -> i * 2, 5);

			assertNotNull(results);
			assertEquals(10, results.size());
			for (var i = 0; i < 10; i++) {
				assertEquals(i * 2, results.get(i));
			}
		}

		@Test
		void shouldTransformInputsUsingIterable() throws Exception {
			var inputs = List.of(1, 2, 3, 4, 5);

			var results = VirtualThreads.callAll(inputs, i -> i * 2, 3);

			assertNotNull(results);
			assertEquals(5, results.size());
			assertEquals(2, results.get(0));
			assertEquals(10, results.get(4));
		}
	}

	@Nested
	class RunAllWithConsumerTests {
		@Test
		void shouldProcessInputsUsingIterator() throws Exception {
			var processed = new AtomicInteger();
			var inputs = List.of(1, 2, 3, 4, 5);

			VirtualThreads.runAll(inputs.iterator(), i -> processed.incrementAndGet(), 3);

			assertEquals(5, processed.get());
		}

		@Test
		void shouldProcessInputsUsingStream() throws Exception {
			var inputs = IntStream.range(0, 10).boxed();
			var processed = new AtomicInteger();

			VirtualThreads.runAll(inputs, i -> processed.incrementAndGet(), 5);

			assertEquals(10, processed.get());
		}

		@Test
		void shouldProcessInputsUsingIterable() throws Exception {
			var processed = new AtomicInteger();
			var inputs = List.of(1, 2, 3, 4, 5);

			VirtualThreads.runAll(inputs, i -> processed.incrementAndGet(), 3);

			assertEquals(5, processed.get());
		}
	}
}
