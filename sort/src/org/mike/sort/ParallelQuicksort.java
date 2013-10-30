package org.mike.sort;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

public class ParallelQuicksort {
	int P;
	int n;
	List<Integer> a;
	int minPartition;
	Stack<Bound> stack;
	
	boolean stop;
	int waiting;
	
	public static void sort(int P, int minPartition, final List<Integer> a) {
		ParallelQuicksort sorter = new ParallelQuicksort(P, minPartition);
		sorter.parentSort(a);
	}
	
	public static void sort(final List<Integer> a) {
		ParallelQuicksort sorter = new ParallelQuicksort();
		sorter.parentSort(a);
	}
	
	private ParallelQuicksort(int p, int minPartition) {
		this.P = p;
		this.minPartition = minPartition;
	}
	
	private ParallelQuicksort() {
		this.P = Runtime.getRuntime().availableProcessors();
		this.minPartition = 0;
	}

	void parentSort(final List<Integer> a) {
		n = a.size();
		this.a = a;
		initializeStack();
		
		stop = false;
		waiting = 0;
		
		List<Thread> threads = new ArrayList<Thread>();
		for (int p = 0; p < P; p++) {
			Thread t = new Thread(new Runnable() {
				public void run() {
					doSort();
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
	
	void doSort() {
		Bound bounds;
		int median;
		
 		while (!stop) {
			bounds = stackDelete();
			while (bounds.low < bounds.high) {
				if (bounds.high - bounds.low < minPartition) {
					SequentialSort.insertionSort(a, bounds.low, bounds.high);
					break;
				}
				else {
					median = partition(a, bounds.low, bounds.high);
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
	
	void initializeStack() {
		stack = new Stack<Bound>();
		Bound b = createBound(0, a.size() - 1);
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

	int partition(List<Integer> a, int left, int right) {
		int pivotIndex = left;
		Integer pivotValue = a.get(pivotIndex);
		swap(a, pivotIndex, right);
		int storeIndex = left;
		for (int i = left; i <= right - 1; i++) {
			if (a.get(i) <= pivotValue) {
				swap(a, i, storeIndex);
				storeIndex = storeIndex + 1;
			}
		}
		swap(a, storeIndex, right);
		return storeIndex;
	}
	
	void swap(List<Integer> a, int i1, int i2) {
		Integer temp = a.get(i1);
		a.set(i1, a.get(i2));
		a.set(i2, temp);
	}
	
	private static Bound createBound(int low, int high) {
		Bound b = new Bound();
		b.low = low;
		b.high = high;
		return b;
	}
}
