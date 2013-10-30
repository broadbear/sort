package org.mike.drivers;


import java.util.ArrayList;
import java.util.Collections;
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
		
		int mean = test();
		System.out.println("mean ["+mean+"]");
		
//		PSRSSort psrsSort = new PSRSSort(3);
//		List<Integer> list = createPSRSTestList();
//		System.out.println(list);
//		psrsSort.sort(list);
//		System.out.println(list.size()+","+psrsSort.getBounds(0)+","+psrsSort.getBounds(1)+","+psrsSort.getBounds(2));
//		for (int i = 0; i < 3; i++) {
//			PSRSSort.Bound b = psrsSort.getBounds(i);
//			List<Integer> sample = psrsSort.getSample(b.low, b.high);
//			System.out.println(sample);
//		}
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
	
	public static void verify(List<Integer> list) {
		Integer prev = -1;
		for (Integer i: list) {
			if (i < prev) {
				throw new RuntimeException("bad sort! prev["+prev+"] curr["+i+"]");
			}
			prev = i;
		}
	}
	
	public static int test() {
		List<Integer> times = new ArrayList<Integer>();
		
		for(int i = 0; i < 100; i++) {
			List<Integer> list = Driver.createList(4000000);
			long startTime = System.currentTimeMillis();
			Collections.sort(list);
//			ParallelQuicksort.sort(2, 1000, list);
			long endTime = System.currentTimeMillis();
			long time = endTime - startTime;
			System.out.println("iteration ["+i+"] time ["+time+"]");
			times.add((int)(time));
		}
		
		List<Integer> sample = getSample(32, times);
		int stdDev = calculateStdDev(sample);
		int mean = calculateMean(sample);
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
