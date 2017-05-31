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

package scouter.lang;

import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

public class AlertRuleData {
	public String objType;
	public long checkInterval = 60000L;
	public String title;
	public byte level;
	public String condition;
	public String message;
	public long lastCheckTime;
	public boolean enabled = true;

	public AlertRuleData() {
	}

	public AlertRuleData(String objType, String title, byte level, String condition, String message) {
		this.objType = objType;
		this.title = title;
		this.level = level;
		this.condition = condition;
		this.message = message;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeText(objType);
		out.writeLong(checkInterval);
		out.writeText(title);
		out.writeByte(level);
		out.writeText(condition);
		out.writeText(message);
		out.writeLong(lastCheckTime);
		out.writeBoolean(enabled);
	}

	public void read(DataInputX input) throws IOException {
		this.objType = input.readText();
		this.checkInterval = input.readLong();
		this.title = input.readText();
		this.level = input.readByte();
		this.condition = input.readText();
		this.message = input.readText();
		this.lastCheckTime = input.readLong();
		this.enabled = input.readBoolean();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlertRuleData other = (AlertRuleData) obj;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	public String toString() {
		return "AlertData [objType=" + objType + ", checkInterval=" + checkInterval + ", title=" + title + ", level="
				+ level + ", condition=" + condition + ", message=" + message + "]";
	}
}