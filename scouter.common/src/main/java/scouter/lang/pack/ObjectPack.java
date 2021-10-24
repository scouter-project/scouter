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

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.value.MapValue;
import scouter.util.Hexa32;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * Object that contains one agent(called object) information
 */
public class ObjectPack implements Pack {

	public static final String TAG_KEY_DEAD_TIME = "deadTime";

	/**
	 * Object type
	 */
	public String objType;
	/**
	 * Object ID
	 */
	public int objHash;
	/**
	 * Object full name
	 */
	public String objName;
	/**
	 * IP address
	 */
	public String address;
	/**
	 * Version
	 */
	public String version;
	/**
	 * Whether alive
	 */
	public boolean alive = true;
	/**
	 * Last wake up time
	 */
	public long wakeup;
	/**
	 * More info
	 */
	public MapValue tags = new MapValue();

	//internal use

	//------  for kube support
	public transient long initialTime;
	public transient boolean allowProceed;
	public transient boolean pushSeq;
	public transient String podName;
	public transient String hostName;
	public transient int podSeq;
	//-----------------------------

	public transient int updated;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("OBJECT ");
		sb.append(" objType=").append(objType);
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" objName=").append(objName);
		if (isOk(address))
			sb.append(" addr=").append(address);
		if (isOk(version))
			sb.append(" ").append(version);
		if (alive)
			sb.append(" alive");
		if (wakeup > 0)
			sb.append(" ").append(new Timestamp(wakeup));
		if (tags.size() > 0)
			sb.append(" ").append(tags);
		return sb.toString();
	}

	private boolean isOk(String s) {
		return s != null && s.length() > 0;
	}

	public void wakeup() {
		this.wakeup = System.currentTimeMillis();
		this.alive = true;
	}

	public void setDeadTime(int deadTime) {
		this.tags.put(TAG_KEY_DEAD_TIME, deadTime);
	}

	public int getDeadTime() {
		return tags.getInt(TAG_KEY_DEAD_TIME);
	}

	public byte getPackType() {
		return PackEnum.OBJECT;
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeText(objType);
		dout.writeDecimal(objHash);
		dout.writeText(objName);
		dout.writeText(address);
		dout.writeText(version);
		dout.writeBoolean(alive);
		dout.writeDecimal(wakeup);
		dout.writeValue(tags);
	}

	public Pack read(DataInputX din) throws IOException {
		this.objType = din.readText();
		this.objHash = (int) din.readDecimal();
		this.objName = din.readText();
		this.address = din.readText();
		this.version = din.readText();
		this.alive = din.readBoolean();
		this.wakeup = din.readDecimal();
		this.tags = (MapValue) din.readValue();
		return this;
	}

	public int hashCode() {
		return objHash;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return (objHash == ((ObjectPack) obj).objHash);
	}

	public void copyKubePropFrom(ObjectPack initialPack) {
		this.initialTime = initialPack.initialTime;
		this.podName = initialPack.podName;
		this.hostName = initialPack.hostName;
		this.podSeq = initialPack.podSeq;
	}

	public void addKubeProp(String podName, String hostName, int podSeq) {
		this.podName = podName;
		this.hostName = hostName;
		this.podSeq = podSeq;
	}
}
