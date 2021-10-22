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

import java.io.IOException;


public class MessageStep extends StepSingle {

	public String message;

	public MessageStep() {
	}
	public MessageStep(String message) {
		this.message = message;
	}
	public MessageStep(int start_time , String message) {
		this.start_time = start_time;
		this.message = message;
	}
	public byte getStepType() {
		return StepEnum.MESSAGE;
	}

	public String getMessage() {
		return message;
	}

	public String toString() {
		return "MessageStep " +message;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeText(message);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.message = in.readText();
		return this;
	}
}
