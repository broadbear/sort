package org.mike.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public final class PSRSSortArray {
	int[] a;
	int[] aFinal;
	int[] samples;
	int[] pivots;
	CyclicBarrier barrier1;
	CyclicBarrier barrier2;
	CyclicBarrier barrier3;
	CyclicBarrier barrier4;
	CyclicBarrier barrier5;
	Integer[] localListSize;
	int P;
	Map<Integer, List<Bound>> procBoundMap = new HashMap<Integer, List<Bound>>();
	boolean debug = false;
	boolean perf = true;
	Comparator<Bound> boundComparator = new Comparator<Bound> () {
		@Override
		public int compare(Bound b1, Bound b2) {
			if (a[b1.low] == a[b2.low]) return 0;
			else if (a[b1.low] < a[b2.low]) return -1;
			else return 1;
		}
	};

	public static int[] sort(int P, int[] a, boolean debug) {
		PSRSSortArray psrsSort = new PSRSSortArray(P);
		psrsSort.debug = debug;
		int[] sortedA = psrsSort.parentSort(a);
		return sortedA;
	}
	
	public static int[] sort(int P, int[] a) {
		PSRSSortArray psrsSort = new PSRSSortArray(P);
		int[] sortedA = psrsSort.parentSort(a);
		return sortedA;
	}
	
	PSRSSortArray(int p) {
		this.P = p;
		samples = new int[p * p];
		barrier1 = new CyclicBarrier(p);
		barrier2 = new CyclicBarrier(p);
		barrier3 = new CyclicBarrier(p);
		barrier4 = new CyclicBarrier(p);
		barrier5 = new CyclicBarrier(p);
		localListSize = new Integer[p];
	}

	int[] parentSort(final int[] a) {
		this.a = a;
		this.aFinal = new int[a.length];
		
		List<Thread> threads = new ArrayList<Thread>();
		for (int p = 0; p < P; p++) {
			final int pTemp = p;
			Thread t = new Thread(new Runnable() {
				public void run() {
					childSort(pTemp);
				}
			});
			t.start();
			threads.add(t);
		}
		for (Thread t: threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return aFinal;
	}
	
	void childSort(int p) {
		Bound localBound = getBounds(p);

		long start = 0; long end = 0;
		if (perf && p == 0) start = System.currentTimeMillis();
		
		// quicksort local list
		if (debug) System.out.println("p["+p+"] quicksort low["+localBound.low+"] high["+localBound.high+"]");
		// TODO: an issue below with inclusive/low, exclusive/high, other alg assumes inclusive/low, inclusive/high
		Arrays.sort(a, localBound.low, localBound.high + 1); // TODO: falling back on Java sort, which is a quicksort
//		if (p == 0) System.out.println("local sorted: "+a);
		
		// sample local list
		int[] sample = getSample(localBound.low, localBound.high);
		System.arraycopy(sample, 0, samples, p * P, sample.length);
		barrierAwait(barrier1);

		if (perf && p == 0) {
			end = System.currentTimeMillis();
			System.out.println("parallel sort and sampling phase: "+(end - start));
			start = System.currentTimeMillis();
		}

		if (debug && p == 0) System.out.println("samples: "+Arrays.toString(samples));
		
		// sort the sample list and obtain the pivots
		if (p == 0) {
			Arrays.sort(samples);
			pivots = getPivots(samples);
		}
		barrierAwait(barrier2);

		if (perf && p == 0) {
			end = System.currentTimeMillis();
			System.out.println("sample sort and pivot generation phase: "+(end - start));
			start = System.currentTimeMillis();
		}

		if (debug && p == 0) System.out.println("sorted samples: "+Arrays.toString(samples));
		if (debug && p == 0) System.out.println("pivots: "+Arrays.toString(pivots));
		
		// store bounds, map of proc specific lists
		dissectLocalList(a, localBound);
		barrierAwait(barrier3);

		if (perf && p == 0) {
			end = System.currentTimeMillis();
			System.out.println("local list dissection phase: "+(end - start));
			start = System.currentTimeMillis();
		}
		
		if (debug && p == 0) System.out.println("procBoundMap: "+procBoundMap);

		if (p == 0) {
			for (int i = 0; i < P; i++) {
				localListSize[i] = findLocalListSize(i);
				if (debug) System.out.println("p["+i+"] size["+localListSize[i]+"]");
			}
		}
		barrierAwait(barrier4);

		long myStart = System.currentTimeMillis(); // TODO: remove
		// each proc iterates own list, merge (insert lowest value in central list, update bound)
		mergeWithHeap(p);
		
//		if (perf && p == 0) {
//			end = System.currentTimeMillis();
//			System.out.println("local list merge phase: "+(end - start));
//			start = System.currentTimeMillis();
//		}
		if (perf) {
			end = System.currentTimeMillis();
			System.out.println("proc["+p+"] local list merge phase: "+(end - myStart));
			start = System.currentTimeMillis();
		}
	}
	
	Bound getBounds(int p) {
		Bound b = new Bound();
		int n = a.length;
		int elemsPerProc = (int) Math.ceil((float)n / (float)P);
		if (debug) System.out.println("p["+p+"] bounds n["+n+"] elemsPerProc["+elemsPerProc+"]");
		b.low = p * elemsPerProc;
		b.high = b.low - 1 + elemsPerProc;
		if (b.high > a.length - 1) {
			b.high = a.length - 1;
		}
		return b;
	}
	
	int[] getSample(int low, int high) {
		int[] sample = new int[P];
		int n = a.length;
		for (int i = 0; i < P; i++) {
			int index = ((i * n) / (P * P)) + 1 + low;
			if (debug) System.out.println("i ["+i+"] n ["+n+"] P ["+P+"] b.low ["+low+"] index ["+index+"]");
			if (index > high) {
				index = high;
			}
			sample[i] = a[index - 1];
		}
		return sample;
	}
	
	int[] getPivots(int[] samples) {
		int[] pivots = new int[P - 1];
		for (int i = 1; i < P; i++) {
			int index = ((i * P) + (int) Math.floor(P / 2));
			if (debug) System.out.println("i ["+i+"] P ["+P+"] index ["+index+"]");
			if (index > samples.length - 1) {
				index = samples.length - 1;
			}
			pivots[i - 1] = samples[index - 1];
		}
		return pivots;
	}
	
	void barrierAwait(CyclicBarrier barrier) {
		try {
			barrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
	
	void dissectLocalList(int[] list, Bound b) {
		Bound newBound = new Bound();
		newBound.low = b.low;
		for (int i = 0; i <= pivots.length - 1; i++) {
			int j = newBound.low;
			
			int currPivotValue = pivots[i];
			while(a[j] <= currPivotValue && j <= b.high) {
				j++;
			}
						
			newBound.high = j - 1;
			addBound(i, newBound);
			newBound = new Bound();
			newBound.low = j;
		}

		if (newBound.low < b.high) {
			int i = newBound.low;
			while (i < b.high) {
				i++;
			}
			newBound.high = i;
			addBound(pivots.length, newBound);
		}
	}

	/**
	 * Brute force list merge.
	 * 
	 * TODO: list param not needed as a and aFinal are member vars.
	 * TODO: this could be made more efficient with a heap.
	 * 
	 * As is: n elements, k bounds = O(n * k)
	 * With heap: O(n log k)
	 * 
	 * @param list
	 * @param p
	 */
	void mergeLocalLists(int[] list, int p) {
		List<Bound> boundList = getBoundList(p);
		int currIndex = findStartIndex(p);
		while(boundList.size() > 0) {
			Bound lowest = findNextLowest(boundList);
			aFinal[currIndex] = a[lowest.low];
			lowest.low++;
			currIndex++;
			if (lowest.low > lowest.high) {
				boundList.remove(lowest);
			}
		}
	}

	/**
	 * This method merges a set of local array segments
	 * with the use of a heap for efficiency. At this point
	 * several bounds denoting low and high array indexes have
	 * been populated and associated with a process.
	 * 
	 * @param p
	 */
	void mergeWithHeap(int p) {
		List<Bound> boundList = getBoundList(p);
		int currIndex = findStartIndex(p);
		PriorityQueue<Bound> heap = new PriorityQueue<Bound>(boundComparator);
		for (Bound b: boundList) {
			heap.add(b);
		}
		while (!heap.isEmpty()) {
			Bound b = heap.poll();
			aFinal[currIndex] = a[b.low];
			currIndex++;
			if (b.low < b.high) {
				b.low++;
				heap.add(b);
			}
		}
	}
	
	int findStartIndex(int p) {
		int startIndex = 0;
		for (int i = 0; i < p; i++) {
			startIndex += localListSize[i];
		}
		return startIndex;
	}

	int findLocalListSize(int p) {
		List<Bound> boundList = getBoundList(p);
		int size = 0;
		for (Bound b: boundList) {
			size += (b.high - b.low) + 1; 
		}
		return size;
	}
	
	/**
	 * Seeks the lowest value associated with a collection of bounds.
	 * The
	 * 
	 * @param boundList
	 * @return
	 */
	Bound findNextLowest(List<Bound> boundList) {
		Bound lowest = null;
		for (Bound b: boundList) {
			if (lowest == null || a[b.low] < a[lowest.low]) {
				lowest = b;
			}			
		}
		return lowest;
	}
	
	synchronized void addBound(int p, Bound b) {
		List<Bound> boundList = getBoundList(p);
		boundList.add(b);
	}

	List<Bound> getBoundList(int p) {
		List<Bound> boundList = procBoundMap.get(p);
		if (boundList == null) {
			boundList = new ArrayList<Bound>();
			procBoundMap.put(p, boundList);
		}
		return boundList;
	}
}
