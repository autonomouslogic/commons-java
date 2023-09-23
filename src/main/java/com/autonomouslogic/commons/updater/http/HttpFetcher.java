package com.autonomouslogic.commons.updater.http;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.commons.updater.UpdateFetcher;
import com.autonomouslogic.commons.updater.UpdateItem;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
public class HttpFetcher implements UpdateFetcher<Path, HttpMeta> {
	private static final ZoneId GMT_ZONE = ZoneId.of("GMT");
	private static final DateTimeFormatter HTTP_DATE_TIME = DateTimeFormatter.ofPattern(
					"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
			.withZone(GMT_ZONE);

	@NonNull
	private final URI uri;

	@NonNull
	private final HttpClient client;

	@NonNull
	@Setter
	private Consumer<HttpRequest.Builder> requestBuilder = builder -> {};

	@Setter
	private boolean useIfNoneMatch = true;

	@Setter
	private boolean useIfModifiedSince = true;

	@Override
	public Publisher<UpdateItem<Path, HttpMeta>> fetchUpdate(UpdateItem<Path, HttpMeta> lastUpdate) {
		return Maybe.defer(() -> {
					var request = createRequest(lastUpdate);
					var file = Files.createTempFile(HttpFetcher.class.getSimpleName() + "-", ".tmp");
					file.toFile().deleteOnExit();
					var bodyHandler = HttpResponse.BodyHandlers.ofFile(file);
					return Rx3Util.toMaybe(client.sendAsync(request, bodyHandler))
							.observeOn(Schedulers.computation())
							.flatMap((HttpResponse<Path> response) -> handleResponse(response, file));
				})
				.subscribeOn(Schedulers.io())
				.toFlowable();
	}

	@SneakyThrows
	private static Maybe<UpdateItem<Path, HttpMeta>> handleResponse(HttpResponse<Path> response, Path file) {
		var status = response.statusCode();
		if (status == 304) {
			return Maybe.empty();
		}
		if (status == 200 || status == 204) {
			return Maybe.just(createUpdate(response, file));
		}
		throw new IOException("Unexpected status code " + status);
	}

	private static UpdateItem<Path, HttpMeta> createUpdate(HttpResponse<Path> response, Path file) {
		var etag = getHeader(response, "ETag").orElse(null);
		var lastModified = getHeader(response, "Last-Modified")
				.map(s -> HTTP_DATE_TIME.parse(s, ZonedDateTime::from).toInstant())
				.orElse(null);
		return UpdateItem.from(file, new HttpMeta(lastModified, etag));
	}

	private HttpRequest createRequest(UpdateItem<Path, HttpMeta> lastUpdate) {
		var builder = HttpRequest.newBuilder().uri(uri).GET();
		if (lastUpdate != null) {
			var meta = lastUpdate.getUpdateMeta().getMeta();
			if (useIfNoneMatch && meta.getEtag() != null) {
				builder.header("If-None-Match", meta.getEtag());
			}
			if (useIfModifiedSince && meta.getLastModified() != null) {
				builder.header(
						"If-Modified-Since",
						HTTP_DATE_TIME.format(meta.getLastModified().atZone(GMT_ZONE)));
			}
		}
		requestBuilder.accept(builder);
		return builder.build();
	}

	private static Optional<String> getHeader(HttpResponse<?> response, String name) {
		return response.headers().firstValue(name);
	}

	public static HttpFetcher create(@NonNull URI uri) {
		return new HttpFetcher(uri, defaultHttpClientBuilder().build());
	}

	public static HttpClient.Builder defaultHttpClientBuilder() {
		return HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(20));
	}
}
