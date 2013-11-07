package org.mike.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class PSRSSortTest {

	@Test
	public void testGetBounds() {
		testN(1, 10, 0, 9);
		testN(2, 10, 0, 4, 	5, 9);
		testN(3, 10, 0, 3, 	4, 7, 	8, 9);
		testN(4, 10, 0, 2, 	3, 5, 	6, 8, 	9, 9);
		
		testN(1, 11, 0, 10);
		testN(2, 11, 0, 5, 	6, 10);
		testN(3, 11, 0, 3, 	4, 7, 	8, 10);
		testN(4, 11, 0, 2, 	3, 5, 	6, 8, 	9, 10);
		
		testN(1, 100, 0, 99);
		testN(2, 100, 0, 49, 	50, 99);
		testN(3, 100, 0, 33,	34, 67, 	68, 99);
		testN(4, 100, 0, 24,	25, 49,		50, 74,		75, 99);
		
		testN(1, 101, 0, 100);
		testN(2, 101, 0, 50, 	51, 100);
		testN(3, 101, 0, 33,	34, 67, 	68, 100);
		testN(4, 101, 0, 25,	26, 51,		52, 77,		78, 100);		
	}

	public void testN(int P, int n, int... bounds) {
		PSRSSort sorter = createPsrsSort(P, n);
		for (int p = 0; p < P; p++){
			Bound b = sorter.getBounds(p);
			int low = bounds[(p*2)];
			int high = bounds[(p*2)+1];
			assertBound(b, low, high);
		}
		System.out.println("-");
	}
	
	public void assertBound(Bound b, int low, int high) {
		System.out.println("asserting low["+low+"] high["+high+"]");
		Assert.assertEquals(low, b.low);
		Assert.assertEquals(high, b.high);
	}
	
	public PSRSSort createPsrsSort(int p, int n) {
		PSRSSort psrsSort = new PSRSSort(p);
		psrsSort.a = createList(n);
		psrsSort.debug = true;
		return psrsSort;
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
}
