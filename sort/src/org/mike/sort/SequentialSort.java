package org.mike.sort;

import java.util.Comparator;
import java.util.List;

public class SequentialSort {
	
	public static final int MIN_PARITIION = 100;
	
	public static <T extends Comparable<? super T>> void insertionSort(List<T> a, int low, int high) {
		insertionSort(a, low, high, null);
	}
	
	public static <T> void insertionSort(List<T> a, int low, int high, Comparator<? super T> c) {
		for (int i = low + 1; i < high + 1; i++) {
			int j = i;
			T temp = a.get(i);
			
			// TODO: generics trickery
			if (c == null) {
				while ((j > 0) && compare(a.get(j - 1), temp) > 0) {
					a.set(j, a.get(j - 1));
					j--;
				}
			}
			else {
				while ((j > 0) && (c.compare(a.get(j - 1), temp) > 0)) {
					a.set(j, a.get(j - 1));
					j--;
				}
			}
			
			a.set(j, temp);
		}
	}
	
	public static <T extends Comparable<? super T>> void quicksort(List<T> a, int left, int right) {
		quicksort(a, left, right, null);
	}
	
	public static <T> void quicksort(List<T> a, int left, int right, Comparator<? super T> c) {
		if (left < right) {
			if (right - left < MIN_PARITIION) {
				insertionSort(a, left, right, c); // TODO: something here breaks PSRS
			}
			else {
				int pivotIndex = left;
				int newPivotIndex = partition(a, left, right, pivotIndex, c);
				quicksort(a, left, newPivotIndex - 1, c);
				quicksort(a, newPivotIndex + 1, right, c);
			}
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
	
	public static <T extends Comparable<? super T>> void quicksort3(List<T> a, int left, int right) {
		quicksort3(a, left, right, null);
	}

	public static <T> void quicksort3(List<T> a, int left, int right, Comparator<? super T> c) {
		int i = left - 1;
		int j = right;
		int p = left - 1;
		int q = right;
		
		// TODO: Had to add this, hope its correct.
		if (right < 0) { 
			return;
		}
		
		T v = a.get(right);
		if (right <= left) {
			return;
		}
		
		while(true) {

			if (c == null) {
				while (compare(a.get(++i), v) < 0) {
				}
				while (compare(v, a.get(--j)) < 0) {
					if (j == left) {
						break;
					}
				}
			}
			else {
				while (c.compare(a.get(++i), v) < 0) {
				}
				while (c.compare(v, a.get(--j)) < 0) {
					if (j == left) {
						break;
					}
				}
			}
			
			if (i >= j) {
				break;
			}
			swap(a, i, j);
			if (a.get(i) == v) {
				p++;
				swap(a, p, i);
			}
			if (v == a.get(j)) {
				q--;
				swap(a, j, q);
			}
		}
		swap(a, i, right);
		j = i - 1;
		i = i + 1;
		for (int k = left; k < p; k++, j--) {
			swap(a, k, j);
		}
		for (int k = right - 1; k > q; k--, i++) {
			swap(a, i, k);
		}
		quicksort3(a, left, j, c);
		quicksort3(a, i, right, c);
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
