package org.mike.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class PSRSSort {
	private List<Integer> a;
	private List<Integer> samples;
	private List<Integer> pivots;
	CyclicBarrier barrier;
	private int P;

	public PSRSSort(int p) {
		this.P = p;
		samples = new ArrayList<Integer>();
		barrier = new CyclicBarrier(p);
	}

	public void sort(final List<Integer> a) {
		this.a = a;
		
		for (int p = 0; p < P; p++) {
			doSort(p); // TODO: threads
		}
	}
	
	public void doSort(int p) {
		Bound b = getBounds(p);
		SequentialSort.quicksort(a, b.low, b.high);
		List<Integer> sample = getSample(b.low, b.high);
		samples.addAll(sample); // TODO: Synchronize
		if (p == 0) {
			SequentialSort.quicksort(samples, 0, samples.size() - 1);
			pivots = getPivots(samples);
		}
		barrierAwait();
		
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
		
	public class Bound {
		int low;
		int high;
		public String toString() {
			return "["+low+","+high+"]";
		}
	}
}
