package org.mike.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PSRSSort<T> {
	List<T> a;
	Object[] aFinal;
	List<T> samples;
	List<T> pivots;
	CyclicBarrier barrier1;
	CyclicBarrier barrier2;
	CyclicBarrier barrier3;
	CyclicBarrier barrier4;
	Integer[] localListSize;
	int P;
	Map<Integer, List<Bound>> procBoundMap = new HashMap<Integer, List<Bound>>();
	boolean debug = false;
	boolean perf = true;
	Comparator<? super T> comp;
	Comparator<Bound> boundComparator = new Comparator<Bound> () {
		@Override
		public int compare(Bound b1, Bound b2) {
			if (comp == null) {
				Comparable b1Val = (Comparable) a.get(b1.low);
				return b1Val.compareTo((Comparable) a.get(b2.low));
			}
			else {
				return comp.compare(a.get(b1.low), a.get(b2.low));
			}			
		}
	};
	final Logger log = LoggerFactory.getLogger(PSRSSort.class);

	public static <T extends Comparable<? super T>> List<T> sort(int P, List<T> a, boolean debug) {
		PSRSSort<T> psrsSort = new PSRSSort<T>(P);
		psrsSort.debug = debug;
		List<T> sortedA = psrsSort.parentSort(a);
		return sortedA;
	}
	
	public static <T extends Comparable<? super T>> List<T> sort(int P, List<T> a) {
		return sort(P, a, null);
	}
	
	public static <T> List<T> sort(int P, List<T> a, Comparator<? super T> c) {
		PSRSSort<T> psrsSort = new PSRSSort<T>(P);
		psrsSort.comp = c;
		List<T> sortedA = psrsSort.parentSort(a);
		return sortedA;
	}
	
	PSRSSort(int p) {
		this.P = p;
		samples = Collections.synchronizedList(new ArrayList<T>());
		barrier1 = new CyclicBarrier(p);
		barrier2 = new CyclicBarrier(p);
		barrier3 = new CyclicBarrier(p);
		barrier4 = new CyclicBarrier(p);
		localListSize = new Integer[p];
	}

	List<T> parentSort(final List<T> a) {
		log.debug("enter");
		this.a = a;
		log.debug("creating final array");
		this.aFinal = new Integer[a.size()]; // TODO: sometimes some latency here
		log.debug("final array created");
		
		// TODO: below, something is taking a really long 
		//   time, sometimes, for some or all threads to start.
		//   What could it be?
		List<Thread> threads = new ArrayList<Thread>();
		for (int p = 0; p < P; p++) {
			final int pTemp = p;
			log.debug("proc["+p+"] creating");
			Thread t = new Thread(new Runnable() {
				public void run() {
					childSort(pTemp);
				}
			});
			log.debug("proc["+p+"] created");
			log.debug("proc["+p+"] starting");
			t.setPriority(Thread.MIN_PRIORITY);
			t.start(); // TODO: seems starting so many threads at once may choke out subsequent threads
			           // TODO: there is also a problem if there are not enough cores to handle the threads
			log.debug("proc["+p+"] started");
			threads.add(t);
		}
		for (Thread t: threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		List<Object> returnList = Arrays.asList(aFinal);
		return (List<T>) returnList;
	}
	
	void childSort(int p) {
		log.debug("p["+p+"] enter childSort");
		Bound localBound = getBounds(p);

		long start = 0; long end = 0;
		if (perf && p == 0) start = System.currentTimeMillis();

		long sortStart = System.currentTimeMillis();
		// quicksort local list
		if (debug) log.debug("p["+p+"] quicksort low["+localBound.low+"] high["+localBound.high+"]");
		SequentialSort.quicksort3(a, localBound.low, localBound.high, comp);
//		if (p == 0) log.debug("local sorted: "+a);

		// sample local list
		List<T> sample = getSample(localBound.low, localBound.high);
		samples.addAll(sample);
		
		if (perf) {
			end = System.currentTimeMillis();
			log.debug("proc["+p+"] parallel sort and sampling phase: "+(end - sortStart));
		}

		barrierAwait(barrier1);
		
		if (perf && p == 0) {
			end = System.currentTimeMillis();
			log.debug("parallel sort and sampling phase: "+(end - start));
			start = System.currentTimeMillis();
		}

		if (debug && p == 0) log.debug("samples: "+samples.toString());

		// sort the sample list and obtain the pivots
		if (p == 0) {
			SequentialSort.quicksort3(samples, 0, samples.size() - 1, comp);
			pivots = getPivots(samples);
		}
		barrierAwait(barrier2);

		if (perf && p == 0) {
			end = System.currentTimeMillis();
			log.debug("sample sort and pivot generation phase: "+(end - start));
			start = System.currentTimeMillis();
		}

		if (debug && p == 0) log.debug("sorted samples: "+samples.toString());
		if (debug && p == 0) log.debug("pivots: "+pivots.toString());
		
		// store bounds, map of proc specific lists
		disectLocalList(a, localBound);
		barrierAwait(barrier3);

		if (perf && p == 0) {
			end = System.currentTimeMillis();
			log.debug("local list dissection phase: "+(end - start));
			start = System.currentTimeMillis();
		}

		if (debug && p == 0) log.debug("procBoundMap: "+procBoundMap);

		if (p == 0) {
			for (int i = 0; i < P; i++) {
				localListSize[i] = findLocalListSize(i);
				if (debug) log.debug("p["+i+"] size["+localListSize[i]+"]");
			}
		}
		barrierAwait(barrier4);

		long mergeStart = System.currentTimeMillis(); // TODO: remove
		// each proc iterates own list, merge (insert lowest value in central list, update bound)
		mergeLocalLists(a, p);

		if (perf) {
			end = System.currentTimeMillis();
			log.debug("proc["+p+"] local list merge phase: "+(end - mergeStart));
		}
	}
	
	Bound getBounds(int p) {
		Bound b = new Bound();
		int n = a.size();
		int elemsPerProc = (int) Math.ceil((float)n / (float)P);
		if (debug) log.debug("p["+p+"] bounds n["+n+"] elemsPerProc["+elemsPerProc+"]");
		b.low = p * elemsPerProc;
		b.high = b.low - 1 + elemsPerProc;
		if (b.high > a.size() - 1) {
			b.high = a.size() - 1;
		}
		return b;
	}
	
	List<T> getSample(int low, int high) {
		List<T> sample = new ArrayList<T>();
		int n = a.size();
		for (int i = 0; i < P; i++) {
			int index = ((i * n) / (P * P)) + 1 + low;
			if (debug) log.debug("i ["+i+"] n ["+n+"] P ["+P+"] b.low ["+low+"] index ["+index+"]");
			if (index > high) {
				index = high;
			}
			sample.add(a.get(index - 1));
		}
		return sample;
	}
	
	List<T> getPivots(List<T> list) {
		List<T> pivots = new ArrayList<T>();
		for (int i = 1; i < P; i++) {
			int index = ((i * P) + (int) Math.floor(P / 2));
			if (debug) log.debug("i ["+i+"] P ["+P+"] index ["+index+"]");
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
	
	void disectLocalList(List<T> list, Bound b) {
		Bound newBound = new Bound();
		newBound.low = b.low;
		for (int pivotIndex = 0; pivotIndex <= pivots.size() - 1; pivotIndex++) {
			int i = newBound.low;			
			i = findNextHighBound(pivotIndex, i, b.high);			
			newBound.high = i - 1;
			addBound(pivotIndex, newBound);
			newBound = new Bound();
			newBound.low = i;
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

	// TODO: Some generics trickery, best we can do? Better than before?
	int findNextHighBound(int pivotIndex, int currIndex, int high) {
		if (comp == null) {
			Comparable pivotValue = (Comparable) pivots.get(pivotIndex);
			while (((Comparable) a.get(currIndex)).compareTo(pivotValue) <= 0 && currIndex <= high) {
				currIndex++;
			}
		}
		else {
			T pivotValue = pivots.get(pivotIndex);
			while(comp.compare(a.get(currIndex), pivotValue) <= 0 && currIndex <= high) {
				currIndex++;
			}
		}
		return currIndex;
	}
	
	void mergeLocalLists(List<T> list, int p) {
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
	
	void mergeWithHeap(int p) {
		List<Bound> boundList = getBoundList(p);
		int currIndex = findStartIndex(p);
		PriorityQueue<Bound> heap = new PriorityQueue<Bound>(boundComparator);
		for (Bound b: boundList) {
			heap.add(b);
		}
		while (!heap.isEmpty()) {
			Bound b = heap.poll();
			aFinal[currIndex] = a.get(b.low);
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
	
	Bound findNextLowest(List<Bound> boundList) {
		Bound lowest = null;
		for (Bound b: boundList) {
			
			// TODO: more generics trickery
			if (comp == null) {
				Comparable currValue = (Comparable) a.get(b.low);
				if (lowest == null || currValue.compareTo((Comparable) a.get(lowest.low)) < 0) {
					lowest = b;
				}
			}
			else {
				T currValue = a.get(b.low);
				if (lowest == null || comp.compare(currValue, a.get(lowest.low)) < 0) {
					lowest = b;
				}
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
