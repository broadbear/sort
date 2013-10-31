package org.mike.sort;

import java.util.Comparator;
import java.util.List;

public class SequentialSort {
	
	public static <T> void insertionSort(List<T> a, int low, int high, Comparator<? super T> c) {
		for (int i = low + 1; i < high + 1; i++) {
			int j = i;
			T temp = a.get(i);
			while ((j > 0) && (c.compare(a.get(j - 1), temp) > 0)) {
				a.set(j, a.get(j - 1));
				j--;
			}
			a.set(j, temp);
		}
	}

	public static <T> void insertionSort(List<T> a, int low, int high) {
		for (int i = low + 1; i < high + 1; i++) {
			int j = i;
			T temp = a.get(i);
			while ((j > 0) && (((Comparable) a.get(j - 1)).compareTo(temp)) > 0) {
				a.set(j, a.get(j - 1));
				j--;
			}
			a.set(j, temp);
		}
	}
	
	public static void quicksort(List<Integer> a, int left, int right) {
		if (left < right) {
			int pivotIndex = left;
			int newPivotIndex = partition(a, left, right, pivotIndex);
			quicksort(a, left, newPivotIndex - 1);
			quicksort(a, newPivotIndex + 1, right);
		}
	}
	
	public static int partition(List<Integer> a, int left, int right, int pivotIndex) {
		Integer pivotValue = a.get(pivotIndex);
		swap(a, pivotIndex, right);
		int storeIndex = left;
		for (int i = left; i < right; i++) {
			if (a.get(i) < pivotValue) {
				swap(a, i, storeIndex);
				storeIndex = storeIndex + 1;
			}
		}
		swap(a, storeIndex, right);
		return storeIndex;
	}
	
	public static void swap(List<Integer> a, int i1, int i2) {
		Integer temp = a.get(i1);
		a.set(i1, a.get(i2));
		a.set(i1, temp);
	}
}
