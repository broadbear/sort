package org.mike.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

public class ParallelQuicksort<T> {
	int P;
	int n;
	int minPartition;
	Stack<Bound> stack;
	Comparator<? super T> c;
	
	boolean stop;
	int waiting;

	public static <T> void sort(int P, int minPartition, final List<T> a, Comparator<? super T> c) {
		ParallelQuicksort<T> sorter = new ParallelQuicksort<T>(P, minPartition, c);
		sorter.parentSort(a);
	}
	
	public static <T extends Comparable<? super T>> void sort(int P, int minPartition, final List<T> a) {
		ParallelQuicksort<T> sorter = new ParallelQuicksort<T>(P, minPartition);
		sorter.parentSort(a);
	}
	
	public static <T extends Comparable<? super T>> void sort(final List<T> a) {
		ParallelQuicksort<T> sorter = new ParallelQuicksort<T>();
		sorter.parentSort(a);
	}
	
	private ParallelQuicksort(int p, int minPartition, Comparator<? super T> c) {
		this.P = p;
		this.minPartition = minPartition;
		this.c = c;
	}
	
	private ParallelQuicksort(int p, int minPartition) {
		this.P = p;
		this.minPartition = minPartition;
	}
	
	private ParallelQuicksort() {
		this.P = Runtime.getRuntime().availableProcessors();
		this.minPartition = 0;
	}

	void parentSort(final List<T> a) {
		n = a.size();
//		this.a = a;
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
					if (c == null) {
						SequentialSort.insertionSort(a, bounds.low, bounds.high);
					}
					else {
						SequentialSort.insertionSort(a, bounds.low, bounds.high, c);
					}
					break;
				}
				else {
					if (c == null) {
						median = partition(a, bounds.low, bounds.high);
					}
					else {
						median = partition(a, bounds.low, bounds.high, c);
					}
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

	static <T> int partition(List<T> a, int left, int right) {
		int pivotIndex = left;
		Comparable pivotValue = (Comparable) a.get(pivotIndex);
		swap(a, pivotIndex, right);
		int storeIndex = left;
		for (int i = left; i <= right - 1; i++) {
			if (((Comparable) a.get(i)).compareTo(pivotValue) <= 0) {
				swap(a, i, storeIndex);
				storeIndex = storeIndex + 1;
			}
		}
		swap(a, storeIndex, right);
		return storeIndex;
	}

	int partition(List<T> a, int left, int right, Comparator<? super T> c) {
		int pivotIndex = left;
		T pivotValue = a.get(pivotIndex);
		swap(a, pivotIndex, right);
		int storeIndex = left;
		for (int i = left; i <= right - 1; i++) {
			if (c.compare(a.get(i), pivotValue) <= 0) {
				swap(a, i, storeIndex);
				storeIndex = storeIndex + 1;
			}
		}
		swap(a, storeIndex, right);
		return storeIndex;
	}
	
	// TODO: Replace with Collections.swap? Test for perf.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void swap(List a, int i, int j) {
		a.set(i, a.set(j, a.get(i)));
	}
	
	private static Bound createBound(int low, int high) {
		Bound b = new Bound();
		b.low = low;
		b.high = high;
		return b;
	}
}
