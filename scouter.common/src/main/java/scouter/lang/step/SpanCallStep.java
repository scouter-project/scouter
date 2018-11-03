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

public class SpanCallStep extends ApiCallStep2 {
	public String nameDebug;

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
		return StepEnum.SPANCALL;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeValue(tags);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.tags = (MapValue) in.readValue();
		return this;
	}
	public static SpanCallStep fromPack(SpanPack pack, int index) {
		SpanCallStep step = new SpanCallStep();
		step.spanPack = pack;

		step.index = index;
		step.tags = pack.tags;
		step.elapsed = pack.elapsed;
		step.error = pack.error;
		step.hash = pack.name;
		step.timestamp = pack.timestamp;
		step.spanType = pack.spanType;
		step.localEndpointServiceName = pack.localEndpointServiceName;
		step.localEndpointIp = pack.localEndpointIp;
		step.localEndpointPort = pack.localEndpointPort;
		step.remoteEndpointServiceName = pack.remoteEndpointServiceName;
		step.remoteEndpointIp = pack.remoteEndpointIp;
		step.remoteEndpointPort = pack.remoteEndpointPort;
		step.debug = pack.debug;
		step.shared = pack.shared;

		step.txid = pack.txid;

		if (pack.tags != null) {
			if(pack.tags.containsKey("http.url")) {
				step.opt = 1;
				step.address = pack.tags.getText("http.url");
			}
		}

		return step;
	}

}
