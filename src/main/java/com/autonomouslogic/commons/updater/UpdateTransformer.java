package com.autonomouslogic.commons.updater;

public interface UpdateTransformer<T, M, R> {
	R transform(UpdateItem<T, M> item);

	static <T, M> UpdateTransformer<T, M, T> identity() {
		return UpdateItem::getItem;
	}
}
