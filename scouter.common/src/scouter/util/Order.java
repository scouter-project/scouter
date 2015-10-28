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

public enum Order {
	REVERSE, EQUAL, OK;

	public Order asc(Comparable a, Comparable b) {
		if (this != EQUAL)
			return this;

		int cmp = a.compareTo(b);
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return OK;
		return REVERSE;
	}

	public Order asc(int a, int b) {
		if (this != EQUAL)
			return this;

		int cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return OK;
		return REVERSE;
	}

	public Order asc(long a, long b) {
		if (this != EQUAL)
			return this;

		long cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return OK;
		return REVERSE;
	}

	public Order asc(double a, double b) {
		if (this != EQUAL)
			return this;

		double cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return OK;
		return REVERSE;
	}

	public Order asc(float a, float b) {
		if (this != EQUAL)
			return this;

		float cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return OK;
		return REVERSE;
	}

	public Order desc(Comparable a, Comparable b) {
		if (this != EQUAL)
			return this;

		int cmp = a.compareTo(b);
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return REVERSE;
		return OK;
	}

	public Order desc(int a, int b) {
		if (this != EQUAL)
			return this;

		int cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return REVERSE;
		return OK;
	}

	public Order desc(long a, long b) {
		if (this != EQUAL)
			return this;

		long cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return REVERSE;
		return OK;
	}

	public Order desc(double a, double b) {
		if (this != EQUAL)
			return this;

		double cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return REVERSE;
		return OK;
	}

	public Order desc(float a, float b) {
		if (this != EQUAL)
			return this;

		float cmp = a - b;
		if (cmp == 0)
			return EQUAL;
		if (cmp < 0)
			return REVERSE;
		return OK;
	}
}