package com.autonomouslogic.commons.updater.http;

import java.net.URI;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	void test() {}
}
