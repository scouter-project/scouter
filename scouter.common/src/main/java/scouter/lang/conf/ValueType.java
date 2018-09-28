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
package scouter.lang.conf;

/**
 * Created by gunlee on 2017. 8. 17.
 */
public enum ValueType {
	VALUE(1),
	NUM(2),
	BOOL(3),
	COMMA_SEPARATED_VALUE(4),
	COMMA_COLON_SEPARATED_VALUE(5),
	;

	int type;
	ValueType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public static ValueType of(int type) {
		ValueType[] values = values();
		for (ValueType value : values) {
			if (value.type == type) {
				return value;
			}
		}
		return VALUE;
	}
}
