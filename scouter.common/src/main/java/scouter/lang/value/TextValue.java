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


public class TextValue implements Value, Comparable {

	public String value;

	public TextValue() {
	}

	public TextValue(String value) {
		this.value = (value == null ? "" : value);
	}

	public int compareTo(Object o) {
		if (o instanceof TextValue) {
			if (this.value == null) {
				return ((TextValue) o).value == null ? 0 : -1;
			}
			return this.value.compareTo(((TextValue) o).value);
		}
		return 1;
	}

	public boolean equals(Object o) {
		if (o instanceof TextValue) {
			if (this.value == null) {
				return ((TextValue) o).value == null;
			}
			return this.value.equals(((TextValue) o).value);
		}
		return false;
	}

	public int hashCode() {
		if (this.value == null)
			return 0;
		return this.value.hashCode();
	}

	public byte getValueType() {
		return ValueEnum.TEXT;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeText(value);
	}

	public Value read(DataInputX in) throws IOException {
		this.value = in.readText();
		return this;
	}

	public String toString() {
		return value;
	}


	public Object toJavaObject() {
		return this.value;
	}
}