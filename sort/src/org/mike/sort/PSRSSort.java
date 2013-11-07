package org.mike.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public final class PSRSSort {
	List<Integer> a;
	Integer[] aFinal;
	List<Integer> samples;
	List<Integer> pivots;
	CyclicBarrier barrier1;
	CyclicBarrier barrier2;
	CyclicBarrier barrier3;
	CyclicBarrier barrier4;
	Integer[] localListSize;
	int P;
	Map<Integer, List<Bound>> procBoundMap = new HashMap<Integer, List<Bound>>();
	boolean debug = false;

	public static List<Integer> sort(List<Integer> a, int P, boolean debug) {
		PSRSSort psrsSort = new PSRSSort(P);
		psrsSort.debug = debug;
		List<Integer> sortedA = psrsSort.parentSort(a);
		return sortedA;
	}
	
	public static List<Integer> sort(List<Integer> a, int P) {
		List<Integer> sortedA = new PSRSSort(P).parentSort(a);
		return sortedA;
	}
	
	PSRSSort(int p) {
		this.P = p;
		samples = Collections.synchronizedList(new ArrayList<Integer>());
		barrier1 = new CyclicBarrier(p);
		barrier2 = new CyclicBarrier(p);
		barrier3 = new CyclicBarrier(p);
		barrier4 = new CyclicBarrier(p);
		localListSize = new Integer[p];
	}

	List<Integer> parentSort(final List<Integer> a) {
		this.a = a;
		this.aFinal = new Integer[a.size()];
		
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
		
		return Arrays.asList(aFinal);
	}
	
	void childSort(int p) {
		Bound localBound = getBounds(p);
		
		// quicksort local list
		SequentialSort.quicksort(a, localBound.low, localBound.high);
//		if (p == 0) System.out.println("local sorted: "+a);

		// sample local list
		List<Integer> sample = getSample(localBound.low, localBound.high);
		samples.addAll(sample);
		barrierAwait(barrier1);
		if (debug && p == 0) System.out.println("samples: "+samples);
		
		// sort the sample list and obtain the pivots
		if (p == 0) {
			SequentialSort.quicksort(samples, 0, samples.size() - 1);
			pivots = getPivots(samples);
		}
		barrierAwait(barrier2);
		if (debug && p == 0) System.out.println("sorted samples: "+samples);
		if (debug && p == 0) System.out.println("pivots: "+pivots);
		
		// store bounds, map of proc specific lists
		disectLocalList(a, localBound);
		barrierAwait(barrier3);
		if (debug && p == 0) System.out.println("procBoundMap: "+procBoundMap);

		if (p == 0) {
			for (int i = 0; i < P; i++) {
				localListSize[i] = findLocalListSize(i);
				if (debug) System.out.println("p["+i+"] size["+localListSize[i]+"]");
			}
		}
		barrierAwait(barrier4);

		// each proc iterates own list, merge (insert lowest value in central list, update bound)
		mergeLocalLists(a, p);
	}
	
	Bound getBounds(int p) {
		Bound b = new Bound();
		int n = a.size();
		int elemsPerProc = (int) Math.ceil((float)n / (float)P);
		if (debug) System.out.println("p["+p+"] n["+n+"] elemsPerProc["+elemsPerProc+"]");
		b.low = p * elemsPerProc;
		b.high = b.low - 1 + elemsPerProc;
		if (b.high > a.size() - 1) {
			b.high = a.size() - 1;
		}
		return b;
	}
	
	List<Integer> getSample(int low, int high) {
		List<Integer> sample = new ArrayList<Integer>();
		int n = a.size();
		for (int i = 0; i < P; i++) {
			int index = ((i * n) / (P * P)) + 1 + low;
			if (debug) System.out.println("i ["+i+"] n ["+n+"] P ["+P+"] b.low ["+low+"] index ["+index+"]");
			if (index > high) {
				index = high;
			}
			sample.add(a.get(index - 1));
		}
		return sample;
	}
	
	List<Integer> getPivots(List<Integer> list) {
		List<Integer> pivots = new ArrayList<Integer>();
		for (int i = 1; i < P; i++) {
			int index = ((i * P) + (int) Math.floor(P / 2));
			if (debug) System.out.println("i ["+i+"] P ["+P+"] index ["+index+"]");
			if (index > list.size() - 1) {
				index = list.size() - 1;
			}
			pivots.add(list.get(index - 1));
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
	
	void disectLocalList(List<Integer> list, Bound b) {
		Bound newBound = new Bound();
		newBound.low = b.low;
		for (int i = 0; i <= pivots.size() - 1; i++) {
			int j = newBound.low;
			Integer currPivotValue = pivots.get(i);
			while (a.get(j) <= currPivotValue && j <= b.high) {
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
			addBound(pivots.size(), newBound);
		}
	}
	
	void mergeLocalLists(List<Integer> list, int p) {
		List<Bound> boundList = getBoundList(p);
		int currIndex = findStartIndex(p);
		while(boundList.size() > 0) {
			Bound lowest = findNextLowest(boundList);
			aFinal[currIndex] = a.get(lowest.low);
			lowest.low++;
			currIndex++;
			if (lowest.low > lowest.high) {
				boundList.remove(lowest);
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
	
	Bound findNextLowest(List<Bound> boundList) {
		Bound lowest = null;
		for (Bound b: boundList) {
			Integer currValue = a.get(b.low);
			if (lowest == null || currValue < a.get(lowest.low)) {
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
