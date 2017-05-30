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
import java.util.Arrays;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.CompareUtil;


public class FloatArray  implements Value, Comparable {

	public float[] value;

	public FloatArray() {
	}

	public FloatArray(float[] value) {
		this.value = value;
	}

	public int compareTo(Object o) {
		if (o instanceof FloatArray) {
			return CompareUtil.compareTo(this.value, ((FloatArray) o).value);
		}
		return 1;
	}

	public boolean equals(Object o) {
		if (o instanceof FloatArray) {
			return Arrays.equals(this.value,((FloatArray) o).value);
		}
		return false;
	}

	private int _hash;
	public int hashCode() {
		if(_hash==0){
			_hash= Arrays.hashCode(this.value);
		}
		return _hash;
	}

	public byte getValueType() {
		return ValueEnum.ARRAY_FLOAT;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeArray(value);
	}

	public Value read(DataInputX in) throws IOException {
		this.value = in.readArray(new float[0]);
		return this;
	}

	public String toString() {
		return Arrays.toString(value);
	}

	public Object toJavaObject() {
		return this.value;
	}
}