package org.mike.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public final class PSRSSort {
	List<Integer> a;
	List<Integer> aFinal;
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
		
		for (int p = 0; p < P; p++) {
			childSort(p); // TODO: threads
		}
		
		// join
		
		// iterate procs, concatenating each procs own central list into single central list
		for (int i = 0; i < P; i++) {
			List<Integer> procMergedList = procMergedListMap.get(i);
			aFinal.addAll(procMergedList);
		}

		return aFinal;
	}
	
	void childSort(int p) {
		Bound b = getBounds(p);
		SequentialSort.quicksort(a, b.low, b.high);
		List<Integer> sample = getSample(b.low, b.high);
		samples.addAll(sample);
		if (p == 0) {
			SequentialSort.quicksort(samples, 0, samples.size() - 1);
			pivots = getPivots(samples);
		}
		barrierAwait();
		

		// store bounds, map of proc specific lists
		disectLocalList(a, b);

		// each proc iterates own list, merge (insert lowest value in central list, update bound)
		List<Integer> mergedSubList = mergeSubLists(a, p);
		procMergedListMap.put(p, mergedSubList);		
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
			int index = ((i * n) / (P * P)) + 1 + low - 1;
//			System.out.println("i ["+i+"] n ["+n+"] P ["+P+"] b.low ["+low+"] index ["+index+"]");
			if (index > high) {
				index = high;
			}
			sample.add(a.get(index));
		}
		return sample;
	}
	
	List<Integer> getPivots(List<Integer> list) {
		return new ArrayList<Integer>(); // TODO
	}
	
	void barrierAwait() {
		try {
			barrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void disectLocalList(List<Integer> list, Bound b) {
		Bound newBound = new Bound();
		newBound.low = b.low;
		for (int i = 0; i <= pivots.size() - 1; i++) {
			int j = newBound.low;
			Integer currPivotValue = pivots.get(i);
			while (a.get(j) <= currPivotValue && j <= b.high) { // TODO: advances one index too far
				j++;
			}
			newBound.high = j - 1;
			List<Bound> boundList = getBoundList(i);
			boundList.add(newBound);
			newBound = new Bound();
			newBound.low = j;
		}

		if (newBound.low < b.high) {
			int i = newBound.low;
			while (i < b.high) {
				i++;
			}
			newBound.high = i;
			List<Bound> boundList = getBoundList(pivots.size());
			boundList.add(newBound);
		}
	}
	
	List<Integer> mergeSubLists(List<Integer> list, int p) {
		List<Integer> subList = new ArrayList<Integer>();
		List<Bound> boundList = getBoundList(p);
		while(boundList.size() > 0) {
			Bound lowest = null;
			for (Bound b: boundList) {
				Integer currValue = a.get(b.low);
				if (lowest == null || currValue < a.get(lowest.low)) {
					lowest = b;
				}
			}
			subList.add(a.get(lowest.low));
			lowest.low++;
			if (lowest.low > lowest.high) {
				boundList.remove(lowest);
			}
		}
		return subList;
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
