package com.autonomouslogic.commons.concurrent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
	public static <T> List<T> runAll(@NonNull Stream<Callable<T>> tasks, int concurrency)
			throws InterruptedException, ExecutionException {
		return null;
	}
}
