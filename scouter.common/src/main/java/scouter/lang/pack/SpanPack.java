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
import scouter.util.IPUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	public int objHash;
	public int error;

	public int localEndpointServiceName;
	public byte[] localEndpointIp;
	public short localEndpointPort;

	public int remoteEndpointServiceName;
	public byte[] remoteEndpointIp;
	public short remoteEndpointPort;

	public boolean debug;
	public boolean shared;

	public ListValue annotationTimestamps;
	public ListValue annotationValues;

	public MapValue tags;

	public byte getPackType() {
		return PackEnum.SPAN;
	}

	@Override
	public String toString() {
		return "SpanPack{" +
				"gxid=" + gxid +
				", txid=" + txid +
				", caller=" + caller +
				", timestamp=" + timestamp +
				", elapsed=" + elapsed +
				", spanType=" + spanType +
				", name=" + name +
				", objHash=" + objHash +
				", error=" + error +
				", localEndpointServiceName=" + localEndpointServiceName +
				", localEndpointIp=" + IPUtil.toString(localEndpointIp) +
				", localEndpointPort=" + localEndpointPort +
				", remoteEndpointServiceName=" + remoteEndpointServiceName +
				", remoteEndpointIp=" + IPUtil.toString(remoteEndpointIp) +
				", remoteEndpointPort=" + remoteEndpointPort +
				", debug=" + debug +
				", shared=" + shared +
				", annotationTimestamps=" + annotationTimestamps +
				", annotationValues=" + annotationValues +
				", tags=" + tags +
				'}';
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeLong(gxid);
		dout.writeLong(txid);
		dout.writeLong(caller);
		dout.writeLong(timestamp);
		dout.writeDecimal(elapsed);
		dout.writeByte(spanType);
		dout.writeDecimal(name);
		dout.writeDecimal(objHash);
		dout.writeDecimal(error);

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
		this.objHash = (int) din.readDecimal();
		this.error = (int) din.readDecimal();

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

	public static byte[] toBytes(SpanPack[] pack) {
		if (pack == null)
			return null;

		try {
			DataOutputX dout = new DataOutputX(pack.length * 300);
			for (int i = 0; i < pack.length; i++) {
				dout.writePack(pack[i]);
			}
			return dout.toByteArray();

		} catch (IOException e) {
		}

		return null;
	}

	public static byte[] toBytes(List<SpanPack> packs) {
		if (packs == null)
			return null;

		try {
			int size = packs.size();
			DataOutputX dout = new DataOutputX(size * 300);
			for (int i = 0; i < size; i++) {
				dout.writePack(packs.get(i));
			}
			return dout.toByteArray();

		} catch (IOException e) {
		}
		return null;
	}

	public static List<byte[]> toBytesList(List<SpanPack> packs, int maxBytes) {
		if (packs == null)
			return null;

		try {
			List<byte[]> byteResultList = new ArrayList<byte[]>();

			int maxLen = Math.max(maxBytes - 18000, 18000);
			int size = packs.size();
			DataOutputX dout = new DataOutputX(Math.min(size * 500, maxBytes));

			for (SpanPack pack : packs) {
				dout.writePack(pack);
				if (dout.getWriteSize() > maxLen) {
					byteResultList.add(dout.toByteArray());
					dout = new DataOutputX(Math.min(size * 500, maxBytes));
				}
			}

			if (dout.getWriteSize() > 0) {
				byteResultList.add(dout.toByteArray());
			}
			return byteResultList;

		} catch (IOException e) {
		}
		return null;
	}

	public static SpanPack[] toObjects(byte[] buff) throws IOException {
		if (buff == null)
			return null;

		ArrayList<SpanPack> arr = new ArrayList<SpanPack>();
		DataInputX din = new DataInputX(buff);
		while (din.available() > 0) {
			arr.add((SpanPack) din.readPack());
		}
		return (SpanPack[]) arr.toArray(new SpanPack[arr.size()]);
	}

	public static List<SpanPack> toObjectList(byte[] buff) {
		if (buff == null)
			return null;

		ArrayList<SpanPack> arr = new ArrayList<SpanPack>();
		DataInputX din = new DataInputX(buff);
		try {
			while (din.available() > 0) {
				arr.add((SpanPack) din.readPack());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return arr;
	}
}