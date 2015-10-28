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

public class ServiceGroupColorManager {
	
	private static ServiceGroupColorManager instance;
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
	
/*	static RGB[] default_rgb_map = { 
		new RGB(54, 138, 255),
		new RGB(83, 193, 75),
		new RGB(232, 219, 95),
		new RGB(92, 209, 229),
		new RGB(184, 184, 184),
		new RGB(225, 79, 202),
		new RGB(0, 51, 153),
		new RGB(153, 56, 0),
		new RGB(89, 135, 0), 
		new RGB(0, 180, 219), 
		new RGB(102, 0, 51), 
		new RGB(237, 206, 122), 
		new RGB(0, 87, 102)
};*/
	
/*	static RGB[] default_rgb_map = { 
		new RGB(71, 200, 62),
		new RGB(70, 65, 217),
		new RGB(217, 65, 140),
		new RGB(204, 166, 61),
		new RGB(159, 201, 60),
		new RGB(243, 97, 166),
		new RGB(67, 116, 217),
		new RGB(217, 65, 197),
		new RGB(204, 114, 61), 
		new RGB(196, 183, 59), 
		new RGB(61, 183, 204), 
		new RGB(128, 65, 217), 
	};*/
	
	public synchronized static ServiceGroupColorManager getInstance() {
		if (instance == null) {
			instance = new ServiceGroupColorManager();
		}
		return instance;
	}
	
	private ServiceGroupColorManager() { }
	
	public Color getColor(String grpName) {
		return  assignedColor.get(grpName);
	}
	
	public Color assignColor(String grpName) {
		Color color = assignedColor.get(grpName);
		if (color != null) {
			return color;
		}
		color = searchAvaliableColor(default_rgb_map[assignedColor.size() % default_rgb_map.length]);
		assignedColor.put(grpName, color);
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
	
	public String getServiceGroup(RGB rgb) {
		Iterator<String> itr = assignedColor.keySet().iterator();
		while (itr.hasNext()) {
			String grpName = itr.next();
			Color objColor = assignedColor.get(grpName);
			if (objColor.getRGB().equals(rgb)) {
				return grpName;
			}
		}
		return null;
	}
}
