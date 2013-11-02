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
	CyclicBarrier barrier;
	int P;
	Map<Integer, List<Bound>> procBoundMap = new HashMap<Integer, List<Bound>>();
	Map<Integer, List<Integer>> procMergedListMap = new HashMap<Integer, List<Integer>>();
	
	public static List<Integer> sort(List<Integer> a, int P) {
		List<Integer> sortedA = new PSRSSort(P).parentSort(a);
		return sortedA;
	}
	
	PSRSSort(int p) {
		this.P = p;
		samples = Collections.synchronizedList(new ArrayList<Integer>());
		barrier = new CyclicBarrier(p);
	}

	List<Integer> parentSort(final List<Integer> a) {
		this.a = a;
		this.aFinal = new Integer[a.size()];
		
		for (int p = 0; p < P; p++) {
			childSort(p); // TODO: threads
		}
		
		// join
		
		return Arrays.asList(aFinal);
	}
	
	void childSort(int p) {
		Bound b = getBounds(p);
		
		// quicksort local list
		SequentialSort.quicksort(a, b.low, b.high);

		// sample local list
		List<Integer> sample = getSample(b.low, b.high);
		samples.addAll(sample);
		
		// sort the sample list and obtain the pivots
		if (p == 0) {
			SequentialSort.quicksort(samples, 0, samples.size() - 1);
			pivots = getPivots(samples);
		}
		barrierAwait();
		
		// store bounds, map of proc specific lists
		disectLocalList(a, b);
		barrierAwait();

		// each proc iterates own list, merge (insert lowest value in central list, update bound)
		mergeLocalLists(a, p);
	}
	
	Bound getBounds(int p) {
		Bound b = new Bound();
		int n = a.size();
		int elemsPerProc = (int) Math.ceil(n / P);
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
//			System.out.println("i ["+i+"] n ["+n+"] P ["+P+"] b.low ["+low+"] index ["+index+"]");
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
//			System.out.println("i ["+i+"] P ["+P+"] index ["+index+"]");
			if (index > list.size() - 1) {
				index = list.size() - 1;
			}
			pivots.add(list.get(index - 1));
		}
		return pivots;
	}
	
	void barrierAwait() {
		try {
			barrier.await();
			barrier.reset();
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
		int currIndex = findStartIndex(boundList); // TODO: this is wrong, lowest bound is not where it should start
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
	
	int findStartIndex(List<Bound> boundList) {
		int startIndex = Integer.MAX_VALUE;
		for (Bound b: boundList) {
			if (startIndex > b.low) {
				startIndex = b.low;
			}
		}
		return startIndex;
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
	
	void addBound(int p, Bound b) {
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
