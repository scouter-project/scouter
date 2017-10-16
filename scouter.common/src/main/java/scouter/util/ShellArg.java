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

package scouter.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShellArg {
	private Map<String, String> parameter = new HashMap<String, String>();
	private Map<String, String> parameter2 = new HashMap<String, String>();

	public ShellArg(String[] args) {

		int i = 0;
		while (i < args.length) {
			if (args[i].startsWith("-")) {
				if (hasNextValue(i, args)) {
					String key = args[i];
					parameter.put(key, args[i + 1]);
					i++;
					if (hasNextValue(i, args)) {
						parameter2.put(key, args[i + 1]);
						i++;
					}
				} else {
					parameter.put(args[i], "");
				}
			}
			i++;
		}
	}

	private boolean hasNextValue(int i, String[] args) {
		return i + 1 < args.length && args[i + 1].startsWith("-") == false;
	}

	public boolean hasKey(String key) {
		return parameter.containsKey(key);
	}

	public Set<String> keys() {
		return parameter.keySet();
	}

	public String get(String key) {
		return parameter.get(key);
	}

	public String get(String key, String defaultValue) {
		String s = parameter.get(key);
		return s == null ? defaultValue : s;
	}

	public int getInt(String key, int defaultValue) {
		String s = parameter.get(key);
		return s == null ? defaultValue : CastUtil.cint(s);
	}

	public String get2(String key) {
		return parameter2.get(key);
	}

	public void put(String key, String value) {
		this.parameter.put(key, value);
	}

	public String[] toStringArray() {
		List<String> arr = new ArrayList<String>();
		Iterator<String> itr = parameter.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			String value1 = parameter.get(key);
			String value2 = parameter2.get(key);
			arr.add(key);
			add(arr, value1);
			add(arr, value2);
		}
		return (String[]) arr.toArray(new String[arr.size()]);
	}

	private void add(List<String> arr, String value) {
		if (value != null) {
			if (value.length() == 0 || value.indexOf(' ') >= 0) {
				arr.add("\"" + value + "\"");
			} else {
				arr.add(value);
			}
		}
	}

}