package com.autonomouslogic.commons.updater.file;

import com.autonomouslogic.commons.updater.UpdateChecker;
import com.autonomouslogic.commons.updater.UpdateItem;
import java.nio.file.Path;

public class FileUpdateChecker implements UpdateChecker<Path, FileMeta> {
	@Override
	public boolean isNew(UpdateItem<Path, FileMeta> currentItem, UpdateItem<Path, FileMeta> newItem) {
		return currentItem == null
				|| !currentItem
						.getUpdateMeta()
						.getMeta()
						.getLastModified()
						.equals(newItem.getUpdateMeta().getMeta().getLastModified());
	}
}
