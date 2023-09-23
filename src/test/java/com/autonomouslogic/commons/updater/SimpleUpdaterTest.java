package com.autonomouslogic.commons.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Flowable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class SimpleUpdaterTest {
	final Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
	final UpdateItem<String, Integer> item1 = UpdateItem.from("item-1", 1, clock);
	final UpdateItem<String, Integer> item2 = UpdateItem.from("item-2", 2, clock);
	UpdateFetcher<String, Integer> fetcher;
	UpdateChecker<String, Integer> checker;
	UpdateTransformer<String, Integer, String> transformer;
	SimpleUpdater<String, Integer, String> updater;
	InOrder inOrder;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setup() {
		fetcher = Mockito.mock(UpdateFetcher.class);
		checker = Mockito.mock(UpdateChecker.class);
		transformer = Mockito.mock(UpdateTransformer.class);

		updater = new SimpleUpdater<>(fetcher, checker, transformer);
		inOrder = Mockito.inOrder(fetcher, checker, transformer);

		when(transformer.transform(any())).thenAnswer(invocation -> {
			UpdateItem<String, Integer> item = invocation.getArgument(0);
			return item.getItem() + "-transformed";
		});
		when(checker.isNew(any(), any())).thenAnswer(invocation -> {
			UpdateItem<String, Integer> previous = invocation.getArgument(0);
			UpdateItem<String, Integer> current = invocation.getArgument(1);
			return previous == null
					|| previous.getUpdateMeta().getMeta()
							!= current.getUpdateMeta().getMeta();
		});
	}

	@Test
	void shouldDoInitialUpdate() {
		runUpdate(item1, null);
	}

	@Test
	void shouldDoSubsequentUpdate() {
		runUpdate(item1, null);
		runUpdate(item2, item1);
	}

	@Test
	void shouldSkipNonUpdates() {
		runUpdate(item1, null);
		runNonUpdate(item1);
	}

	void runUpdate(UpdateItem<String, Integer> item, UpdateItem<String, Integer> previous) {
		when(fetcher.fetchUpdate(any())).thenReturn(Flowable.just(item));

		var update = Flowable.fromPublisher(updater.updateNow()).blockingFirst();
		assertNotNull(update);
		assertEquals(item.getItem() + "-transformed", update.getItem());
		assertEquals(item.getUpdateMeta().getMeta(), update.getUpdateMeta().getMeta());
		assertEquals(Instant.EPOCH, update.getUpdateMeta().getLastUpdated());

		inOrder.verify(fetcher).fetchUpdate(same(previous));
		inOrder.verify(checker).isNew(same(previous), same(item));
		inOrder.verify(transformer).transform(same(item));
		inOrder.verifyNoMoreInteractions();
	}

	void runNonUpdate(UpdateItem<String, Integer> item) {
		when(fetcher.fetchUpdate(any())).thenReturn(Flowable.just(item));

		var update = Flowable.fromPublisher(updater.updateNow()).toList().blockingGet();
		assertEquals(0, update.size());

		inOrder.verify(fetcher).fetchUpdate(same(item));
		inOrder.verify(checker).isNew(same(item), same(item));
		inOrder.verifyNoMoreInteractions();
	}
}
