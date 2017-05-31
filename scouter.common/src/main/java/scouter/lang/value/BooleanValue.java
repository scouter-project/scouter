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


public class BooleanValue implements Value, Comparable {

	public boolean value;

	public BooleanValue() {
	}

	public BooleanValue(boolean value) {
		this.value = value;
	}

	public int compareTo(Object o) {
		if (o instanceof BooleanValue) {
			boolean thisVal = this.value;
			boolean anotherVal = ((BooleanValue) o).value;
			if (thisVal == anotherVal)
				return 0;
			return thisVal ? 1 : -1;
		}
		return 1;
	}

	public boolean equals(Object o) {
		if (o instanceof BooleanValue) {
			return this.value == ((BooleanValue) o).value;
		}
		return false;
	}

	public int hashCode() {
		return value ? 1 : 0;
	}

	public byte getValueType() {
		return ValueEnum.BOOLEAN;
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeBoolean(value);
	}

	public Value read(DataInputX din) throws IOException {
		this.value = din.readBoolean();
		return this;
	}

	public String toString() {
		return Boolean.toString(value);
	}

	public Object toJavaObject() {
		return this.value;
	}
}