/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.util;


public class CompareUtil {
	public static int compareTo(byte[] l, byte[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		for (int i = 0; i < l.length && i < r.length; i++) {
			if (l[i] > r[i])
				return 1;
			if (l[i] < r[i])
				return -1;
		}
		return l.length - r.length;
	}

	public static int compareTo(short[] l, short[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		for (int i = 0; i < l.length && i < r.length; i++) {
			if (l[i] > r[i])
				return 1;
			if (l[i] < r[i])
				return -1;
		}
		return l.length - r.length;
	}

	public static int compareTo(int[] l, int[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		for (int i = 0; i < l.length && i < r.length; i++) {
			if (l[i] > r[i])
				return 1;
			if (l[i] < r[i])
				return -1;
		}
		return l.length - r.length;
	}

	public static int compareTo(float[] l, float[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		for (int i = 0; i < l.length && i < r.length; i++) {
			if (l[i] > r[i])
				return 1;
			if (l[i] < r[i])
				return -1;
		}
		return l.length - r.length;
	}

	public static int compareTo(long[] l, long[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		for (int i = 0; i < l.length && i < r.length; i++) {
			if (l[i] > r[i])
				return 1;
			if (l[i] < r[i])
				return -1;
		}
		return l.length - r.length;
	}

	public static int compareTo(double[] l, double[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;

		for (int i = 0; i < l.length && i < r.length; i++) {
			if (l[i] > r[i])
				return 1;
			if (l[i] < r[i])
				return -1;
		}
		return l.length - r.length;
	}

	public static int compareTo(Comparable[] l, Comparable[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		for (int i = 0; i < l.length && i < r.length; i++) {
			int c = compareTo(l[i], r[i]);
			if (c != 0)
				return c;
		}
		return l.length - r.length;
	}
	public static int compareTo(String[] l, String[] r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		for (int i = 0; i < l.length && i < r.length; i++) {
			int c = compareTo(l[i], r[i]);
			if (c != 0)
				return c;
		}
		return l.length - r.length;
	}
	public static boolean equals(byte[] l, byte[] r) {
		return compareTo(l, r) == 0;
	}

	public static boolean equals(short[] l, short[] r) {
		return compareTo(l, r) == 0;
	}

	public static boolean equals(int[] l, int[] r) {
		return compareTo(l, r) == 0;
	}

	public static boolean equals(float[] l, float[] r) {
		return compareTo(l, r) == 0;
	}

	public static boolean equals(long[] l, long[] r) {
		return compareTo(l, r) == 0;
	}

	public static boolean equals(double[] l, double[] r) {
		return compareTo(l, r) == 0;
	}

	public static boolean equals(String[] l, String[] r) {
		return compareTo(l, r) == 0;
	}

	public static int compareTo(String l, String r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		return l.compareTo(r);
	}
	public static int compareTo(Comparable l, Comparable r) {
		if (l == null && r == null)
			return 0;
		if (l == null)
			return -1;
		if (r == null)
			return 1;
		return l.compareTo(r);
	}

	public static boolean equals(String l, String r) {
		if (l == null)
			return r == null;
		else
			return l.equals(r);
	}
	public static boolean equals(Object l, Object r) {
		if (l == null)
			return r == null;
		else
			return l.equals(r);
	}
	public static boolean equals(long l, long r) {
		return l==r;
	}
	public static boolean equals(int l, int r) {
		return l==r;
	}

}