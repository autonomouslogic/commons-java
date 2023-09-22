package com.autonomouslogic.commons.updater.http;

import com.autonomouslogic.commons.updater.UpdateChecker;
import com.autonomouslogic.commons.updater.UpdateItem;
import java.nio.file.Path;

public class HttpChecker implements UpdateChecker<Path, HttpMeta> {
	@Override
	public boolean isNew(UpdateItem<Path, HttpMeta> currentItem, UpdateItem<Path, HttpMeta> newItem) {
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
}
