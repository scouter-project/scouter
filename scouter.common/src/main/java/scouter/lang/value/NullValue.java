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


public class NullValue implements Value {
	public static NullValue value = new NullValue();

	public int compareTo(Object o) {
		if (o instanceof NullValue) {
			return 0;
		}
		return 1;
	}

	public boolean equals(Object o) {
		return (o instanceof NullValue);
	}

	public int hashCode() {
		return 0;
	}

	public byte getValueType() {
		return ValueEnum.NULL;
	}

	public void write(DataOutputX out) throws IOException {
	}

	public Value read(DataInputX in) throws IOException {
		return this;
	}

	public String toString() {
		return null;
	}

	public Object toJavaObject() {
		return null;
	}

}