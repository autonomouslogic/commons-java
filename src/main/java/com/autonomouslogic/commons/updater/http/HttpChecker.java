package com.autonomouslogic.commons.updater.http;

import com.autonomouslogic.commons.updater.UpdateChecker;
import com.autonomouslogic.commons.updater.UpdateItem;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpChecker implements UpdateChecker<Path, HttpMeta> {
	@Override
	public boolean isNew(UpdateItem<Path, HttpMeta> currentItem, UpdateItem<Path, HttpMeta> newItem) {
		if (internalIsNew(currentItem, newItem)) {
			deleteFile(currentItem);
			return true;
		}
		return false;
	}

	private boolean internalIsNew(UpdateItem<Path, HttpMeta> currentItem, UpdateItem<Path, HttpMeta> newItem) {
		if (currentItem == null) {
			return true;
		}
		var currentMeta = currentItem.getUpdateMeta().getMeta();
		var newMeta = newItem.getUpdateMeta().getMeta();
		if (currentMeta.getLastModified() != null && newMeta.getLastModified() != null) {
			if (currentMeta.getLastModified().isBefore(newMeta.getLastModified())) {
				return true;
			}
		}
		if (currentMeta.getEtag() != null && newMeta.getEtag() != null) {
			if (!currentMeta.getEtag().equals(newMeta.getEtag())) {
				return true;
			}
		}

		return false;
	}

	private void deleteFile(UpdateItem<Path, HttpMeta> currentItem) {
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
