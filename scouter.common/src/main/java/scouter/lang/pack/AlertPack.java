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
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import java.io.IOException;

/**
 * Object that contains one alert information
 */
public class AlertPack implements Pack {

	/**
	 * indicate sign about hash value
	 */
	public static String HASH_FLAG = "_hash_";

	/**
	 * Alert time
	 */
	public long time;
	/**
	 * Object type
	 */
	public String objType;
	/**
	 * Object ID
	 */
	public int objHash;
	/**
	 * Alert level. 0:Info, 1:Warn, 2:Error, 3:Fatal
	 */
	public byte level;
	/**
	 * Alert title
	 */
	public String title;
	/**
	 * Alert message
	 */
	public String message;
	/**
	 * More info
	 */
	public MapValue tags = new MapValue();

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ALERT ");
		sb.append(DateUtil.timestamp(time));
		sb.append(" objType=").append(objType);
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" level=").append(level);
		sb.append(" title=").append(title);
		sb.append(" message=").append(message);
		sb.append(" tags=").append(tags);

		return sb.toString();
	}

	public byte getPackType() {
		return PackEnum.ALERT;
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeLong(time);
		dout.writeByte(level);
		dout.writeText(objType);
		dout.writeInt(objHash);
		dout.writeText(title);
		dout.writeText(message);
		dout.writeValue(tags);
	}

	public Pack read(DataInputX din) throws IOException {
		// this.key = din.readLong();
		this.time = din.readLong();
		this.level = din.readByte();
		this.objType = din.readText();
		this.objHash = din.readInt();
		this.title = din.readText();
		this.message = din.readText();
		this.tags = (MapValue) din.readValue();
		return this;
	}

}
