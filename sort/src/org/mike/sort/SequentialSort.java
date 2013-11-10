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
			while ((j > 0) && compare(a.get(j - 1), temp) > 0) {
				a.set(j, a.get(j - 1));
				j--;
			}
			a.set(j, temp);
		}
	}
	
	public static <T extends Comparable<? super T>> void quicksort(List<T> a, int left, int right) {
		quicksort(a, left, right, null);
	}
	
	public static <T> void quicksort(List<T> a, int left, int right, Comparator<? super T> c) {
		if (left < right) {
			int pivotIndex = left;
			int newPivotIndex = partition(a, left, right, pivotIndex, c);
			quicksort(a, left, newPivotIndex - 1, c);
			quicksort(a, newPivotIndex + 1, right, c);
		}
	}
	
	static <T> int partition(List<T> a, int left, int right, int pivotIndex, Comparator<? super T> c) {
		T pivotValue = a.get(pivotIndex);
		swap(a, pivotIndex, right);
		int storeIndex = left;
		for (int i = left; i <= right - 1; i++) {
			
			// TODO: generics trickery
			if (c == null) {
				if (compare(a.get(i), pivotValue) < 0) {
					swap(a, i, storeIndex);
					storeIndex = storeIndex + 1;
				}
			}
			else {
				if (c.compare(a.get(i), pivotValue) < 0) {
					swap(a, i, storeIndex);
					storeIndex = storeIndex + 1;
				}
			}
			
		}
		swap(a, storeIndex, right);
		return storeIndex;
	}
	
	static <T> void swap(List<T> a, int i1, int i2) {
		T temp = a.get(i1);
		a.set(i1, a.get(i2));
		a.set(i2, temp);
	}
	
	static <T> int compare(T o1, T o2) {
		return ((Comparable) o1).compareTo(o2);
	}
}
