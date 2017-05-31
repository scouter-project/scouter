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


public class FloatValue extends NumberValue  implements Value, Comparable {

	public float value;

	public FloatValue() {
	}

	public FloatValue(float value) {
		this.value = value;
	}

	public int compareTo(Object o) {
		if (o instanceof FloatValue) {
			return Float.compare(this.value, ((FloatValue) o).value);
		}
		return 1;
	}

	public boolean equals(Object o) {
		if (o instanceof FloatValue) {
			return this.value == ((FloatValue) o).value;
		}
		return false;
	}

	public int hashCode() {
		return Float.floatToIntBits(value);
	}

	public byte getValueType() {
		return ValueEnum.FLOAT;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeFloat(value);
	}

	public Value read(DataInputX in) throws IOException {
		this.value = in.readFloat();
		return this;
	}

	public String toString() {
		return Float.toString(value);
	}


	// ////////////////////////////////
	public double doubleValue() {
		return value;
	}

	public float floatValue() {
		return (float) value;
	}

	public int intValue() {
		return (int) value;
	}

	public long longValue() {
		return (long) value;
	}

	public Object toJavaObject() {
		return this.value;
	}
}