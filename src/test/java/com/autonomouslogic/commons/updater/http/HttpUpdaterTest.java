package com.autonomouslogic.commons.updater.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.reactivex.rxjava3.core.Maybe;
import java.io.FileReader;
import java.net.URI;
import java.time.Instant;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
public class HttpUpdaterTest {
	MockWebServer server;
	HttpUpdater updater;

	@BeforeEach
	@SneakyThrows
	void setup() {
		server = new MockWebServer();
		server.start(0);
		updater = HttpUpdater.create(URI.create("http://localhost:" + server.getPort()));
	}

	@AfterEach
	@SneakyThrows
	void after() {
		server.close();
	}

	@Test
	@SneakyThrows
	void shouldFetchInitial() {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Last-Modified", "Wed, 21 Oct 2015 07:28:00 GMT")
				.addHeader("ETag", "\"33a64df551425fcc55e4d42a148795d9f25f89d4\"")
				.setBody("Body line\n"));

		var item = Maybe.fromPublisher(updater.updateNow()).blockingGet();
		assertNotNull(item);
		assertEquals(
				Instant.parse("2015-10-21T07:28:00Z"),
				item.getUpdateMeta().getMeta().getLastModified());
		assertEquals(
				"\"33a64df551425fcc55e4d42a148795d9f25f89d4\"",
				item.getUpdateMeta().getMeta().getEtag());
		try (var reader = new FileReader(item.getItem().toFile())) {
			assertEquals("Body line\n", IOUtils.toString(reader));
		}

		var request = server.takeRequest();
		assertEquals("GET", request.getMethod());
		assertNull(request.getHeader("If-Modified-Since"));
		assertNull(request.getHeader("If-None-Match"));
	}
}
