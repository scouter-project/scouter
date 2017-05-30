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

package scouter.lang.pack;

import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.ArrayUtil;
import scouter.util.CompressUtil;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

public class StackPack implements Pack {

	public long time;
	public int objHash;
	public byte[] data;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Stack ");
		sb.append(DateUtil.timestamp(time));
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" stack=").append(ArrayUtil.len(data) + "bytes");
		return sb.toString();
	}

	public byte getPackType() {
		return PackEnum.STACK;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeDecimal(time);
		out.writeDecimal(objHash);
		out.writeBlob(data);
	}

	public Pack read(DataInputX in) throws IOException {

		this.time = in.readDecimal();
		this.objHash = (int) in.readDecimal();
		this.data = in.readBlob();

		return this;
	}

	public void setStack(String stack) {
		if (stack == null) {
			this.data = null;
			return;
		}

		try {
			this.data = CompressUtil.doZip(stack.getBytes());
		} catch (Exception e) {
		}
	}

	public String getStack() {
		if (ArrayUtil.isEmpty(data))
			return "";
		try {
			return new String(CompressUtil.unZip(data));
		} catch (Exception e) {
			return "";
		}
	}
}