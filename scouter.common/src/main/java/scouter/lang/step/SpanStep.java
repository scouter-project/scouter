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

public class SpanStep extends CommonSpanStep {

	public byte getStepType() {
		return StepEnum.SPAN;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		return this;
	}

	public static SpanStep fromPack(SpanPack pack, int index, long initialTime) {
		SpanStep step = new SpanStep();
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
		return step;
	}

	@Override
	public String toString() {
		return "SpanStep{}=" + super.toString();
	}
}