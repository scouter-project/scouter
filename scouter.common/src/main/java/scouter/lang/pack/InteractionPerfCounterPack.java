/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.lang.pack;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.value.FloatValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.NumberValue;
import scouter.lang.value.Value;

import java.io.IOException;

/**
 * Object that contains counter information
 */
public class InteractionPerfCounterPack implements Pack {

	/**
	 * Counter time
	 */
	public long time;
	/**
	 * Object name
	 */
	public String objName;
	/**
	 * Interaction type @ CounterConstants
	 */
	public String interactionType;

	public int fromHash;
	public int toHash;

	public int period;
	public int count;
	public int errorCount;
	public long totalElapsed;

	public MapValue customData = new MapValue();

	public InteractionPerfCounterPack() {
	}

	public InteractionPerfCounterPack(String objName, String interactionType) {
		this.objName = objName;
		this.interactionType = interactionType;
	}

	public byte getPackType() {
		return PackEnum.PERF_INTERACTION_COUNTER;
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeLong(time);
		dout.writeText(objName);
		dout.writeText(interactionType);
		dout.writeInt(fromHash);
		dout.writeInt(toHash);
		dout.writeInt(period);
		dout.writeInt(count);
		dout.writeInt(errorCount);
		dout.writeLong(totalElapsed);
		dout.writeValue(customData);
	}

	public Pack read(DataInputX din) throws IOException {
		this.time = din.readLong();
		this.objName = din.readText();
		this.interactionType = din.readText();
		this.fromHash = din.readInt();
		this.toHash = din.readInt();
		this.period = din.readInt();
		this.count = din.readInt();
		this.errorCount = din.readInt();
		this.totalElapsed = din.readLong();
		this.customData = (MapValue) din.readValue();
		return this;
	}

    public void putCustom(String key, Object o) {
	    if (o instanceof Number) {
            this.customData.put(key, new FloatValue(((Number) o).floatValue()));
        }
    }

	public void putCustom(String key, Value value) {
		this.customData.put(key, value);
	}

	public void addCustom(String key, NumberValue value) {
		Value old = this.customData.get(key);
		if (old == null) {
			this.customData.put(key, value);
		} else if (old instanceof NumberValue) {
			((NumberValue) old).add(value);
		}
	}

	@Override
	public String toString() {
		return "PerfInteractionCounterPack{" +
				"time=" + time +
				", objName='" + objName + '\'' +
				", interactionType='" + interactionType + '\'' +
				", fromHash='" + fromHash + '\'' +
				", toHash='" + toHash + '\'' +
				", period=" + period +
				", count=" + count +
				", errorCount=" + errorCount +
				", totalElapsed=" + totalElapsed +
				", customData=" + customData +
				'}';
	}
}