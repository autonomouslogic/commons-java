package com.autonomouslogic.commons.updater.file;

import com.autonomouslogic.commons.updater.UpdateFetcher;
import com.autonomouslogic.commons.updater.UpdateItem;
import com.autonomouslogic.commons.updater.UpdateMeta;
import io.reactivex.rxjava3.core.Maybe;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
public class FileUpdateFetcher implements UpdateFetcher<Path, FileMeta> {
	@NonNull
	private final Path path;

	@Override
	public Publisher<UpdateItem<Path, FileMeta>> fetchUpdate(UpdateItem<Path, FileMeta> lastUpdate) {
		return Maybe.defer(() -> {
					if (!Files.exists(path)) {
						return Maybe.empty();
					}
					if (Files.isDirectory(path)) {
						return Maybe.error(new RuntimeException("Path must be a file, directory found: " + path));
					}
					return Maybe.just(UpdateItem.of(
							path,
							UpdateMeta.from(
									new FileMeta(Files.getLastModifiedTime(path).toInstant()))));
				})
				.toFlowable();
	}
}
