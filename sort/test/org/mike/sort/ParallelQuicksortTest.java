package org.mike.sort;

import java.util.List;

import org.junit.Test;

public class ParallelQuicksortTest {

	@Test
	public void testSort() {
		int n = 1000;
		int minPartition = 100;
		testSort(1, minPartition, n);
		testSort(2, minPartition, n);
		testSort(3, minPartition, n);
		testSort(4, minPartition, n);
	}

	public void testSort(int P, int minPartition, int n) {
		List<Integer> list = Harness.createList(n);
		ParallelQuicksort<Integer> sorter = new ParallelQuicksort<Integer>(P, minPartition);
		sorter.parentSort(list);
		Harness.verify(list);
		System.out.println("P["+P+"] sort ok");
	}

}
