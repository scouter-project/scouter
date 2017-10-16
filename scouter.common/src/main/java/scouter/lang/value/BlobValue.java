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
import scouter.util.CompareUtil;


public class BlobValue implements Value, Comparable {

	public byte[] value;

	public BlobValue() {
	}

	public BlobValue(byte[] value) {
		this.value = (value == null ? new byte[0] : value);
	}

	public int compareTo(Object o) {
		if (o instanceof BlobValue) {
			if (this.value == null) {
				return ((BlobValue) o).value == null ? 0 : -1;
			}
			return CompareUtil.compareTo(this.value, ((BlobValue) o).value);
		}
		return 1;
	}

	public boolean equals(Object o) {
		if (o instanceof BlobValue) {
			if (this.value == null) {
				return ((BlobValue) o).value == null;
			}
			return this.value.equals(((BlobValue) o).value);
		}
		return false;
	}

	public int hashCode() {
		if (this.value == null)
			return 0;
		return this.value.hashCode();
	}

	public byte getValueType() {
		return ValueEnum.BLOB;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeBlob(value);
	}

	public Value read(DataInputX in) throws IOException {
		this.value = in.readBlob();
		return this;
	}

	public String toString() {
		if (value == null)
			return null;
		return "byte[" + value.length + "]";
	}

	public Object toJavaObject() {
		return this.value;
	}
}