package org.mike.sort;

import java.util.Collections;
import java.util.List;

public class TestScript {

	public static void main(String[] args) {
		int n = 5000000;
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
		
		System.out.println("collections");
		for (int i = 0; i < 20; i++) {
			long time = testSort(n, std);
			System.out.println("sort["+i+"] time["+time+"]");
		}
		System.out.println("pcollections");
		for (int i = 0; i < 20; i++) {
			long time = testSort(n, psrs);
			System.out.println("sort["+i+"] time["+time+"]");
		}
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

	static abstract class SortStrategy<T> {
		abstract public List<T> sort(List<T> unsorted);
	}	
}
