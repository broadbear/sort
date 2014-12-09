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
			l.add(r.nextInt(100000));
		}
		return l;
	}

	public static int[] createArray(int n) {
		int[] l = new int[n];
		Random r = new Random();
		r.setSeed(123);
		for (int i = 0; i < n; i++) {
			l[i] = r.nextInt(100000);
		}
		return l;
	}

	public static void verify(List<Integer> list) {
		Integer prev = -1;
		for (int i = 0; i < list.size(); i++) {
			Integer curr = list.get(i);
			if (curr < prev) {
				Assert.fail("bad sort! index["+i+"] prev["+prev+"] curr["+curr+"]");
			}
			prev = curr;
		}
	}
	
	public static void verify(int[] list) {
		Integer prev = -1;
		for (int i = 0; i < list.length; i++) {
			Integer curr = list[i];
			if (curr < prev) {
				Assert.fail("bad sort! index["+i+"] prev["+prev+"] curr["+curr+"]");
			}
			prev = curr;
		}
	}
}
