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
import scouter.util.IPUtil;

public class IP4Value implements Value, Comparable {

	private final static byte[] empty = new byte[4];
	public byte[] value = empty;

	public IP4Value() {
	}

	public IP4Value(String ip) {
		this.value = IPUtil.toBytes(ip);
	}

	public IP4Value(byte[] value) {
		this.value = value;
	}

	public int compareTo(Object o) {
		if (o instanceof IP4Value) {
			byte[] thisVal = this.value;
			byte[] anotherVal = ((IP4Value) o).value;
			return CompareUtil.compareTo(thisVal, anotherVal);
		}
		return 1;
	}

	public boolean equals(Object o) {
		if (o instanceof IP4Value) {
		
			byte[] thisVal = this.value;
			byte[] anotherVal = ((IP4Value) o).value;
			if(thisVal==null || anotherVal==null)
				return thisVal==anotherVal;
			
			for (int i = 0; i < 4; i++) {
				if (thisVal[i] != anotherVal[i])
					return false;
			}
			return true;
		}
		return false;
	}

	public int hashCode() {
		if (value == null || value == empty)
			return 0;
		return DataInputX.toInt(value, 0);
	}

	public static void main(String[] args) {
		System.out.println(new IP4Value().hashCode());
	}

	public byte getValueType() {
		return ValueEnum.IP4ADDR;
	}

	public void write(DataOutputX out) throws IOException {
		if (value == null)
			value = empty;
		out.write(value);
	}

	public Value read(DataInputX in) throws IOException {
		if (value == null)
			value = empty;
		this.value = in.read(4);
		return this;
	}

	public String toString() {
		if (value == null)
			value = empty;
		return IPUtil.toString(value);
	}

	public Object toJavaObject() {
		if (value == null)
			value = empty;
		return IPUtil.toString(value);
	}
}