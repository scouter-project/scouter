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
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

import java.io.IOException;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 27/10/2018
 */
public class SpanPack implements Pack {

	public long gxid;
	public long txid;
	public long caller;
	public long timestamp;
	public int elapsed;
	public byte spanType;
	public int name;

	public int localEndpointServiceName;
	public byte[] localEndpointIp;
	public short localEndpointPort;

	public int remoteEndpointServiceName;
	public byte[] remoteEndpointIp;
	public short remoteEndpointPort;

	boolean debug;
	boolean shared;

	ListValue annotationTimestamps;
	ListValue annotationValues;

	MapValue tags;

	public byte getPackType() {
		return PackEnum.SPAN;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SpanPack ");
		sb.append(DateUtil.timestamp(timestamp));
		sb.append(" name=").append(Hexa32.toString32(name));
		sb.append(" gxid=").append(Hexa32.toString32(gxid));
		sb.append(" txid=").append(Hexa32.toString32(txid));
		sb.append(" caller=").append(Hexa32.toString32(caller));
		sb.append(" spanType=").append(Hexa32.toString32(spanType));
		sb.append(" elapsed=").append(Hexa32.toString32(elapsed));
		return sb.toString();
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeLong(gxid);
		dout.writeLong(txid);
		dout.writeLong(caller);
		dout.writeLong(timestamp);
		dout.writeDecimal(elapsed);
		dout.writeByte(spanType);
		dout.writeDecimal(name);

		dout.writeDecimal(localEndpointServiceName);
		dout.writeBlob(localEndpointIp);
		dout.writeShort(localEndpointPort);
		dout.writeDecimal(remoteEndpointServiceName);
		dout.writeBlob(remoteEndpointIp);
		dout.writeShort(remoteEndpointPort);

		dout.writeBoolean(debug);
		dout.writeBoolean(shared);

		dout.writeValue(annotationTimestamps);
		dout.writeValue(annotationValues);
		dout.writeValue(tags);
	}

	public Pack read(DataInputX din) throws IOException {
		this.gxid = din.readLong();
		this.txid = din.readLong();
		this.caller = din.readLong();
		this.timestamp = din.readLong();
		this.elapsed = (int) din.readDecimal();
		this.spanType = din.readByte();
		this.name = (int) din.readDecimal();

		this.localEndpointServiceName = (int) din.readDecimal();
		this.localEndpointIp = din.readBlob();
		this.localEndpointPort = din.readShort();
		this.remoteEndpointServiceName = (int) din.readDecimal();
		this.remoteEndpointIp = din.readBlob();
		this.remoteEndpointPort = din.readShort();

		this.debug = din.readBoolean();
		this.shared = din.readBoolean();

		this.annotationTimestamps = (ListValue) din.readValue();
		this.annotationValues = (ListValue) din.readValue();
		this.tags = (MapValue) din.readValue();

		return this;
	}

}