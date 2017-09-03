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

public class StepControl extends StepSummary {

	public String message;
	public int code;

	public StepControl() {
	}

	public StepControl(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public int getCode() {
		return code;
	}

	public byte getStepType() {
		return StepEnum.CONTROL;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeText(message);
		out.writeDecimal(code);
	}

	public Step read(DataInputX in) throws IOException {
		this.message = in.readText();
		this.code = (int) in.readDecimal();
		return this;
	}
}