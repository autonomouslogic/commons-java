package com.autonomouslogic.commons.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class VirtualThreadsTest {
	class CountingIterator implements Iterator<Integer> {
		final AtomicInteger pulled = new AtomicInteger();
		int i = 0;

		public boolean hasNext() {
			return i < 5;
		}

		public Integer next() {
			pulled.incrementAndGet();
			return i++;
		}
	}

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

		@Test
		void shouldCaptureError() throws Exception {
			Callable<Void> task = () -> {
				throw new Error("boom");
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.callAll(List.of(task), 1));
			assertInstanceOf(Error.class, ex.getCause());
		}

		@Test
		void shouldCaptureManualSneakyCheckedException() throws Exception {
			Callable<Void> task = () -> {
				sneakyThrow(new IOException("boom"));
				return null;
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.callAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
		}

		@Test
		void shouldCaptureLombokSneakyThrowsException() throws Exception {
			Callable<Void> task = () -> {
				throwSneakyCheckedException();
				return null;
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.callAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
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

		@Test
		void shouldCaptureError() throws Exception {
			Runnable task = () -> {
				throw new Error("boom");
			};
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(List.of(task), 1));
			assertInstanceOf(Error.class, ex.getCause());
		}

		@Test
		void shouldCaptureManualSneakyCheckedException() throws Exception {
			Runnable task = () -> sneakyThrow(new IOException("boom"));
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
		}

		@Test
		void shouldCaptureLombokSneakyThrowsException() throws Exception {
			Runnable task = VirtualThreadsTest::throwSneakyCheckedException;
			var ex = assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(List.of(task), 1));
			assertInstanceOf(IOException.class, ex.getCause());
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

	@Nested
	class LazyInputConsumptionTests {
		@Test
		void callAllFunctionIteratorShouldConsumeInputsLazily() throws Exception {
			var iterator = new CountingIterator();
			var pulledAtFirstTask = new AtomicInteger(-1);

			var results = VirtualThreads.callAll(
					iterator,
					i -> {
						pulledAtFirstTask.compareAndSet(-1, iterator.pulled.get());
						return i * 2;
					},
					1);

			assertEquals(List.of(0, 2, 4, 6, 8), results);
			assertEquals(1, pulledAtFirstTask.get());
		}

		@Test
		void runAllConsumerIteratorShouldConsumeInputsLazily() throws Exception {
			var iterator = new CountingIterator();
			var pulledAtFirstTask = new AtomicInteger(-1);

			VirtualThreads.runAll(iterator, i -> pulledAtFirstTask.compareAndSet(-1, iterator.pulled.get()), 1);

			assertEquals(5, iterator.pulled.get());
			assertEquals(1, pulledAtFirstTask.get());
		}
	}

	@Nested
	class MaxConcurrencyValidationTests {
		Iterator<Callable<Integer>> callableIterator(CountingIterator source) {
			return new Iterator<>() {
				public boolean hasNext() {
					return source.hasNext();
				}

				public Callable<Integer> next() {
					var i = source.next();
					return () -> i;
				}
			};
		}

		Iterator<Runnable> runnableIterator(CountingIterator source) {
			return new Iterator<>() {
				public boolean hasNext() {
					return source.hasNext();
				}

				public Runnable next() {
					source.next();
					return () -> {};
				}
			};
		}

		@Test
		void callAllCallableIteratorShouldRejectInvalidMaxConcurrency() {
			var source = new CountingIterator();
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.callAll(callableIterator(source), 0));
			assertEquals(0, source.pulled.get());
		}

		@Test
		void callAllCallableIterableShouldRejectInvalidMaxConcurrency() {
			var source = new CountingIterator();
			assertThrows(
					IllegalArgumentException.class,
					() -> VirtualThreads.callAll((Iterable<Callable<Integer>>) () -> callableIterator(source), 0));
			assertEquals(0, source.pulled.get());
		}

		@Test
		void callAllCallableStreamShouldRejectInvalidMaxConcurrency() {
			var source = new CountingIterator();
			var stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(callableIterator(source), 0), false);
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.callAll(stream, 0));
			assertEquals(0, source.pulled.get());
		}

		@Test
		void runAllRunnableIteratorShouldRejectInvalidMaxConcurrency() {
			var source = new CountingIterator();
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.runAll(runnableIterator(source), 0));
			assertEquals(0, source.pulled.get());
		}

		@Test
		void runAllRunnableIterableShouldRejectInvalidMaxConcurrency() {
			var source = new CountingIterator();
			assertThrows(
					IllegalArgumentException.class,
					() -> VirtualThreads.runAll((Iterable<Runnable>) () -> runnableIterator(source), 0));
			assertEquals(0, source.pulled.get());
		}

		@Test
		void runAllRunnableStreamShouldRejectInvalidMaxConcurrency() {
			var source = new CountingIterator();
			var stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(runnableIterator(source), 0), false);
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.runAll(stream, 0));
			assertEquals(0, source.pulled.get());
		}

		@Test
		void callAllFunctionIteratorShouldRejectInvalidMaxConcurrency() {
			var iterator = new CountingIterator();
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.callAll(iterator, i -> i, 0));
			assertEquals(0, iterator.pulled.get());
		}

		@Test
		void callAllFunctionIterableShouldRejectInvalidMaxConcurrency() {
			var iterator = new CountingIterator();
			assertThrows(
					IllegalArgumentException.class,
					() -> VirtualThreads.callAll((Iterable<Integer>) () -> iterator, i -> i, 0));
			assertEquals(0, iterator.pulled.get());
		}

		@Test
		void callAllFunctionStreamShouldRejectInvalidMaxConcurrency() {
			var iterator = new CountingIterator();
			var stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.callAll(stream, i -> i, 0));
			assertEquals(0, iterator.pulled.get());
		}

		@Test
		void runAllConsumerIteratorShouldRejectInvalidMaxConcurrency() {
			var iterator = new CountingIterator();
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.runAll(iterator, i -> {}, 0));
			assertEquals(0, iterator.pulled.get());
		}

		@Test
		void runAllConsumerIterableShouldRejectInvalidMaxConcurrency() {
			var iterator = new CountingIterator();
			assertThrows(
					IllegalArgumentException.class,
					() -> VirtualThreads.runAll((Iterable<Integer>) () -> iterator, i -> {}, 0));
			assertEquals(0, iterator.pulled.get());
		}

		@Test
		void runAllConsumerStreamShouldRejectInvalidMaxConcurrency() {
			var iterator = new CountingIterator();
			var stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
			assertThrows(IllegalArgumentException.class, () -> VirtualThreads.runAll(stream, i -> {}, 0));
			assertEquals(0, iterator.pulled.get());
		}
	}

	@Nested
	class ThreadTypeTests {
		@Test
		void isVirtualShouldReturnFalseForPlatformThread() {
			assertTrue(!VirtualThreads.isVirtual());
		}

		@Test
		void isVirtualShouldReturnTrueForVirtualThread() throws Exception {
			var result = new AtomicInteger();
			var virtualThread = Thread.ofVirtual().start(() -> {
				result.set(VirtualThreads.isVirtual() ? 1 : 0);
			});

			virtualThread.join();

			assertEquals(1, result.get());
		}

		@Test
		void checkIsVirtualShouldThrowForPlatformThread() {
			assertThrows(NotVirtualThreadException.class, VirtualThreads::checkIsVirtual);
		}

		@Test
		void checkIsVirtualShouldNotThrowForVirtualThread() throws Exception {
			var exceptionThrown = new AtomicInteger();
			var virtualThread = Thread.ofVirtual().start(() -> {
				try {
					VirtualThreads.checkIsVirtual();
				} catch (NotVirtualThreadException e) {
					exceptionThrown.set(1);
				}
			});

			virtualThread.join();

			assertEquals(0, exceptionThrown.get());
		}
	}

	@Nested
	class OnVirtualThreadTests {
		@Test
		void runnableShouldExecuteImmediatelyOnVirtualThread() throws Exception {
			var executed = new AtomicInteger();
			var virtualThread = Thread.ofVirtual().start(() -> {
				try {
					VirtualThreads.onVirtualThread(() -> executed.set(1));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			virtualThread.join();

			assertEquals(1, executed.get());
		}

		@Test
		void runnableShouldCreateNewVirtualThreadFromPlatformThread() throws Exception {
			var executed = new AtomicInteger();
			var threadId = new AtomicInteger();

			VirtualThreads.onVirtualThread(() -> {
				executed.set(1);
				threadId.set((int) Thread.currentThread().threadId());
			});

			assertEquals(1, executed.get());
			assertNotEquals(Thread.currentThread().threadId(), threadId.get());
		}

		@Test
		void callableShouldReturnResultOnVirtualThread() throws Exception {
			var result = new AtomicInteger();
			var virtualThread = Thread.ofVirtual().start(() -> {
				try {
					var value = VirtualThreads.onVirtualThread(() -> 42);
					result.set(value);
				} catch (Exception e) {
					Thread.currentThread().interrupt();
				}
			});

			virtualThread.join();

			assertEquals(42, result.get());
		}

		@Test
		void callableShouldReturnResultFromCreatedVirtualThread() throws Exception {
			var result = VirtualThreads.onVirtualThread(() -> 123);

			assertEquals(123, result);
		}

		@Test
		void callableShouldPropagateException() {
			var failureMessage = "Test failure";

			Callable<Void> task = () -> {
				throw new RuntimeException(failureMessage);
			};
			var exception = assertThrows(RuntimeException.class, () -> VirtualThreads.onVirtualThread(task));

			assertEquals(failureMessage, exception.getMessage());
		}

		@Test
		void runnableShouldPropagateException() {
			var failureMessage = "Test failure";

			Runnable task = () -> {
				throw new RuntimeException(failureMessage);
			};
			var exception = assertThrows(RuntimeException.class, () -> VirtualThreads.onVirtualThread(task));

			assertEquals(failureMessage, exception.getMessage());
		}

		@Test
		void runnableShouldCaptureError() {
			Runnable task = () -> {
				throw new Error("boom");
			};
			assertThrows(Error.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		void callableShouldCaptureError() {
			Callable<Void> task = () -> {
				throw new Error("boom");
			};
			assertThrows(Error.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		void runnableShouldPropagateExceptionWhenAlreadyOnVirtualThread() throws Exception {
			var failureMessage = "Test failure on virtual thread";
			var exceptionThrown = new AtomicBoolean();
			var exceptionMessage = new AtomicReference<String>();

			var virtualThread = Thread.ofVirtual().start(() -> {
				Runnable task = () -> {
					throw new RuntimeException(failureMessage);
				};
				try {
					VirtualThreads.onVirtualThread(task);
				} catch (RuntimeException e) {
					exceptionThrown.set(true);
					exceptionMessage.set(e.getMessage());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			virtualThread.join();

			assertTrue(exceptionThrown.get());
			assertEquals(failureMessage, exceptionMessage.get());
		}

		@Test
		void callableShouldPropagateExceptionWhenAlreadyOnVirtualThread() throws Exception {
			var failureMessage = "Test failure on virtual thread";
			var exceptionThrown = new AtomicBoolean();
			var exceptionMessage = new AtomicReference<String>();

			var virtualThread = Thread.ofVirtual().start(() -> {
				Callable<Void> task = () -> {
					throw new RuntimeException(failureMessage);
				};
				try {
					VirtualThreads.onVirtualThread(task);
				} catch (RuntimeException e) {
					exceptionThrown.set(true);
					exceptionMessage.set(e.getMessage());
				} catch (Exception e) {
					Thread.currentThread().interrupt();
				}
			});

			virtualThread.join();

			assertTrue(exceptionThrown.get());
			assertEquals(failureMessage, exceptionMessage.get());
		}

		@Test
		void runnableShouldCaptureManualSneakyCheckedException() {
			Runnable task = () -> sneakyThrow(new IOException("boom"));
			assertThrows(IOException.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		void runnableShouldCaptureLombokSneakyThrowsException() {
			Runnable task = VirtualThreadsTest::throwSneakyCheckedException;
			assertThrows(IOException.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		void callableShouldCaptureManualSneakyCheckedException() {
			Callable<Void> task = () -> {
				sneakyThrow(new IOException("boom"));
				return null;
			};
			assertThrows(IOException.class, () -> VirtualThreads.onVirtualThread(task));
		}

		@Test
		void callableShouldCaptureLombokSneakyThrowsException() {
			Callable<Void> task = () -> {
				throwSneakyCheckedException();
				return null;
			};
			assertThrows(IOException.class, () -> VirtualThreads.onVirtualThread(task));
		}
	}

	@Nested
	class InterruptedCallerCancelsSpawnedTaskTests {
		@Test
		@Timeout(10)
		void onVirtualThreadRunnableShouldCancelSpawnedTask() throws Exception {
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

		@Test
		@Timeout(10)
		void onVirtualThreadCallableShouldCancelSpawnedTask() throws Exception {
			var taskStarted = new CountDownLatch(1);
			var taskInterrupted = new CountDownLatch(1);
			var blocker = new CountDownLatch(1);
			var callerInterrupted = new AtomicBoolean();

			Callable<Void> task = () -> {
				taskStarted.countDown();
				try {
					blocker.await();
				} catch (InterruptedException e) {
					taskInterrupted.countDown();
				}
				return null;
			};
			var caller = new Thread(() -> {
				try {
					VirtualThreads.onVirtualThread(task);
				} catch (InterruptedException e) {
					callerInterrupted.set(true);
				} catch (Exception e) {
					Thread.currentThread().interrupt();
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

		@Test
		@Timeout(10)
		void callAllShouldCancelSpawnedTasksWhenCallerInterrupted() throws Exception {
			var tasksStarted = new CountDownLatch(2);
			var tasksInterrupted = new CountDownLatch(2);
			var blocker = new CountDownLatch(1);
			var callerInterrupted = new AtomicBoolean();

			var tasks = List.<Callable<Void>>of(
					() -> {
						tasksStarted.countDown();
						try {
							blocker.await();
						} catch (InterruptedException e) {
							tasksInterrupted.countDown();
						}
						return null;
					},
					() -> {
						tasksStarted.countDown();
						try {
							blocker.await();
						} catch (InterruptedException e) {
							tasksInterrupted.countDown();
						}
						return null;
					});

			var caller = new Thread(() -> {
				try {
					VirtualThreads.callAll(tasks, 2);
				} catch (InterruptedException e) {
					callerInterrupted.set(true);
				} catch (ExecutionException e) {
					Thread.currentThread().interrupt();
				}
			});

			try {
				caller.start();
				assertTrue(tasksStarted.await(2, TimeUnit.SECONDS), "tasks should have started");
				caller.interrupt();
				caller.join(2000);
				assertTrue(callerInterrupted.get(), "caller should observe the InterruptedException");
				assertTrue(
						tasksInterrupted.await(500, TimeUnit.MILLISECONDS),
						"spawned virtual threads should be interrupted when the waiting caller is interrupted, "
								+ "but they were left running");
			} finally {
				blocker.countDown();
			}
		}

		@Test
		@Timeout(10)
		void runAllShouldCancelSpawnedTasksWhenCallerInterrupted() throws Exception {
			var tasksStarted = new CountDownLatch(2);
			var tasksInterrupted = new CountDownLatch(2);
			var blocker = new CountDownLatch(1);
			var callerInterrupted = new AtomicBoolean();

			var tasks = List.<Runnable>of(
					() -> {
						tasksStarted.countDown();
						try {
							blocker.await();
						} catch (InterruptedException e) {
							tasksInterrupted.countDown();
						}
					},
					() -> {
						tasksStarted.countDown();
						try {
							blocker.await();
						} catch (InterruptedException e) {
							tasksInterrupted.countDown();
						}
					});

			var caller = new Thread(() -> {
				try {
					VirtualThreads.runAll(tasks, 2);
				} catch (InterruptedException e) {
					callerInterrupted.set(true);
				} catch (ExecutionException e) {
					Thread.currentThread().interrupt();
				}
			});

			try {
				caller.start();
				assertTrue(tasksStarted.await(2, TimeUnit.SECONDS), "tasks should have started");
				caller.interrupt();
				caller.join(2000);
				assertTrue(callerInterrupted.get(), "caller should observe the InterruptedException");
				assertTrue(
						tasksInterrupted.await(500, TimeUnit.MILLISECONDS),
						"spawned virtual threads should be interrupted when the waiting caller is interrupted, "
								+ "but they were left running");
			} finally {
				blocker.countDown();
			}
		}
	}

	@Nested
	class ConcurrentFailuresTests {
		@Test
		@Timeout(10)
		void callAllShouldRetainConcurrentFailuresAsSuppressed() throws Exception {
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

		@Test
		@Timeout(10)
		void runAllShouldRetainConcurrentFailuresAsSuppressed() throws Exception {
			var secondaryStarted = new CountDownLatch(1);

			Runnable primary = () -> {
				try {
					secondaryStarted.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				throw new IllegalStateException("primary failure");
			};
			Runnable secondary = () -> {
				secondaryStarted.countDown();
				try {
					Thread.sleep(30_000);
				} catch (InterruptedException e) {
					throw new IllegalStateException("secondary failure");
				}
			};

			var thrown =
					assertThrows(ExecutionException.class, () -> VirtualThreads.runAll(List.of(primary, secondary), 2));

			assertEquals("primary failure", thrown.getCause().getMessage());
			assertTrue(
					Arrays.stream(thrown.getSuppressed())
							.anyMatch(s -> String.valueOf(s.getMessage()).contains("secondary failure")
									|| String.valueOf(s.getCause()).contains("secondary failure")),
					"failures of other in-flight tasks should be retained as suppressed exceptions, "
							+ "but were dropped: " + Arrays.toString(thrown.getSuppressed()));
		}
	}

	@Nested
	class FailFastStallTests {
		@Test
		@Timeout(30)
		void callAllShouldNotStallOnTasksIgnoringInterrupts() {
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

			assertFailFastNoStall(release, () -> VirtualThreads.callAll(List.of(failing, uncooperative), 2));
		}

		@Test
		@Timeout(30)
		void runAllShouldNotStallOnTasksIgnoringInterrupts() {
			var release = new CountDownLatch(1);
			var uncooperativeStarted = new CountDownLatch(1);

			Runnable failing = () -> {
				try {
					uncooperativeStarted.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				throw new RuntimeException("boom");
			};
			Runnable uncooperative = () -> {
				uncooperativeStarted.countDown();
				while (true) {
					try {
						release.await(15, TimeUnit.SECONDS);
						break;
					} catch (InterruptedException e) {
						// Simulates a task which does not respond to interruption.
					}
				}
			};

			assertFailFastNoStall(release, () -> VirtualThreads.runAll(List.of(failing, uncooperative), 2));
		}

		private static void assertFailFastNoStall(
				CountDownLatch release, org.junit.jupiter.api.function.Executable action) {
			var start = System.nanoTime();
			try {
				assertThrows(ExecutionException.class, action);
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

	@Nested
	class InFlightTaskLeakOnInputFailureTests {
		@Test
		@Timeout(10)
		void callAllInFlightTasksShouldBeCancelledWhenIteratorThrows() throws Exception {
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

			assertInFlightTaskCancelled(
					taskStarted,
					taskInterrupted,
					blocker,
					() -> VirtualThreads.callAll(faultyIterator(blockingTask), 2));
		}

		@Test
		@Timeout(10)
		void runAllInFlightTasksShouldBeCancelledWhenIteratorThrows() throws Exception {
			var taskStarted = new CountDownLatch(1);
			var taskInterrupted = new CountDownLatch(1);
			var blocker = new CountDownLatch(1);

			Runnable blockingTask = () -> {
				taskStarted.countDown();
				try {
					blocker.await();
				} catch (InterruptedException e) {
					taskInterrupted.countDown();
					Thread.currentThread().interrupt();
				}
			};

			assertInFlightTaskCancelled(
					taskStarted,
					taskInterrupted,
					blocker,
					() -> VirtualThreads.runAll(faultyIterator(blockingTask), 2));
		}

		private static <T> Iterator<T> faultyIterator(T task) {
			return new Iterator<>() {
				boolean yielded = false;

				@Override
				public boolean hasNext() {
					if (!yielded) return true;
					throw new IllegalStateException("input iteration failed");
				}

				@Override
				public T next() {
					yielded = true;
					return task;
				}
			};
		}

		private static void assertInFlightTaskCancelled(
				CountDownLatch taskStarted,
				CountDownLatch taskInterrupted,
				CountDownLatch blocker,
				org.junit.jupiter.api.function.Executable action)
				throws Exception {
			try {
				assertThrows(IllegalStateException.class, action);
				assertTrue(taskStarted.await(2, TimeUnit.SECONDS), "submitted task should have started");
				assertTrue(taskInterrupted.await(500, TimeUnit.MILLISECONDS));
			} finally {
				blocker.countDown();
			}
		}
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
