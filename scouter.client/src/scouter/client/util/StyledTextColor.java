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
 *
 */
package scouter.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

public class StyledTextColor {
	private Map<String, Color> map = new TreeMap<String, Color>();

	public void add(String text, Color color) {
		map.put(text, color);
	}

	public List<StyleRange> calc(String text) {
		List<String> keys = new ArrayList<String>(map.keySet());

		Map<Integer, StyleRange> srlist = new TreeMap<Integer, StyleRange>();
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			Color c = map.get(key);
			int x = text.indexOf(key);
			while (x >= 0) {
				srlist.put(x,style(x, key.length(), c));
				x = text.indexOf(key, x+key.length());
			}
		}
		return new ArrayList<StyleRange>(srlist.values());

	}

	private static StyleRange style(int start, int length, Color c) {
		StyleRange t = new StyleRange();
		t.start = start;
		t.length = length;
		t.foreground = c;
		t.fontStyle = SWT.NORMAL;
		return t;
	}
}
