package com.autonomouslogic.commons.updater.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.reactivex.rxjava3.core.Maybe;
import java.io.FileReader;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
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

		assertNull(server.takeRequest(0, TimeUnit.MILLISECONDS));
	}

	@Test
	@SneakyThrows
	void shouldHandleConditionalFetch() {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Last-Modified", "Thu, 01 Oct 2015 07:28:00 GMT")
				.addHeader("ETag", "\"33a64df551425fcc55e4d42a148795d9f25f89d4\"")
				.setBody("Body line\n"));
		server.enqueue(new MockResponse().setResponseCode(304));

		var item = Maybe.fromPublisher(updater.updateNow()).blockingGet();
		try (var reader = new FileReader(item.getItem().toFile())) {
			assertEquals("Body line\n", IOUtils.toString(reader));
		}

		var update = Maybe.fromPublisher(updater.updateNow()).blockingGet();
		assertNull(update);

		var request1 = server.takeRequest();
		assertEquals("GET", request1.getMethod());
		assertNull(request1.getHeader("If-Modified-Since"));
		assertNull(request1.getHeader("If-None-Match"));

		var request2 = server.takeRequest();
		assertEquals("GET", request2.getMethod());
		assertEquals("Thu, 01 Oct 2015 07:28:00 GMT", request2.getHeader("If-Modified-Since"));
		assertEquals("\"33a64df551425fcc55e4d42a148795d9f25f89d4\"", request2.getHeader("If-None-Match"));

		assertNull(server.takeRequest(0, TimeUnit.MILLISECONDS));
	}

	@Test
	@SneakyThrows
	void shouldHandleUpdateInConditionalFetch() {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Last-Modified", "Thu, 01 Oct 2015 07:28:00 GMT")
				.addHeader("ETag", "\"33a64df551425fcc55e4d42a148795d9f25f89d4\"")
				.setBody("Body 1 line\n"));
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.addHeader("Last-Modified", "Thu, 01 Oct 2020 07:28:00 GMT")
				.addHeader("ETag", "\"23d9523fb158d6d30c9aab45f1333e7fae53849c\"")
				.setBody("Body 2 line\n"));

		var item1 = Maybe.fromPublisher(updater.updateNow()).blockingGet();
		try (var reader = new FileReader(item1.getItem().toFile())) {
			assertEquals("Body 1 line\n", IOUtils.toString(reader));
		}

		var item2 = Maybe.fromPublisher(updater.updateNow()).blockingGet();
		try (var reader = new FileReader(item2.getItem().toFile())) {
			assertEquals("Body 2 line\n", IOUtils.toString(reader));
		}
		assertFalse(item1.getItem().toFile().exists());

		var request1 = server.takeRequest();
		assertEquals("GET", request1.getMethod());
		assertNull(request1.getHeader("If-Modified-Since"));
		assertNull(request1.getHeader("If-None-Match"));

		var request2 = server.takeRequest();
		assertEquals("GET", request2.getMethod());
		assertEquals("Thu, 01 Oct 2015 07:28:00 GMT", request2.getHeader("If-Modified-Since"));
		assertEquals("\"33a64df551425fcc55e4d42a148795d9f25f89d4\"", request2.getHeader("If-None-Match"));

		assertNull(server.takeRequest(0, TimeUnit.MILLISECONDS));
	}
}
