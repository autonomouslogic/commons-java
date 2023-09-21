package com.autonomouslogic.commons.updater.file;

import com.autonomouslogic.commons.updater.SimpleUpdater;
import com.autonomouslogic.commons.updater.UpdateTransformer;
import java.nio.file.Path;
import lombok.NonNull;

public class FileUpdater extends SimpleUpdater<Path, FileMeta, Path> {
	public FileUpdater(FileUpdateFetcher fetcher, FileUpdateChecker checker) {
		super(fetcher, checker, UpdateTransformer.identity());
	}

	public static FileUpdater create(@NonNull Path path) {
		return new FileUpdater(new FileUpdateFetcher(path), new FileUpdateChecker());
	}
}
