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
import scouter.lang.TimeTypeEnum;
import scouter.lang.value.FloatValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.NumberValue;
import scouter.lang.value.Value;
import scouter.util.DateUtil;

/**
 * Object that contains multiple counter information
 */
public class PerfCounterPack implements Pack {

	/**
	 * Counter time
	 */
	public long time;
	/**
	 * Object name
	 */
	public String objName;
	/**
	 * Time type. 1:Real-time, 2:OneMin, 3:FiveMin, 4:TenMin, 5:Hour, 6:Day
	 */
	public byte timetype;
	/**
	 * Multiple counter data. Key is counter name. ref.)scouter.lang.value.MapValue
	 */
	public MapValue data = new MapValue();

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("PerfCounter ").append(DateUtil.timestamp(time));
		buf.append(" ").append(objName);
		buf.append(" ").append(TimeTypeEnum.getString(timetype));
		buf.append(" ").append(data);
		return buf.toString();
	}

	public byte getPackType() {
		return PackEnum.PERF_COUNTER;
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeLong(time);
		dout.writeText(objName);
		dout.writeByte(timetype);
		dout.writeValue(data);
	}

	public Pack read(DataInputX din) throws IOException {
		this.time = din.readLong();
		this.objName = din.readText();
		this.timetype = din.readByte();
		this.data = (MapValue) din.readValue();
		return this;
	}

    public void put(String key, Object o) {
	    if (o instanceof Number) {
            this.data.put(key, new FloatValue(((Number) o).floatValue()));
        }
    }

	public void put(String key, Value value) {
		this.data.put(key, value);
	}

	public void add(String key, NumberValue value) {
		Value old = this.data.get(key);
		if (old == null) {
			this.data.put(key, value);
		} else if (old instanceof NumberValue) {
			((NumberValue) old).add(value);
		}
	}
}