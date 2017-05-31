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
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

public class StatusPack implements Pack {

	public long time;
	public String objType;
	public int objHash;
	public String key;
	public MapValue data = new MapValue();

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Status ");
		sb.append(DateUtil.timestamp(time));
		sb.append(" objType=").append(objType);
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" key=").append(key);
		sb.append(" data=").append(data);
		return sb.toString();
	}

	public byte getPackType() {
		return PackEnum.PERF_STATUS;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeDecimal(time);
		out.writeText(objType);
		out.writeDecimal(objHash);
		out.writeText(key);
		out.writeValue(data);
	}

	public Pack read(DataInputX in) throws IOException {

		this.time = in.readDecimal();
		this.objType = in.readText();
		this.objHash = (int) in.readDecimal();
		this.key = in.readText();
		this.data = (MapValue) in.readValue();

		return this;
	}
	
	public ListValue newList(String name) {
		ListValue list = new ListValue();
		data.put(name, list);
		return list;
	}
	
	public MapPack toMapPack() {
		MapPack pack = new MapPack();
		for(String key : data.keySet()) {
			pack.put(key, data.get(key));
		}
		return pack;
	}
}