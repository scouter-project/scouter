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

package scouter.server.db;

import scouter.util.LinkedSet;
import scouter.util.StringKeyLinkedMap;

public class TextDupCheck {

	static StringKeyLinkedMap<LinkedSet<TextUnit>> dupcheck = new StringKeyLinkedMap<LinkedSet<TextUnit>>();

	static class TextUnit {
		int date;

		int text;

		public TextUnit(String datestr, int text) {
			this.date = Integer.parseInt(datestr);
			this.text = text;
		}

		public int hashCode() {
			return date ^ text;
		}
		public boolean equals(Object obj) {
			if (obj instanceof TextUnit) {
				TextUnit other = (TextUnit) obj;
				return (date == other.date && text == other.text);
			}
			return false;
		}

	}

	static boolean isDuplicated(String div, TextUnit tu) {
		LinkedSet<TextUnit> set = dupcheck.get(div);
		if (set == null)
			return false;
		return set.contains(tu);
	}

	static void addDuplicated(String div, TextUnit tu) {
		LinkedSet<TextUnit> set = dupcheck.get(div);
		if (set == null) {
			set = new LinkedSet<TextUnit>().setMax(10000);
			dupcheck.put(div, set);
		}
		set.put(tu);
	}

}