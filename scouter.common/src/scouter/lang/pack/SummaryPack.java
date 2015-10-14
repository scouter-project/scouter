/*
 *  Copyright 2015 the original author or authors.
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
import scouter.util.DateUtil;
import scouter.util.Hexa32;

public class SummaryPack implements Pack {

	public long time;
	public int objHash;
	public byte stype;
	public int[] id;
	public int[] count;
	public int[] errorCnt;
	public long[] elapsedSum;
	
	//service only
	public long[] cpuTime;
	public long[] memAlloc;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Summary ");
		sb.append(DateUtil.timestamp(time));
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" stype=").append(stype);
		sb.append(" id(").append(ArrayUtil.len(id));
		sb.append(") count(").append(ArrayUtil.len(count));
		sb.append(") errorCnt(").append(ArrayUtil.len(errorCnt));
		sb.append(") elapsedSum(").append(ArrayUtil.len(elapsedSum));
		sb.append(")");
		return sb.toString();
	}

	public byte getPackType() {
		return PackEnum.SUMMARY;
	}

	public void write(DataOutputX out) throws IOException {
		DataOutputX o = new DataOutputX();

		o.writeDecimal(time);
		o.writeInt(objHash);
		o.writeByte(stype);
		
		o.writeArray(id);
		o.writeArray(count);
		o.writeArray(errorCnt);
		o.writeDecimalArray(elapsedSum);
		
		o.writeDecimalArray(cpuTime);
		o.writeDecimalArray(memAlloc);
		
		out.writeBlob(o.toByteArray());
	}

	public Pack read(DataInputX din) throws IOException {

		DataInputX n = new DataInputX(din.readBlob());

		this.time = n.readDecimal();
		this.objHash = n.readInt();
		this.stype = n.readByte();
		this.id = n.readArray(new int[0]);
		this.count = n.readArray(new int[0]);
		this.errorCnt =n.readArray(new int[0]);
		this.elapsedSum = n.readDecimalArray();
        //
		this.cpuTime = n.readDecimalArray();
		this.memAlloc = n.readDecimalArray();
		
		return this;
	}
     
}