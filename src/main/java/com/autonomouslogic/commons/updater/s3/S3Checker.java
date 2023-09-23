package com.autonomouslogic.commons.updater.s3;

import com.autonomouslogic.commons.updater.UpdateChecker;
import com.autonomouslogic.commons.updater.UpdateItem;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3Checker implements UpdateChecker<Path, S3Meta> {
	@Override
	public boolean isNew(UpdateItem<Path, S3Meta> currentItem, UpdateItem<Path, S3Meta> newItem) {
		if (internalIsNew(currentItem, newItem)) {
			deleteFile(currentItem);
			return true;
		}
		return false;
	}

	private boolean internalIsNew(UpdateItem<Path, S3Meta> currentItem, UpdateItem<Path, S3Meta> newItem) {
		if (currentItem == null) {
			return true;
		}
		return currentItem
				.getUpdateMeta()
				.getMeta()
				.getLastModified()
				.isBefore(newItem.getUpdateMeta().getMeta().getLastModified());
	}

	private void deleteFile(UpdateItem<Path, S3Meta> currentItem) {
		if (currentItem == null) {
			return;
		}
		var file = currentItem.getItem().toFile();
		if (file.exists()) {
			try {
				if (!file.delete()) {
					log.debug("Failed to delete temporary file {}", file);
				}
			} catch (Exception e) {
				log.debug("Failed to delete temporary file {}", file, e);
			}
		}
	}
}
