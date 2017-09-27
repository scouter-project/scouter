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

package scouter.lang.pack;

public enum AlertLevelEnum {
	INFO(0),
	WARN(1),
	ERROR(2),
	FATAL(3),
	;

	private int level;

	AlertLevelEnum(int level) {
		this.level = level;
	}

	public int getLevel() {
		return this.level;
	}

	public static AlertLevelEnum of(int b) {
		for (AlertLevelEnum level : AlertLevelEnum.values()) {
			if (level.level == b) {
				return level;
			}
		}
		return INFO;
	}
}
