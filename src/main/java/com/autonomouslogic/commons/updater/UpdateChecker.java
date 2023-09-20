package com.autonomouslogic.commons.updater;

public interface UpdateChecker<T, M> {
	boolean isNew(UpdateItem<T, M> currentItem, UpdateItem<T, M> newItem);
}
