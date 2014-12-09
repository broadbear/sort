package org.mike.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestScript {

	public static void main(String[] args) {
		int n = 20000000;
		int iterations = 5;
		SortStrategy<Integer> psrs = new SortStrategy<Integer>() {
			public List<Integer> sort(List<Integer> unsorted) {
				System.out.println("sort start");
				List<Integer> sorted = PSRSSort.sort(8, unsorted, false);
				System.out.println("sort end");
				return sorted;
			}
		};

		SortStrategy<Integer> std = new SortStrategy<Integer>() {
			public List<Integer> sort(List<Integer> unsorted) {
				Collections.sort(unsorted);
				return unsorted;
			}
		};

		SortStrategy<Integer> pqsort = new SortStrategy<Integer>() {
			public List<Integer> sort(List<Integer> unsorted) {
				ParallelQuicksort.sort(4, 1000, unsorted);
				return unsorted;
			}
		};
		
		SortStrategy<Integer> qsort = new SortStrategy<Integer>() {
			public List<Integer> sort(List<Integer> unsorted) {
				SequentialSort.quicksort(unsorted, 0, unsorted.size() - 1);
				return unsorted;
			}
		};

		SortStrategy<Integer> qsort3 = new SortStrategy<Integer>() {
			public List<Integer> sort(List<Integer> unsorted) {
				SequentialSort.quicksort3(unsorted, 0, unsorted.size() - 1);
				return unsorted;
			}
		};

//		testSorts("pquicksort", n, pqsort, iterations);		
		testSorts("psrs", n, psrs, iterations);
//		testSorts("quicksort3", n, qsort3, iterations);
//		testSorts("quicksort", n, qsort, iterations);
		testSorts("collections", n, std, iterations);
	}
	
	
	static <T> long testSorts(String id, int n, SortStrategy<Integer> s, int iterations) {
		System.out.println(id);
		List<Long> times = new ArrayList<Long>();
		for (int i = 0; i < iterations; i++) {
			System.out.println("creating input list");
			List<Integer> list = Harness.createList(n);
			System.out.println("input list created");
			long time = testSort(list, s);
			times.add(time);
			System.out.println("sort["+i+"] time["+time+"]");
		}
		long mean = calculateMean(times);		
		System.out.println("mean["+mean+"]");
		return mean;
	}
	
	static <T> long testSort(List<Integer> list, SortStrategy<Integer> s) {
		try {
			long start = System.currentTimeMillis();
			List<Integer> sorted = s.sort(list);
			long end = System.currentTimeMillis();
			long time = end - start;
			Harness.verify(sorted);
			return time;
		}
		catch (Exception e) {
			System.out.println("exception");
			e.printStackTrace();
		}
		return 0;
	}

	public static long calculateMean(List<Long> set) {
		long numerator = 0;
		for (Long i: set) {
			numerator += i;
		}
		long result = (long) Math.floor(numerator / set.size());
		return result;
	}

	static abstract class SortStrategy<T> {
		abstract public List<T> sort(List<T> unsorted);
	}	
}
