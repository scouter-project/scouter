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

import java.io.IOException;

public class SpanCallStep extends CommonSpanStep {

	public long txid;
	transient public byte opt;
	public String address;
	public byte async;

	public byte getStepType() {
		return StepEnum.SPANCALL;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeDecimal(txid);
		out.writeByte(opt);
		switch(opt){
			case 1:
				out.writeText(address);
		}
		out.writeByte(async);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.txid = in.readDecimal();
		this.opt= in.readByte();
		switch (opt) {
			case 1:
				this.address = in.readText();
				break;
			default:
		}
		this.async= in.readByte();
		return this;
	}

	public static SpanCallStep fromPack(SpanPack pack, int index, long initialTime) {
		SpanCallStep step = new SpanCallStep();
		step.spanPack = pack;

		step.index = index;
		step.start_time = (int) (pack.timestamp - initialTime);
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
		step.annotationTimestamps = pack.annotationTimestamps;
		step.annotationValues = pack.annotationValues;

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
