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

package scouter.lang;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;

public class CounterKey implements Comparable {

	public int objHash;
	public String counter;
	public byte timetype;

	private CounterKey() {
	}

	public CounterKey(int objHash, String counter, byte timetype) {
		this.objHash = objHash;
		this.counter = (counter == null ? "" : counter);
		this.timetype = timetype;
	}

	public int compareTo(Object obj) {
		if (obj instanceof CounterKey) {
			CounterKey o = (CounterKey) obj;
			if (objHash != o.objHash)
				return objHash - o.objHash;
			int x = counter.compareTo(o.counter);
			return x != 0 ? x : timetype - o.timetype;

		}
		return 1;
	}

	public boolean equals(Object obj) {
		if (obj instanceof CounterKey) {
			CounterKey o = (CounterKey) obj;
			return objHash == o.objHash && counter.equals(o.counter) && timetype == o.timetype;
		}
		return false;
	}

	public int hashCode() {
		return objHash ^ counter.hashCode() ^ timetype;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" objHash=").append(objHash);
		sb.append(" counter=").append(counter);
		sb.append(" timetype=").append(TimeTypeEnum.get(timetype));
		return sb.toString();
	}

	public byte[] getBytesKey() {
		try {
			DataOutputX out = new DataOutputX();
			out.writeInt(objHash);
			out.writeText(counter);
			out.writeByte(timetype);
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static CounterKey toCounterKey(byte[] param) {
		try {
			DataInputX in = new DataInputX(param);
			CounterKey ck = new CounterKey();
			ck.objHash = in.readInt();
			ck.counter = in.readText();
			ck.timetype = in.readByte();
			return ck;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// public void setBytesKey(byte[] cntKey) {
	// try {
	// DataInputX in = new DataInputX(cntKey);
	// this.objHash = in.readInt();
	// this.counter = in.readShort();
	// this.timetype = in.readByte();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// }
	// }

	public static MapPack toMapPacket(CounterKey key) {
		MapPack param = new MapPack();
		param.put("objHash", key.objHash);
		param.put("counter", key.counter);
		param.put("timetype", key.timetype);
		return param;
	}

	public static CounterKey toCounterKey(MapPack param) {
		CounterKey ck = new CounterKey();
		ck.objHash = param.getInt("objHash");
		ck.counter = param.getText("counter");
		ck.timetype = (byte) param.getInt("timetype");
		return ck;
	}
}