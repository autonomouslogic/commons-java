package com.autonomouslogic.commons.rxjava3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Rx3Util_OrderedMergeTest {
	TestSubscriber<Integer> testSubscriber;

	@BeforeEach
	void setup() {
		testSubscriber = new TestSubscriber<>();
	}

	@Test
	@SneakyThrows
	void shouldMergeShortStreams() {
		Rx3Util.orderedMerge(Comparator.naturalOrder(), Flowable.just(1), Flowable.just(3), Flowable.just(2))
				.subscribe(testSubscriber);
		testSubscriber.await().assertComplete().assertResult(1, 2, 3).assertNoErrors();
	}

	@Test
	@SneakyThrows
	void shouldMergeLongStreams() {
		Rx3Util.orderedMerge(Comparator.naturalOrder(), Flowable.just(1, 3, 4), Flowable.just(2, 5, 6))
				.subscribe(testSubscriber);
		testSubscriber.await().assertComplete().assertResult(1, 2, 3, 4, 5, 6).assertNoErrors();
	}

	@Test
	@SneakyThrows
	void shouldMergeStreamsOnUnequalLength() {
		Rx3Util.orderedMerge(Comparator.naturalOrder(), Flowable.just(2), Flowable.just(1, 3))
				.subscribe(testSubscriber);
		testSubscriber.await().assertComplete().assertResult(1, 2, 3).assertNoErrors();
	}

	@Test
	@SneakyThrows
	void shouldMergeOverlappingStreams() {
		Rx3Util.orderedMerge(Comparator.naturalOrder(), Flowable.just(2, 3), Flowable.just(1, 2))
				.subscribe(testSubscriber);
		testSubscriber.await().assertComplete().assertResult(1, 2, 2, 3).assertNoErrors();
	}

	@Test
	@SneakyThrows
	void shouldMergeEmptyStreams() {
		Rx3Util.orderedMerge(Comparator.naturalOrder(), Flowable.empty(), Flowable.just(1, 2))
				.subscribe(testSubscriber);
		testSubscriber.await().assertComplete().assertResult(1, 2).assertNoErrors();
	}

	@Test
	@SneakyThrows
	void shouldPropagateImmediateErrors() {
		var e = new RuntimeException("test");
		Rx3Util.orderedMerge(Comparator.naturalOrder(), Flowable.just(1), Flowable.error(e))
				.subscribe(testSubscriber);
		testSubscriber.await().assertError(e).assertNotComplete();
	}

	@Test
	@SneakyThrows
	void shouldPropagateLaterErrors() {
		var e = new RuntimeException("test");
		Rx3Util.orderedMerge(
						Comparator.naturalOrder(),
						Flowable.concat(Flowable.just(2), Flowable.error(e)),
						Flowable.just(1))
				.subscribe(testSubscriber);
		testSubscriber.await().assertError(e).assertNotComplete();
	}

	@Test
	@SneakyThrows
	void shouldStopEarly() {
		var inspector1 = new TestSubscriber<Integer>();
		var inspector2 = new TestSubscriber<Integer>();
		Flowable.fromPublisher(Rx3Util.orderedMerge(
						Comparator.naturalOrder(),
						Flowable.range(0, 1000).doOnNext(inspector1::onNext),
						Flowable.range(0, 1000).doOnNext(inspector2::onNext)))
				.take(10)
				.subscribe(testSubscriber);
		testSubscriber.await().assertValueCount(10).assertComplete();
		assertTrue(inspector1.values().size() < 1000, "" + inspector1.values().size());
		assertTrue(inspector2.values().size() < 1000, "" + inspector2.values().size());
	}

	@Test
	@SneakyThrows
	void shouldStopEarlyOnCancel() {
		var inspector1 = new TestSubscriber<Integer>();
		var inspector2 = new TestSubscriber<Integer>();
		Flowable.fromPublisher(Rx3Util.orderedMerge(
						Comparator.naturalOrder(),
						Flowable.range(0, 1000).doOnNext(inspector1::onNext),
						Flowable.range(0, 1000).doOnNext(inspector2::onNext)))
				.subscribe(testSubscriber);
		testSubscriber.awaitCount(10).cancel();
		System.out.println("values: " + testSubscriber.values());
		assertTrue(inspector1.values().size() < 1000, "" + inspector1.values().size());
		assertTrue(inspector2.values().size() < 1000, "" + inspector2.values().size());
	}

	@Test
	@SneakyThrows
	void shouldMergeDelayedStreams() {
		Rx3Util.orderedMerge(Comparator.naturalOrder(), delayedFlow(5, false), delayedFlow(100, false))
				.subscribe(testSubscriber);
		testSubscriber.await();
		System.out.println("values: " + testSubscriber.values());
		testSubscriber.assertValues(0, 0, 1, 1, 2, 2, 3, 3, 4, 4).assertComplete();
	}

	@Test
	@SneakyThrows
	void shouldMergeDelayedStreamsWithErrors() {
		Rx3Util.orderedMerge(Comparator.naturalOrder(), delayedFlow(5, false), delayedFlow(5, true), Flowable.just(1))
				.subscribe(testSubscriber);
		testSubscriber.await();
		System.out.println("values: " + testSubscriber.values());
		testSubscriber.assertError(RuntimeException.class).assertNotComplete();
	}

	private Flowable<Integer> delayedFlow(int interval, boolean error) {
		var flows = new ArrayList<Flowable<Integer>>();
		for (int i = 0; i < 5; i++) {
			if (error && i == 3) {
				flows.add(Flowable.error(new RuntimeException("test")));
			} else {
				flows.add(Flowable.just(i).delay(interval, TimeUnit.MILLISECONDS));
			}
		}
		return Flowable.concat(flows);
	}
}
