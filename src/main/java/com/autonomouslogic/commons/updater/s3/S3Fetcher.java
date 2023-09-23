package com.autonomouslogic.commons.updater.s3;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.commons.updater.UpdateFetcher;
import com.autonomouslogic.commons.updater.UpdateItem;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@RequiredArgsConstructor
public class S3Fetcher implements UpdateFetcher<Path, S3Meta> {
	@NonNull
	private final String bucket;

	@NonNull
	private final String key;

	@NonNull
	private final S3AsyncClient client;

	@Override
	public Publisher<UpdateItem<Path, S3Meta>> fetchUpdate(UpdateItem<Path, S3Meta> lastUpdate) {
		return Maybe.defer(() -> {
					var file = Files.createTempFile(S3Fetcher.class.getSimpleName() + "-", ".tmp");
					file.toFile().deleteOnExit();
					if (lastUpdate == null) {
						return getObject(file);
					} else {
						return headObject(file, lastUpdate).flatMap(ignore -> getObject(file));
					}
				})
				.subscribeOn(Schedulers.io())
				.observeOn(Schedulers.computation())
				.toFlowable();
	}

	private Maybe<UpdateItem<Path, S3Meta>> headObject(
			@NonNull Path file, @NonNull UpdateItem<Path, S3Meta> lastUpdate) {
		return Rx3Util.toMaybe(
						client.headObject(builder -> builder.bucket(bucket).key(key)))
				.flatMap(response -> handleResponse(response, file, lastUpdate));
	}

	private Maybe<UpdateItem<Path, S3Meta>> getObject(@NonNull Path file) {
		return Rx3Util.toMaybe(
						client.getObject(builder -> builder.bucket(bucket).key(key), file))
				.flatMap(response -> handleResponse(response, file));
	}

	@SneakyThrows
	private static Maybe<UpdateItem<Path, S3Meta>> handleResponse(
			@NonNull HeadObjectResponse response, @NonNull Path file, @NonNull UpdateItem<Path, S3Meta> lastUpdate) {
		return Maybe.just(createUpdate(response.versionId(), response.lastModified(), file))
				.flatMap(update -> Maybe.defer(() -> {
					if (update.getUpdateMeta()
							.getMeta()
							.getVersionId()
							.equals(lastUpdate.getUpdateMeta().getMeta().getVersionId())) {
						return Maybe.empty();
					} else {
						return Maybe.just(update);
					}
				}));
	}

	@SneakyThrows
	private static Maybe<UpdateItem<Path, S3Meta>> handleResponse(
			@NonNull GetObjectResponse response, @NonNull Path file) {
		return Maybe.just(createUpdate(response.versionId(), response.lastModified(), file));
	}

	private static UpdateItem<Path, S3Meta> createUpdate(
			@NonNull String versionId, @NonNull Instant lastModified, @NonNull Path file) {
		return UpdateItem.from(file, new S3Meta(versionId, lastModified));
	}

	public static S3Fetcher create(@NonNull String bucket, @NonNull String key) {
		return new S3Fetcher(bucket, key, S3AsyncClient.create());
	}
}
