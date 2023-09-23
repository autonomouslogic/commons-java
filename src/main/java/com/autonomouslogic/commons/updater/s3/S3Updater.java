package com.autonomouslogic.commons.updater.s3;

import com.autonomouslogic.commons.updater.SimpleUpdater;
import com.autonomouslogic.commons.updater.UpdateTransformer;
import java.nio.file.Path;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3Updater extends SimpleUpdater<Path, S3Meta, Path> {
	public S3Updater(S3Fetcher fetcher, S3Checker checker) {
		super(fetcher, checker, UpdateTransformer.identity());
	}

	public static S3Updater create(String bucket, String key, S3AsyncClient client) {
		return new S3Updater(new S3Fetcher(bucket, key, client), new S3Checker());
	}
}
