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
		out.writeDecimal(time);
		out.writeInt(objHash);
		out.writeByte(stype);
		int cnt = ArrayUtil.len(id);
		out.writeDecimal(cnt);
		for (int i = 0; i < cnt; i++) {
			out.writeInt(id[i]);
			out.writeInt(count[i]);
			out.writeDecimal(errorCnt[i]);
			out.writeDecimal(elapsedSum[i]);
		}
	}

	public Pack read(DataInputX in) throws IOException {

		this.time = in.readDecimal();
		this.objHash = in.readInt();
		this.stype = in.readByte();
		int cnt = (int) in.readDecimal();
		if (cnt == 0)
			return this;
		this.id = new int[cnt];
		this.count = new int[cnt];
		this.errorCnt = new int[cnt];
		this.elapsedSum = new long[cnt];
		for (int i = 0; i < cnt; i++) {
			this.id[i] = in.readInt();
			this.count[i] = in.readInt();
			this.errorCnt[i] = (int) in.readDecimal();
			this.elapsedSum[i] = in.readDecimal();
		}

		return this;
	}

}