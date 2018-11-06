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

package scouter.lang.step;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.SpanPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;

import java.io.IOException;

public class CommonSpanStep extends StepSingle {

	public String nameDebug;

	public int hash;
	public int elapsed;
	public int error;

	public long timestamp;
	public byte spanType;

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

	public byte getStepType() {
		return StepEnum.SPAN;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeDecimal(hash);
		out.writeDecimal(elapsed);
		out.writeDecimal(error);

		out.writeDecimal(timestamp);
		out.writeByte(spanType);

		out.writeDecimal(localEndpointServiceName);
		out.writeBlob(localEndpointIp);
		out.writeShort(localEndpointPort);
		out.writeDecimal(remoteEndpointServiceName);
		out.writeBlob(remoteEndpointIp);
		out.writeShort(remoteEndpointPort);

		out.writeBoolean(debug);
		out.writeBoolean(shared);

		out.writeValue(annotationTimestamps);
		out.writeValue(annotationValues);
		out.writeValue(tags);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.hash = (int) in.readDecimal();
		this.elapsed = (int) in.readDecimal();
		this.error = (int) in.readDecimal();

		this.timestamp = in.readDecimal();
		this.spanType = in.readByte();

		this.localEndpointServiceName = (int) in.readDecimal();
		this.localEndpointIp = in.readBlob();
		this.localEndpointPort = in.readShort();
		this.remoteEndpointServiceName = (int) in.readDecimal();
		this.remoteEndpointIp = in.readBlob();
		this.remoteEndpointPort = in.readShort();

		this.debug = in.readBoolean();
		this.shared = in.readBoolean();

		this.annotationTimestamps = (ListValue) in.readValue();
		this.annotationValues = (ListValue) in.readValue();
		this.tags = (MapValue) in.readValue();
		return this;
	}
}