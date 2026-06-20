package com.autonomouslogic.commons.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import lombok.NonNull;

public class VirtualThreads {
	/**
	 * Executes tasks on virtual threads with bounded concurrency.
	 * Returns results in submission order.
	 * Fail-fast: first task failure cancels remaining tasks and propagates.
	 * This method is blocking.
	 */
	public static <T> List<T> runAll(@NonNull Stream<Callable<T>> tasks, int maxConcurrency)
			throws InterruptedException, ExecutionException {
		if (maxConcurrency <= 0) {
			throw new IllegalArgumentException("maxConcurrency must be > 0");
		}
		var iterator = tasks.iterator();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
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
				var finished = completion.take();
				var result = finished.get();
				inFlight--;
				completed.put(result.index, result.value);
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
