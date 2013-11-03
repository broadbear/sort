package org.mike.sort;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


/**
 * Testing:
 * dna1: sort(1, 1, list), run 100 times, take random sample of 32, find mean/std deviation
 * dna2: mutate(dna1), run 100 times, take random sample of 32, find mean/std deviation
 * if (mean2 < mean1) -> "reward" mutation
 * else "penalize" mutation
 * dna3: mutate(dna2+reward/penalty)
 * 
 * or
 * 
 * dna1: 1, 1
 * dna2: mutate(dna1)
 * if (dna2(curr) < dna1(prev)) -> dna3(new): mutate(dna2(curr))
 * else ->dna3(new): mutate(dna1(prev))
 * 
 * @author broadbear
 *
 */
public class Driver {

	public static void main(String[] args) {
//		System.out.println("parallel quicksort");
//		List<Integer> list = Driver.createList(2000000);
////		System.out.println("before: "+list);
//		long startTime = System.currentTimeMillis();
//		new ParallelQuicksortV2(2, 1000).sort(list);
//		long endTime = System.currentTimeMillis();
////		System.out.println("after: "+list);
//		verify(list);
//		System.out.println("sort checks out");
//		System.out.println("time ["+(endTime - startTime)+"]");
		
//		int mean = test(2);
//		System.out.println("mean ["+mean+"]");
//		mean = test(2);
//		System.out.println("mean ["+mean+"]");
//		mean = test(3);
//		System.out.println("mean ["+mean+"]");
//		mean = test(4);
//		System.out.println("mean ["+mean+"]");
		
		List<Integer> list = createPSRSTestList();
		System.out.println("start: "+list);
		PSRSSort sorter = new PSRSSort(3);
		sorter.a = list;
		System.out.println(sorter.a.size()+","+sorter.getBounds(0)+","+sorter.getBounds(1)+","+sorter.getBounds(2));
		for (int i = 0; i < 3; i++) {
			Bound b = sorter.getBounds(i);
			SequentialSort.quicksort(sorter.a, b.low, b.high);
			List<Integer> sample = sorter.getSample(b.low, b.high);
			System.out.println("p["+i+"], sample: "+sample);
			sorter.samples.addAll(sample);
		}
		System.out.println("locally quicksorted: "+sorter.a);
		
		SequentialSort.quicksort(sorter.samples, 0, sorter.samples.size() - 1);
		System.out.println("samples, post-sort: "+sorter.samples);

		sorter.pivots = sorter.getPivots(sorter.samples);
		System.out.println("pivots: "+sorter.pivots);
		
		for (int i = 0; i < 3; i++) {
			sorter.disectLocalList(sorter.a, sorter.getBounds(i));
		}
		for (int i = 0; i < 3; i++) {
			sorter.localListSize[i] = sorter.findLocalListSize(i);
		}
		System.out.println("procBoundMap: "+sorter.procBoundMap);
		sorter.aFinal = new Integer[sorter.a.size()];
		System.out.println("size: "+sorter.a.size());
		for (int i = 0; i < 3; i++) {
			sorter.mergeLocalLists(sorter.a, i);
		}
		System.out.println("final list: "+Arrays.asList(sorter.aFinal));
	}
	
	
	public static List<Integer> createList(int n) {
		List<Integer> l = new ArrayList<Integer>();
		Random r = new Random();
		r.setSeed(123);
		for (int i = 0; i < n; i++) {
			l.add(r.nextInt(10000));
		}
		return l;
	}
	
	public static List<Integer> createPSRSTestList() {
		List<Integer> list = new ArrayList<Integer>();
		Integer[] a = {15, 46, 48, 93, 39, 6, 72, 91, 14, 36, 69, 40, 89, 61, 97, 12, 21, 54, 53, 97, 84, 58, 32, 27, 33, 72, 20};
		for (Integer i: a) {
			list.add(i);
		}
		return list;
	}
	
	public static List<Integer> createPSRSInterimTestList1() {
		List<Integer> list = new ArrayList<Integer>();
		Integer[] a = {6, 14, 15, 39, 46, 48, 72, 91, 93, 12, 21, 36, 40, 54, 61, 69, 89, 97, 20, 27, 32, 33, 53, 58, 72, 84, 97};
		for (Integer i: a) {
			list.add(i);
		}
		return list;
	}
	
	public static void verify(List<Integer> list) {
		Integer prev = -1;
		for (Integer i: list) {
			if (i < prev) {
				throw new RuntimeException("bad sort! prev["+prev+"] curr["+i+"]");
			}
			prev = i;
		}
	}
	
	public static int test(int p) {
		List<Integer> times = new ArrayList<Integer>();
		
		for(int i = 0; i < 100; i++) {
			List<Integer> list = Driver.createList(10000000);
			long startTime = System.currentTimeMillis();
//			Collections.sort(list);
			ParallelQuicksort.sort(p, 1000, list, new Comparator<Integer>() {
				@Override
				public int compare(Integer o1, Integer o2) {
					return o1.compareTo(o2);
				}
			});
			long endTime = System.currentTimeMillis();
			long time = endTime - startTime;
			System.out.println("iteration ["+i+"] time ["+time+"]");
			times.add((int)(time));
			verify(list);
//			System.out.println("sort checks out");
		}
		
//		List<Integer> sample = getSample(32, times);
		int stdDev = calculateStdDev(times);
		int mean = calculateMean(times);
		return mean;
	}
	
	public static List<Integer> getSample(int size, List<Integer> set) {
		List<Integer> sample = new ArrayList<Integer>();
		Random r = new Random();
		for (int i = 0; i < size; i++) {
			int rIndex = r.nextInt(set.size() - 1);
			Integer rValue = set.remove(rIndex);
			sample.add(rValue);
		}
		return sample;
	}
	
	public static int calculateMean(List<Integer> set) {
		int numerator = 0;
		for (Integer i: set) {
			numerator += i;
		}
		int result = (int) Math.floor(numerator / set.size());
		return result;
	}
	
	public static int calculateStdDev(List<Integer> set) {
		return 0;
	}
}
