package org.mike.sort;

import java.util.Comparator;
import java.util.List;

/**
 * Convenience class to wrap the fastest parallel sort algorithm.
 * 
 * @author broadbear
 *
 */
public class PCollections {

	/**
	 * Convenience method that automatically queries the number
	 * of available processors and invokes a PSRS sort. Elements
	 * of the given list must implement the Comparable interface.
	 * 
	 * @param a the list to be sorted
	 * @return the sorted list
	 */
	public static <T extends Comparable<? super T>> List<T> sort(List<T> a) {
		int P = Runtime.getRuntime().availableProcessors();
		List<T> sorted = PSRSSort.sort(P, a);
		return sorted;
	}

	/**
	 * Convenience method that automatically queries the number
	 * of available processors and invokes a PSRS sort. A
	 * Comparator instance must be provided that will determine 
	 * the comparison order of the elements in the given list.
	 * 
	 * @param a the list to be sorted
	 * @parma c a comparator instance
	 * @return the sorted list
	 */
	public static <T> List<T> sort(List<T> a, Comparator<? super T> c) {
		int P = Runtime.getRuntime().availableProcessors();
		List<T> sorted = PSRSSort.sort(P, a, c);
		return sorted;
	}
}
