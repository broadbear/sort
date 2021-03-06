package org.mike.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * This class encapsulates a Parallel Quicksort algorithm.
 * 
 * In practice, it appears to scale well up to a small number
 * of processors when run against a list with millions of
 * elements that consist of a diverse set of values. Multi-process 
 * executions have been demonstrated to be considerably faster than
 * the standard Collecions.sort(), which is implemented
 * as a sequential mergesort, under the conditions described
 * above.
 * 
 * The algorithm sorts the given list 'in-place,' however
 * a stack is utilized in lieu of recursion. The bounds
 * pushed onto this stack do represent a small amount of
 * additional memory required to perform the sort.
 * 
 * Being based on Quicksort, lists that are mostly
 * sorted, and lists with low variance of values will
 * run extremely slow.
 * 
 * @author broadbear
 *
 * @param <T>
 */
public final class ParallelQuicksort<T> {
	int P;
	int n;
	int minPartition;
	Stack<Bound> stack;
	Comparator<? super T> c;
	
	boolean stop;
	int waiting;

	/**
	 * Sorts the given List based on the given Comparator instance.
	 * 
	 * @param P the number of child threads to instantiate.
	 * @param minPartition the smallest run of list elements to sort via the parallel 
	 * quicksort algorithm. Runs smaller than this number are sorted via a sequential insertion sort.
	 * @param a the list to sort. The given list is sorted 'in-place.'
	 * @param c a comparator instance to determine the sort order of individual elements.
	 */
	public static <T> void sort(int P, int minPartition, final List<T> a, Comparator<? super T> c) {
		ParallelQuicksort<T> sorter = new ParallelQuicksort<T>(P, minPartition, c);
		sorter.parentSort(a);
	}

	/**
	 * Sorts the given List based on the list elements' natural ordering.
	 * The list elements must inherit the Comparable interface.
	 * 
	 * @param P the number of child threads to instantiate.
	 * @param minPartition the smallest run of list elements to sort via the parallel 
	 * quicksort algorithm. Runs smaller than this number are sorted via a sequential insertion sort.
	 * @param a the list to sort. The given list is sorted 'in-place.'
	 */
	public static <T extends Comparable<? super T>> void sort(int P, int minPartition, final List<T> a) {
		ParallelQuicksort<T> sorter = new ParallelQuicksort<T>(P, minPartition);
		sorter.parentSort(a);
	}
	

	/**
	 * Sorts the given List based on the list elements' natural ordering.
	 * The list elements must inherit the Comparable interface. This method
	 * uses Java Runtime to configure the sort algorithm to use a number of 
	 * processes equal to processors reported by the system.
	 * 
	 * @param a the list to sort. The given list is sorted 'in-place.'
	 */
	public static <T extends Comparable<? super T>> void sort(final List<T> a) {
		ParallelQuicksort<T> sorter = new ParallelQuicksort<T>();
		sorter.parentSort(a);
	}
	
	ParallelQuicksort(int p, int minPartition, Comparator<? super T> c) {
		this.P = p;
		this.minPartition = minPartition;
		this.c = c;
	}
	
	ParallelQuicksort(int p, int minPartition) {
		this.P = p;
		this.minPartition = minPartition;
	}
	
	ParallelQuicksort() {
		this.P = Runtime.getRuntime().availableProcessors();
		this.minPartition = 0;
	}

	void parentSort(final List<T> a) {
		n = a.size();
		initializeStack(n);
		
		stop = false;
		waiting = 0;
		
		List<Thread> threads = new ArrayList<Thread>();
		for (int p = 0; p < P; p++) {
			Thread t = new Thread(new Runnable() {
				public void run() {
					childSort(a);
				}
			});
			t.start();
			threads.add(t);
		}
		for (Thread t: threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	
	void childSort(List<T> a) {
		Bound bounds;
		int median;
		
 		while (!stop) {
			bounds = stackDelete();
			while (bounds.low < bounds.high) {
				if (bounds.high - bounds.low < minPartition) {
					SequentialSort.insertionSort(a, bounds.low, bounds.high, c);
					break;
				}
				else {
					int pivotIndex = bounds.low;
					median = SequentialSort.partition(a, bounds.low, bounds.high, pivotIndex, c);
					stackInsert(median + 1, bounds.high);
//					System.out.println("t["+Thread.currentThread().getName()+"] bounds "+bounds+" median ["+median+"]");
					if (median <= bounds.low) {
						bounds.high = bounds.low;
					}
					else {
						bounds.high = median - 1;
					}
				}
			}
		}
	}
	
	void initializeStack(int n) {
		stack = new Stack<Bound>();
		Bound b = createBound(0, n - 1);
		stack.push(b);
	}
	
	synchronized void stackInsert(int low, int high) {
		Bound b = createBound(low, high);
		stack.push(b);
		notifyAll();
	}
		
	synchronized Bound stackDelete() {
		try {
			Bound b = stack.pop();
			return b;
		}
		catch (EmptyStackException e) {
			if (++waiting == P) {
				stop = true;
				notifyAll();
			}
			else {
				try {
					wait();
				}
				catch (InterruptedException ie) {
				}
				--waiting;
			}
			Bound b = createBound(0, 0);
			return b;
		}
	}
	
	private static Bound createBound(int low, int high) {
		Bound b = new Bound();
		b.low = low;
		b.high = high;
		return b;
	}
}
