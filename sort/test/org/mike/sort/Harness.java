package org.mike.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

public class Harness {
	public static List<Integer> createList(int n) {
		List<Integer> l = new ArrayList<Integer>();
		Random r = new Random();
		r.setSeed(123);
		for (int i = 0; i < n; i++) {
			l.add(r.nextInt(10000));
		}
		return l;
	}

	public static void verify(List<Integer> list) {
		Integer prev = -1;
		for (Integer i: list) {
			if (i < prev) {
				Assert.fail("bad sort! prev["+prev+"] curr["+i+"]");
			}
			prev = i;
		}
	}
}
