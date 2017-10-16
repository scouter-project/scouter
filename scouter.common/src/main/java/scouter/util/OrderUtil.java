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

public class OrderUtil {

	public static Order asc(Comparable a, Comparable b) {
		int cmp = a.compareTo(b);
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.OK;
		return Order.REVERSE;
	}

	public static Order asc(int a, int b) {
		int cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.OK;
		return Order.REVERSE;
	}

	public static Order asc(long a, long b) {
		long cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.OK;
		return Order.REVERSE;
	}

	public static Order asc(double a, double b) {
		double cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.OK;
		return Order.REVERSE;
	}

	public static Order asc(float a, float b) {
		float cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.OK;
		return Order.REVERSE;
	}

	public static Order desc(Comparable a, Comparable b) {
		int cmp = a.compareTo(b);
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.REVERSE;
		return Order.OK;
	}

	public static Order desc(int a, int b) {
		int cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.REVERSE;
		return Order.OK;
	}

	public static Order desc(long a, long b) {
		long cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.REVERSE;
		return Order.OK;
	}

	public static Order desc(double a, double b) {
		double cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.REVERSE;
		return Order.OK;
	}

	public static Order desc(float a, float b) {
		float cmp = a - b;
		if (cmp == 0)
			return Order.EQUAL;
		if (cmp < 0)
			return Order.REVERSE;
		return Order.OK;
	}
}