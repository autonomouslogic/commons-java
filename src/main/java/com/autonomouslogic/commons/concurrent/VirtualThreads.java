package com.autonomouslogic.commons.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;

public class VirtualThreads {
	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Returns results in submission order.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static <T> List<T> callAll(@NonNull Iterable<? extends Callable<T>> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		if (maxConcurrency <= 0) {
			throw new IllegalArgumentException("maxConcurrency must be > 0");
		}
		var executor = Executors.newVirtualThreadPerTaskExecutor();
		try {
			var iterator = tasks.iterator();
			var completion = new ExecutorCompletionService<Result<T>>(executor);
			var results = new ArrayList<T>();
			int nextIndex = 0;
			int inFlight = 0;
			var completed = new HashMap<Integer, T>();
			while (iterator.hasNext() || inFlight > 0) {
				while (inFlight < maxConcurrency && iterator.hasNext()) {
					int index = nextIndex++;
					var task = iterator.next();
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
	public static <T> List<T> callAll(@NonNull Stream<Callable<T>> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		return callAll(tasks.collect(Collectors.toList()), maxConcurrency);
	}

	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static void runAll(@NonNull Iterable<? extends Runnable> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		if (maxConcurrency <= 0) {
			throw new IllegalArgumentException("maxConcurrency must be > 0");
		}
		var executor = Executors.newVirtualThreadPerTaskExecutor();
		try {
			var iterator = tasks.iterator();
			var completion = new ExecutorCompletionService<Void>(executor);
			int inFlight = 0;
			while (iterator.hasNext() || inFlight > 0) {
				while (inFlight < maxConcurrency && iterator.hasNext()) {
					var task = iterator.next();
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
	public static void runAll(@NonNull Stream<Runnable> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		runAll(tasks.collect(Collectors.toList()), maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Results are returned in submission order.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T, R> List<R> callAll(@NonNull Iterable<T> inputs, @NonNull Function<T, R> fn, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		var tasks = new ArrayList<Callable<R>>();
		for (var input : inputs) {
			tasks.add(() -> fn.apply(input));
		}
		return callAll(tasks, maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Results are returned in submission order.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T, R> List<R> callAll(@NonNull Stream<T> inputs, @NonNull Function<T, R> fn, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		return callAll(inputs.collect(Collectors.toList()), fn, maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T> void runAll(@NonNull Iterable<T> inputs, @NonNull Consumer<T> action, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		var tasks = new ArrayList<Runnable>();
		for (var input : inputs) {
			tasks.add(() -> action.accept(input));
		}
		runAll(tasks, maxConcurrency);
	}

	/**
	 * Executes an action on each input with bounded concurrency.
	 * Fail-fast: first action failure cancels remaining actions and propagates.
	 * This method is blocking.
	 */
	public static <T> void runAll(@NonNull Stream<T> inputs, @NonNull Consumer<T> action, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		runAll(inputs.collect(Collectors.toList()), action, maxConcurrency);
	}

	private static class Result<T> {
		final int index;
		final T value;

		Result(int index, T value) {
			this.index = index;
			this.value = value;
		}
	}
}
