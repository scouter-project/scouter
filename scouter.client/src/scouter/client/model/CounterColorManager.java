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
package scouter.client.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class CounterColorManager {
	
	private static CounterColorManager instance;
	private Map<String, Color> assignedColor = new HashMap<String, Color>();
	
	static RGB[] default_rgb_map = { 
			new RGB(55, 78, 179),
			new RGB(5, 128, 100),
			new RGB(55, 178, 180),
			new RGB(105, 128, 181),
			new RGB(156, 128, 163),
			new RGB(157, 178, 182),
			new RGB(105, 128, 203),
			new RGB(158, 128, 161),
			new RGB(1, 2, 222), 
			new RGB(0, 128, 10), 
			new RGB(101, 9, 251), 
			new RGB(41, 121, 138), 
			new RGB(11, 50, 249)
	};
	

//	// Green Pattern
//	static RGB[] default_rgb_map = { 
//		new RGB(88, 130, 0),
//		new RGB(172, 217, 43),
//		new RGB(75, 153, 38),
//		new RGB(108, 217, 0),
//		new RGB(0, 140, 98),
//		new RGB(118, 174, 0),
//		new RGB(26, 130, 0),
//		new RGB(128, 217, 87),
//		new RGB(128, 130, 43), 
//		new RGB(189, 217, 130) 
//	};
	
	// Brown Pattern
//	static RGB[] default_rgb_map = { 
//		new RGB(204, 158, 40),
//		new RGB(126, 128, 51),
//		new RGB(245, 164, 41),
//		new RGB(179, 107, 0),
//		new RGB(204, 163, 81),
//		new RGB(204, 106, 51),
//		new RGB(245, 209, 81),
//		new RGB(163, 122, 40),
//		new RGB(245, 203, 122), 
//		new RGB(217, 141, 71) 
//	};
	
	public synchronized static CounterColorManager getInstance() {
		if (instance == null) {
			instance = new CounterColorManager();
		}
		return instance;
	}
	
	private CounterColorManager() { }
	
	public Color getColor(String name) {
		return  assignedColor.get(name);
	}
	
	public Color assignColor(String name) {
		Color color = assignedColor.get(name);
		if (color != null) {
			return color;
		}
		color = searchAvaliableColor(default_rgb_map[assignedColor.size() % default_rgb_map.length]);
		assignedColor.put(name, color);
		return color;
	}
	
	private Color searchAvaliableColor(RGB rgb) {
		Iterator<Color> itr = assignedColor.values().iterator();
		while (itr.hasNext()) {
			Color existColor = itr.next();
			if (existColor.getRGB().equals(rgb)) {
				int[] rgbs = new int[3];
				rgbs[0] = rgb.red;
				rgbs[1] = rgb.green;
				rgbs[2] = rgb.blue;
				int rand = (int)(Math.random() *3);
				rgbs[rand] = (rgbs[rand] + 1) % 256;
				return searchAvaliableColor(new RGB(rgbs[0], rgbs[1], rgbs[2]));
			}
		}
		return new Color(null, rgb);
	}
	
	public String getName(RGB rgb) {
		Iterator<String> itr = assignedColor.keySet().iterator();
		while (itr.hasNext()) {
			String name = itr.next();
			Color color = assignedColor.get(name);
			if (color.getRGB().equals(rgb)) {
				return name;
			}
		}
		return null;
	}
}
