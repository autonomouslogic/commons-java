package com.autonomouslogic.commons.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.NonNull;

public class VirtualThreads {
	/**
	 * Runs tasks concurrently on the provided  virtual thread executor.
	 * This is a blocking method.
	 *
	 * @param tasks the tasks to be executed
	 * @param concurrency the maximum concurrency to allow for executing tasks
	 * @return a list of results in the same order as the stream
	 */
	public static <T> List<T> _runAll(@NonNull Stream<Callable<T>> tasks, int concurrency)
			throws InterruptedException, ExecutionException {
		var taskList = tasks.toList();

		if (taskList.isEmpty()) {
			return List.of();
		}

		var executor = Executors.newVirtualThreadPerTaskExecutor();
		var results = new ArrayList<T>(taskList.size());
		var inFlightFutures = new ArrayList<Future<?>>();
		var taskIndex = 0;

		try {
			// Interleave submission and result collection for fail-fast behavior
			while (taskIndex < taskList.size() || !inFlightFutures.isEmpty()) {
				// Submit new tasks up to concurrency limit
				while (inFlightFutures.size() < concurrency && taskIndex < taskList.size()) {
					final var task = taskList.get(taskIndex);
					var future = executor.submit(() -> {
						try {
							return task.call();
						} catch (Exception e) {
							throw e;
						}
					});
					inFlightFutures.add(future);
					taskIndex++;
				}

				// Collect one result if we have in-flight tasks
				if (!inFlightFutures.isEmpty()) {
					try {
						@SuppressWarnings("unchecked")
						var f = (Future<T>) inFlightFutures.remove(0);
						results.add(f.get());
					} catch (ExecutionException e) {
						// Cancel all remaining futures on failure
						for (var future : inFlightFutures) {
							future.cancel(true);
						}
						throw e;
					}
				}
			}

			return results;
		} catch (InterruptedException e) {
			// Cancel all in-flight tasks on interrupt
			for (var future : inFlightFutures) {
				future.cancel(true);
			}
			Thread.currentThread().interrupt();
			throw e;
		} finally {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Returns results in submission order.
	 *
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 */
	public static <T> List<T> runAll(Stream<Callable<T>> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {

		if (maxConcurrency <= 0) {
			throw new IllegalArgumentException("maxConcurrency must be > 0");
		}

		Iterator<Callable<T>> iterator = tasks.iterator();

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

			CompletionService<Result<T>> completion = new ExecutorCompletionService<>(executor);

			List<T> results = new ArrayList<>();
			int nextIndex = 0;
			int inFlight = 0;

			// Store results by index to preserve ordering
			Map<Integer, T> completed = new HashMap<>();

			while (iterator.hasNext() || inFlight > 0) {

				// Fill capacity
				while (inFlight < maxConcurrency && iterator.hasNext()) {
					int index = nextIndex++;
					Callable<T> task = iterator.next();

					completion.submit(() -> {
						T value = task.call();
						return new Result<>(index, value);
					});

					inFlight++;
				}

				if (inFlight == 0) break;

				Future<Result<T>> finished;
				try {
					finished = completion.take();
				} catch (InterruptedException e) {
					executor.shutdownNow();
					Thread.currentThread().interrupt();
					throw e;
				}

				Result<T> result;
				try {
					result = finished.get();
				} catch (ExecutionException e) {
					executor.shutdownNow();
					throw e; // fail fast
				}

				inFlight--;

				completed.put(result.index, result.value);

				// Drain in-order results into output list
				while (completed.containsKey(results.size())) {
					results.add(completed.remove(results.size()));
				}
			}

			return results;
		}
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
