package com.autonomouslogic.commons.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Flowable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SimpleUpdaterTest {
	final Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
	UpdateFetcher<String, Integer> fetcher;
	UpdateChecker<String, Integer> checker;
	UpdateTransformer<String, Integer, String> transformer;
	SimpleUpdater<String, Integer, String> updater;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setup() {
		fetcher = Mockito.mock(UpdateFetcher.class);
		checker = Mockito.mock(UpdateChecker.class);
		transformer = Mockito.mock(UpdateTransformer.class);

		updater = SimpleUpdater.from(fetcher, checker, transformer);
	}

	@Test
	void shouldDoInitialUpdate() {
		final var firstItem = UpdateItem.from("item-1", 1, clock);
		when(fetcher.fetchUpdate(any())).thenReturn(Flowable.just(firstItem));
		when(checker.isNew(any(), any())).thenReturn(true);
		when(transformer.transform(any())).thenReturn("transformed-1");

		var firstUpdate = Flowable.fromPublisher(updater.updateNow()).blockingFirst();
		assertNotNull(firstUpdate);
		assertEquals("transformed-1", firstUpdate.getItem());
		assertEquals(1, firstUpdate.getUpdateMeta().getMeta());
		assertEquals(Instant.EPOCH, firstUpdate.getUpdateMeta().getLastUpdated());

		var inOrder = Mockito.inOrder(fetcher, checker, transformer);
		inOrder.verify(fetcher).fetchUpdate(isNull());
		inOrder.verify(checker).isNew(isNull(), same(firstItem));
		inOrder.verify(transformer).transform(same(firstItem));
		inOrder.verifyNoMoreInteractions();
	}
}
