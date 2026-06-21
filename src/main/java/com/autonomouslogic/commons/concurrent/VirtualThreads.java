package com.autonomouslogic.commons.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;

public class VirtualThreads {
	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Returns results in submission order.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static <T> List<T> callAll(@NonNull Iterator<? extends Callable<T>> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		if (maxConcurrency <= 0) {
			throw new IllegalArgumentException("maxConcurrency must be > 0");
		}
		var executor = Executors.newVirtualThreadPerTaskExecutor();
		try {
			var completion = new ExecutorCompletionService<Result<T>>(executor);
			var results = new ArrayList<T>();
			int nextIndex = 0;
			int inFlight = 0;
			var completed = new HashMap<Integer, T>();
			while (tasks.hasNext() || inFlight > 0) {
				while (inFlight < maxConcurrency && tasks.hasNext()) {
					int index = nextIndex++;
					var task = tasks.next();
					completion.submit(() -> {
						T value = task.call();
						return new Result<>(index, value);
					});
					inFlight++;
				}
				if (inFlight == 0) {
					break;
				}
				try {
					var finished = completion.take();
					try {
						var result = finished.get();
						inFlight--;
						completed.put(result.index, result.value);
						while (completed.containsKey(results.size())) {
							results.add(completed.remove(results.size()));
						}
					} catch (ExecutionException e) {
						executor.shutdownNow();

						try {
							executor.awaitTermination(5, TimeUnit.SECONDS);
						} catch (InterruptedException interrupted) {
							Thread.currentThread().interrupt();
							e.addSuppressed(interrupted);
						}

						throw e;
					}
				} catch (InterruptedException e) {
					executor.shutdownNow();

					try {
						executor.awaitTermination(5, TimeUnit.SECONDS);
					} catch (InterruptedException suppressed) {
						e.addSuppressed(suppressed);
					}

					Thread.currentThread().interrupt();
					throw e;
				}
			}
			return results;
		} finally {
			executor.shutdown();
		}
	}

	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Returns results in submission order.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static <T> List<T> callAll(@NonNull Iterable<? extends Callable<T>> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		return callAll(tasks.iterator(), maxConcurrency);
	}

	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Returns results in submission order.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static <T> List<T> callAll(@NonNull Stream<Callable<T>> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		return callAll(tasks.iterator(), maxConcurrency);
	}

	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static void runAll(@NonNull Iterator<? extends Runnable> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		if (maxConcurrency <= 0) {
			throw new IllegalArgumentException("maxConcurrency must be > 0");
		}
		var executor = Executors.newVirtualThreadPerTaskExecutor();
		try {
			var completion = new ExecutorCompletionService<Void>(executor);
			int inFlight = 0;
			while (tasks.hasNext() || inFlight > 0) {
				while (inFlight < maxConcurrency && tasks.hasNext()) {
					var task = tasks.next();
					completion.submit(() -> {
						task.run();
						return null;
					});
					inFlight++;
				}
				if (inFlight == 0) {
					break;
				}
				try {
					var finished = completion.take();
					try {
						finished.get();
					} catch (ExecutionException e) {
						executor.shutdownNow();

						try {
							executor.awaitTermination(5, TimeUnit.SECONDS);
						} catch (InterruptedException interrupted) {
							Thread.currentThread().interrupt();
							e.addSuppressed(interrupted);
						}

						throw e;
					}
				} catch (InterruptedException e) {
					executor.shutdownNow();

					try {
						executor.awaitTermination(5, TimeUnit.SECONDS);
					} catch (InterruptedException suppressed) {
						e.addSuppressed(suppressed);
					}

					Thread.currentThread().interrupt();
					throw e;
				}
				inFlight--;
			}
		} finally {
			executor.shutdown();
		}
	}

	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static void runAll(@NonNull Iterable<? extends Runnable> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		runAll(tasks.iterator(), maxConcurrency);
	}

	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static void runAll(@NonNull Stream<Runnable> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		runAll(tasks.iterator(), maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Results are returned in submission order.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T, R> List<R> callAll(@NonNull Iterator<T> inputs, @NonNull Function<T, R> fn, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		var tasks = new ArrayList<Callable<R>>();
		while (inputs.hasNext()) {
			var input = inputs.next();
			tasks.add(() -> fn.apply(input));
		}
		return callAll(tasks.iterator(), maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Results are returned in submission order.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T, R> List<R> callAll(@NonNull Iterable<T> inputs, @NonNull Function<T, R> fn, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		return callAll(inputs.iterator(), fn, maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Results are returned in submission order.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T, R> List<R> callAll(@NonNull Stream<T> inputs, @NonNull Function<T, R> fn, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		return callAll(inputs.map(input -> (Callable<R>) () -> fn.apply(input)).iterator(), maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T> void runAll(@NonNull Iterator<T> inputs, @NonNull Consumer<T> action, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		var tasks = new ArrayList<Runnable>();
		while (inputs.hasNext()) {
			var input = inputs.next();
			tasks.add(() -> action.accept(input));
		}
		runAll(tasks.iterator(), maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T> void runAll(@NonNull Iterable<T> inputs, @NonNull Consumer<T> action, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		runAll(inputs.iterator(), action, maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T> void runAll(@NonNull Stream<T> inputs, @NonNull Consumer<T> action, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		runAll(inputs.map(input -> (Runnable) () -> action.accept(input)).iterator(), maxConcurrency);
	}

	/**
	 * Checks if the current thread is a virtual thread.
	 *
	 * @return true if the current thread is a virtual thread, false otherwise
	 */
	public static boolean isVirtual() {
		return Thread.currentThread().isVirtual();
	}

	/**
	 * Asserts that the current thread is a virtual thread.
	 *
	 * @throws NotVirtualThreadException if the current thread is not a virtual thread
	 */
	public static void checkIsVirtual() {
		if (!isVirtual()) {
			throw new NotVirtualThreadException();
		}
	}

	/**
	 * Executes a task on a virtual thread if the current thread is not virtual.
	 * If the current thread is already virtual, the task is executed immediately.
	 *
	 * @param task the task to execute
	 * @throws InterruptedException if the current thread is interrupted while waiting for the task to complete
	 */
	public static void onVirtualThread(@NonNull Runnable task) throws InterruptedException {
		if (isVirtual()) {
			task.run();
		} else {
			var thread = Thread.ofVirtual().start(task);
			thread.join();
		}
	}

	/**
	 * Executes a callable task on a virtual thread if the current thread is not virtual.
	 * If the current thread is already virtual, the task is executed immediately and the result is returned.
	 *
	 * @param <T> the type of the result
	 * @param task the callable task to execute
	 * @return the result of the task
	 * @throws InterruptedException if the current thread is interrupted while waiting for the task to complete
	 * @throws Exception if the task throws an exception
	 */
	public static <T> T onVirtualThread(@NonNull Callable<T> task) throws InterruptedException, Exception {
		if (isVirtual()) {
			return task.call();
		} else {
			var result = new AtomicReference<T>();
			var exception = new AtomicReference<Exception>();
			var thread = Thread.ofVirtual().start(() -> {
				try {
					result.set(task.call());
				} catch (Exception e) {
					exception.set(e);
				}
			});
			thread.join();
			if (exception.get() != null) {
				throw exception.get();
			}
			return result.get();
		}
	}

	private static final class Result<T> {
		private final int index;
		private final T value;

		Result(int index, T value) {
			this.index = index;
			this.value = value;
		}
	}
}
