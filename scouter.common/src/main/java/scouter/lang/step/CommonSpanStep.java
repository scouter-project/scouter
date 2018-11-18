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
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.util.DateUtil;
import scouter.util.IPUtil;

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

	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public int getElapsed() {
		return elapsed;
	}

	public void setElapsed(int elapsed) {
		this.elapsed = elapsed;
	}

	public int getError() {
		return error;
	}

	public void setError(int error) {
		this.error = error;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public byte getSpanType() {
		return spanType;
	}

	public void setSpanType(byte spanType) {
		this.spanType = spanType;
	}

	public int getLocalEndpointServiceName() {
		return localEndpointServiceName;
	}

	public void setLocalEndpointServiceName(int localEndpointServiceName) {
		this.localEndpointServiceName = localEndpointServiceName;
	}

	public byte[] getLocalEndpointIp() {
		return localEndpointIp;
	}

	public void setLocalEndpointIp(byte[] localEndpointIp) {
		this.localEndpointIp = localEndpointIp;
	}

	public short getLocalEndpointPort() {
		return localEndpointPort;
	}

	public void setLocalEndpointPort(short localEndpointPort) {
		this.localEndpointPort = localEndpointPort;
	}

	public int getRemoteEndpointServiceName() {
		return remoteEndpointServiceName;
	}

	public void setRemoteEndpointServiceName(int remoteEndpointServiceName) {
		this.remoteEndpointServiceName = remoteEndpointServiceName;
	}

	public byte[] getRemoteEndpointIp() {
		return remoteEndpointIp;
	}

	public void setRemoteEndpointIp(byte[] remoteEndpointIp) {
		this.remoteEndpointIp = remoteEndpointIp;
	}

	public short getRemoteEndpointPort() {
		return remoteEndpointPort;
	}

	public void setRemoteEndpointPort(short remoteEndpointPort) {
		this.remoteEndpointPort = remoteEndpointPort;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public ListValue getAnnotationTimestamps() {
		return annotationTimestamps;
	}

	public void setAnnotationTimestamps(ListValue annotationTimestamps) {
		this.annotationTimestamps = annotationTimestamps;
	}

	public ListValue getAnnotationValues() {
		return annotationValues;
	}

	public void setAnnotationValues(ListValue annotationValues) {
		this.annotationValues = annotationValues;
	}

	public MapValue getTags() {
		return tags;
	}

	public void setTags(MapValue tags) {
		this.tags = tags;
	}

	@Override
	public String toString() {
		return "CommonSpanStep{" +
				"nameDebug='" + nameDebug + '\'' +
				", hash=" + hash +
				", parent=" + parent +
				", index=" + index +
				", start_time=" + start_time +
				", elapsed=" + elapsed +
				", error=" + error +
				", timestamp=" + timestamp +
				", timestampDt=" + DateUtil.timestamp(timestamp / 1000) +
				", spanType=" + spanType +
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
				"}";
	}
}