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
import scouter.lang.enumeration.ParameterizedMessageLevel;
import scouter.util.StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ParamaterizedMessageStep is a class for message step of formatted text.
 * It use String.format() to format the message owned.
 *
 */
public class ParameterizedMessageStep extends StepSingle {
	private static char delimETX = 3;

	private int hash;
	private int elapsed = -1;
	private byte level = 0; //0:debug, 1:info, 2:warn, 3:error
	private String paramString;
	private transient Map<String, String> tempMap = new HashMap<String, String>();

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
		out.writeDecimal(level);
		out.writeText(paramString);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.hash = (int) in.readDecimal();
		this.elapsed = (int) in.readDecimal();
		this.level = (byte) in.readDecimal();
		this.paramString = in.readText();

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

	public void setLevel(ParameterizedMessageLevel level) {
		this.level = level.getLevel();
	}

	public void setLevelOfByte(byte level) {
		this.level = level;
	}

	public ParameterizedMessageLevel getLevel() {
		return ParameterizedMessageLevel.of(this.level);
	}

	public void putTempMessage(String key, String message) {
	    this.tempMap.put(key, message);
    }

    public String getTempMessage(String key) {
	    return this.tempMap.get(key);
    }

	public void setMessage(int hash, String... params) {
		this.hash = hash;
		StringBuilder sb = new StringBuilder();

		for(String param : params) {
			sb.append(param).append(delimETX); //consider etx when it generates full message.
		}
		this.paramString = sb.toString();
	}

	public String buildMessasge(String messageFormat) {
		String[] params = null;

		try {
			if (this.paramString != null) {
				params = StringUtil.split(this.paramString, delimETX);
			} else {
				return messageFormat;
			}
			if(params.length == 0) {
				return messageFormat;
			}

			return String.format(messageFormat, (Object[])params);
		} catch (Exception e) {
			return messageFormat;
		}
	}
}
