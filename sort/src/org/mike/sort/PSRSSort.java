package org.mike.sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class PSRSSort {
	private List<Integer> a;
	private List<Integer> aFinal;
	private List<Integer> samples;
	private List<Integer> pivots;
	CyclicBarrier barrier;
	private int P;
	private Map<Integer, List<Bound>> procBoundMap = new HashMap<Integer, List<Bound>>();
	
	public PSRSSort(int p) {
		this.P = p;
		samples = new ArrayList<Integer>();
		barrier = new CyclicBarrier(p);
	}

	public List<Integer> parentSort(final List<Integer> a) {
		this.a = a;
		
		for (int p = 0; p < P; p++) {
			childSort(p); // TODO: threads
		}
		
		return aFinal;
	}
	
	public void childSort(int p) {
		Bound b = getBounds(p);
		SequentialSort.quicksort(a, b.low, b.high);
		List<Integer> sample = getSample(b.low, b.high);
		samples.addAll(sample); // TODO: Synchronize
		if (p == 0) {
			SequentialSort.quicksort(samples, 0, samples.size() - 1);
			pivots = getPivots(samples);
		}
		barrierAwait();

		// store bounds, map of proc specific lists
		disectLocalList(a, b, p);

		// each proc iterates own list, merge (insert lowest value in central list, update bound)
		List<Bound> localBoundList = procBoundMap.get(p);
		List<Integer> mergedSubList = mergeSubLists(a, p);
		
		// iterate procs, concatenating each procs own central list into single central list
		aFinal.addAll(mergedSubList); // TODO: synchronize
		
	}
	
	public Bound getBounds(int p) {
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
	
	public List<Integer> getSample(int low, int high) {
		List<Integer> sample = new ArrayList<Integer>();
		int n = a.size();
		for (int i = 0; i < P; i++) {
			int index = ((i * n) / (P * P)) + 1 + low - 1;
			System.out.println("i ["+i+"] n ["+n+"] P ["+P+"] b.low ["+low+"] index ["+index+"]");
			if (index > high) {
				index = high;
			}
			sample.add(a.get(index));
		}
		return sample;
	}
	
	public List<Integer> getPivots(List<Integer> list) {
		return new ArrayList<Integer>();
	}
	
	public void barrierAwait() {
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
	
	public void disectLocalList(List<Integer> list, Bound b, int p) {
		Bound newBound = new Bound();
		newBound.low = b.low;
		for (int i = 0; i <= pivots.size() - 1; i++) {
			int j = newBound.low;
			Integer currPivotValue = pivots.get(i);
			while (a.get(j) <= currPivotValue && j <= b.high) {
				j++;
			}
			newBound.high = j;
			List<Bound> boundList = getBoundList(i);
			boundList.add(newBound);
			newBound = new Bound();
			newBound.low = j + 1;
		}

		if (newBound.low < list.size() - 1) {
			int i = newBound.low;
			while (i < list.size()) {
				i++;
			}
			newBound.high = i;
			List<Bound> boundList = getBoundList(pivots.size());
			boundList.add(newBound);
		}
	}
	
	public List<Integer> mergeSubLists(List<Integer> list, int p) {
		List<Integer> subList = new ArrayList<Integer>();
		List<Bound> boundList = getBoundList(p);
		while(boundList.size() > 0) {
			Integer lowestSoFar = Integer.MAX_VALUE;
			Bound lowestBound = null;
			for (Bound b: boundList) {
				Integer currValue = a.get(b.low);
				if (currValue < lowestSoFar) {
					lowestBound = b;
					lowestSoFar = a.get(b.low);
				}
			}
			subList.add(a.get(lowestBound.low));
			lowestBound.low++;
			if (lowestBound.low > lowestBound.high) {
				boundList.remove(lowestBound);
			}
		}
		return subList;
	}
	
	public List<Bound> getBoundList(int p) {
		List<Bound> boundList = procBoundMap.get(p);
		if (boundList == null) {
			boundList = new ArrayList<Bound>();
			procBoundMap.put(p, boundList);
		}
		return boundList;
	}
}
