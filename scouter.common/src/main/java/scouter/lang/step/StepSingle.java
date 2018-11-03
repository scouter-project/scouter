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

package scouter.lang.step;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.SpanPack;

import java.io.IOException;

abstract public class StepSingle extends Step {

	public int parent;
	public int index;

	public int start_time;
	public int start_cpu;

	//for span
	public SpanPack spanPack;

	@Override
	public int getOrder() {
		return index;
	}

	public int getParent() {
		return parent;
	}

	public int getIndex() {
		return index;
	}

	public int getStart_time() {
		return start_time;
	}

	public int getStart_cpu() {
		return start_cpu;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeDecimal(parent);
		out.writeDecimal(index);
		out.writeDecimal(start_time);
		out.writeDecimal(start_cpu);
	}
	public Step read(DataInputX in) throws IOException {
		this.parent = (int) in.readDecimal();
		this.index = (int) in.readDecimal();
		this.start_time = (int) in.readDecimal();
		this.start_cpu = (int) in.readDecimal();
		return this;
	}
}