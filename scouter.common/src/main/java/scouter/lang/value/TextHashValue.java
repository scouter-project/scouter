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

package scouter.lang.value;


import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.HashUtil;


public class TextHashValue implements Value, Comparable {

	public int value;

	public TextHashValue() {
	}

	public TextHashValue(int value) {
		this.value = value;
	}
	public TextHashValue(String str) {
		this.value = HashUtil.hash(str);
	}
	public int compareTo(Object o) {
		if (o instanceof TextHashValue) {
			long thisVal = this.value;
			long anotherVal = ((TextHashValue) o).value;
			return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
		}
		return 1;
	}

	public boolean equals(Object o) {
		if (o instanceof TextHashValue) {
			return this.value == ((TextHashValue) o).value;
		}
		return false;
	}

	public int hashCode() {
		return value;
	}

	public byte getValueType() {
		return ValueEnum.TEXT_HASH;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeInt(value);
	}

	public Value read(DataInputX in) throws IOException {
		this.value = in.readInt();
		return this;
	}

	public String toString() {
		return Integer.toString(value,16);
	}

	public Object toJavaObject() {
		return this.value;
	}
}