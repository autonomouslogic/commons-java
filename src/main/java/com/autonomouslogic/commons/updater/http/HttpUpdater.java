package com.autonomouslogic.commons.updater.http;

import com.autonomouslogic.commons.updater.SimpleUpdater;
import com.autonomouslogic.commons.updater.UpdateTransformer;
import java.net.URI;
import java.nio.file.Path;

public class HttpUpdater extends SimpleUpdater<Path, HttpMeta, Path> {
	public HttpUpdater(HttpFetcher fetcher, HttpChecker checker) {
		super(fetcher, checker, UpdateTransformer.identity());
	}

	public static HttpUpdater create(URI uri) {
		return new HttpUpdater(HttpFetcher.create(uri), new HttpChecker());
	}
}
