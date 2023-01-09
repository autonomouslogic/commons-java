package com.autonomouslogic.commons.rxjava3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Comparator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Rx3Util_OrderedMergeTest {
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
				.take(100)
				.subscribe(testSubscriber);
		testSubscriber.await().assertValueCount(100).assertComplete();
		assertTrue(inspector1.values().size() < 200, "" + inspector1.values().size());
		assertTrue(inspector2.values().size() < 200, "" + inspector2.values().size());
	}
}
