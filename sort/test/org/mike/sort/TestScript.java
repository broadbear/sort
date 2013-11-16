package org.mike.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestScript {

	public static void main(String[] args) {
		int n = 5000000;
		int iterations = 20;
		SortStrategy<Integer> psrs = new SortStrategy<Integer>() {
			public List<Integer> sort(List<Integer> unsorted) {
				List<Integer> sorted = PCollections.sort(unsorted);
				return sorted;
			}
		};

		SortStrategy<Integer> std = new SortStrategy<Integer>() {
			public List<Integer> sort(List<Integer> unsorted) {
				Collections.sort(unsorted);
				return unsorted;
			}
		};

		testSorts("collections", n, std, iterations);
		testSorts("pcollections", n, psrs, iterations);
	}

	static <T> long testSorts(String id, int n, SortStrategy<Integer> s, int iterations) {
		System.out.println(id);
		List<Long> times = new ArrayList<Long>();
		for (int i = 0; i < iterations; i++) {
			long time = testSort(n, s);
			times.add(time);
			System.out.println("sort["+i+"] time["+time+"]");
		}
		long mean = calculateMean(times);		
		System.out.println("mean["+mean+"]");
		return mean;
	}
	
	static <T> long testSort(int n, SortStrategy<Integer> s) {
		List<Integer> list = Harness.createList(n);
		long start = System.currentTimeMillis();
		List<Integer> sorted = s.sort(list);
		long end = System.currentTimeMillis();
		long time = end - start;
		Harness.verify(sorted);
		return time;
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
