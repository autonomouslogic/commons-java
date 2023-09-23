package com.autonomouslogic.commons.updater.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.commons.updater.UpdateItem;
import io.reactivex.rxjava3.core.Flowable;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileUpdaterTest {
	Path file;
	FileUpdater updater;

	@BeforeEach
	@SneakyThrows
	void setup() {
		file = Files.createTempFile(FileUpdaterTest.class.getSimpleName(), "");
		updater = FileUpdater.create(file);
	}

	@Test
	void shouldDetectFileWhenItChanges() {
		var update = updateNow();
		assertEquals(1, update.size());
		update = updateNow();
		assertEquals(0, update.size());
		put("new content");
		update = updateNow();
		assertEquals(1, update.size());
	}

	private List<UpdateItem<Path, FileMeta>> updateNow() {
		return Flowable.fromPublisher(updater.updateNow()).toList().blockingGet();
	}

	@SneakyThrows
	void put(String content) {
		try (var out = new FileOutputStream(file.toFile())) {
			IOUtils.write(content, out, Charset.defaultCharset());
		}
	}
}
