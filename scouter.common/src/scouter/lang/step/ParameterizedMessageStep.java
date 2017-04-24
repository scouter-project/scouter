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
import scouter.util.StringUtil;

import java.io.IOException;

/**
 * ParamaterizedMessageStep is a class for message step of formatted text.
 * It use String.format() to format the message owned.
 *
 */
public class ParameterizedMessageStep extends StepSingle {
	private static char delimETX = 3;

	public int hash;
	public int elapsed = -1;
	private String paramString;

	public byte getStepType() {
		return StepEnum.PARAMETERIZED_MESSAGE;
	}

	public String toString() {
		return "ParameterizedMessageStep " + hash;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeDecimal(hash);
		out.writeDecimal(elapsed);
		out.writeText(paramString);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.hash = (int) in.readDecimal();
		this.elapsed = (int) in.readDecimal();
		this.paramString = in.readText();

		return this;
	}

	public void setMessage(int hash, String... params) {
		this.hash = hash;
		StringBuilder sb = new StringBuilder();

		for(String param : params) {
			sb.append(param).append(delimETX); //consider etx when it generates full message.
		}
		this.paramString = sb.toString();
	}

	public String buildMessasge(String message, String paramString) {
		String[] params = null;

		try {
			if (paramString != null) {
				params = StringUtil.split(paramString, delimETX);
			} else {
				return message;
			}
			if(params.length == 0) {
				return message;
			}

			return String.format(message, paramString);
		} catch (Exception e) {
			return message;
		}
	}
}
