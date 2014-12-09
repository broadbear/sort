package org.mike.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestScriptArray {

	public static void main(String[] args) {
		int n = 50000000;
		int iterations = 5;
		SortStrategy psrs = new SortStrategy() {
			public int[] sort(int[] unsorted) {
				int[] sorted = PSRSSortArray.sort(8, unsorted);
				return sorted;
			}
		};

		SortStrategy arrays = new SortStrategy() {
			public int[] sort(int[] unsorted) {
				Arrays.parallelSort(unsorted);
				return unsorted;
			}
		};
		
		testSorts("psrs", n, psrs, iterations);
		testSorts("arrays", n, arrays, iterations);
	}
	
	
	static <T> long testSorts(String id, int n, SortStrategy s, int iterations) {
		System.out.println(id);
		List<Long> times = new ArrayList<Long>();
		for (int i = 0; i < iterations; i++) {
			int[] a = Harness.createArray(n);
			long time = testSort(a, s);
			times.add(time);
			System.out.println("sort["+i+"] time["+time+"]");
		}
		long mean = calculateMean(times);		
		System.out.println("mean["+mean+"]");
		return mean;
	}
	
	static <T> long testSort(int[] a, SortStrategy s) {
		try {
			long start = System.currentTimeMillis();
			int[] sorted = s.sort(a);
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

	static abstract class SortStrategy {
		abstract public int[] sort(int[] unsorted);
	}	
}
