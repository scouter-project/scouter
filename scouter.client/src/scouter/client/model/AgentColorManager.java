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

import scouter.client.util.ColorUtil;

public class AgentColorManager {
	
	private static AgentColorManager instance;
	private Map<Integer, Color> assignedColor = new HashMap<Integer, Color>();
	private Map<String, Integer> assignedIndex = new HashMap<String, Integer>();
	
	public synchronized static AgentColorManager getInstance() {
		if (instance == null) {
			instance = new AgentColorManager();
		}
		return instance;
	}
	
	private AgentColorManager() { }
	
	public Color getColor(int objHash) {
		return  assignedColor.get(objHash);
	}
	
	public Color assignColor(String objType, int objHash) {
		Color color = assignedColor.get(objHash);
		if (color != null) {
			return color;
		}
		if (assignedIndex.containsKey(objType) == false) {
			assignedIndex.put(objType, 0);
		}
		int index = assignedIndex.get(objType);
		RGB[] rgbMap = ColorUtil.getDefaultRgbMap();
		color = searchAvaliableColor(rgbMap[index % rgbMap.length]);
		assignedColor.put(objHash, color);
		if (index >= ColorUtil.default_rgb_map.length - 1) {
			assignedIndex.put(objType, 0);
		} else {
			assignedIndex.put(objType, index + 1);
		}
		return color;
	}
	
	public Color changeColor(int objHash, RGB rgb) {
		Color color = searchAvaliableColor(rgb);
		assignedColor.put(objHash, color);
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
	
	public int getObjectHash(RGB rgb) {
		Iterator<Integer> itr = assignedColor.keySet().iterator();
		while (itr.hasNext()) {
			int objHash = itr.next();
			Color objColor = assignedColor.get(objHash);
			if (objColor.getRGB().equals(rgb)) {
				return objHash;
			}
		}
		return 0;
	}

	public static void main(String[] args) {
		System.out.println((int)(Math.random() *3));
	}
	
}
