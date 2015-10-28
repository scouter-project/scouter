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

package scouter.server.tagcnt.core;

import java.util.TreeSet;

import scouter.lang.value.DecimalValue;
import scouter.lang.value.Value;

public class ValueCount implements Comparable<ValueCount> {

	public ValueCount(Value tagValue, double valueCount) {
		this.tagValue = tagValue;
		this.valueCount = valueCount;
	}

	public Value tagValue;
	public double valueCount;

	public String toString() {
		return "[tagValue=" + tagValue + ", valueCount=" + valueCount + "]";
	}

	public int compareTo(ValueCount o) {
		ValueCount v = (ValueCount) o;
		if (this.valueCount == v.valueCount)
			return 0;
		return valueCount > v.valueCount ? 1 : -1;
	}

	public static void main(String[] args) {
		TreeSet<ValueCount> t = new TreeSet<ValueCount>();
		t.add(new ValueCount(new DecimalValue(1), 1.0));
		t.add(new ValueCount(new DecimalValue(2), 2.0));
		System.out.println(t);
	}
}
