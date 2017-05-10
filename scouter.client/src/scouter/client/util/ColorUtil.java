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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import java.util.HashMap;

public class ColorUtil {
	
	private static volatile ColorUtil instance;
	
	public static RGB[] default_rgb_map = { 
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
	
	private HashMap<String, Color> rgb = new HashMap<String, Color>();
	
	public static ColorUtil getInstance() {
		if (instance == null) {
			synchronized (ColorUtil.class) {
				if (instance == null) {
					instance = new ColorUtil();
				}
			}
		}
		return instance;
	}

	private ColorUtil() {
		rgb.put("aliceblue", new Color(null, 240, 248, 255));
		rgb.put("azure",  new Color(null, 240, 255, 255));
		rgb.put("pink",  new Color(null, 255, 191, 203));
		rgb.put("yellow",  new Color(null, 255, 255, 0 ));
		rgb.put("cornsilk",  new Color(null, 255, 248, 220));
		rgb.put("ivory",  new Color(null, 255, 255, 240));

		rgb.put("white", new Color(null, 255, 255, 255));
		rgb.put("blue",  new Color(null, 0, 0, 255));
		rgb.put("red",  new Color(null, 255, 0, 0));
		rgb.put("light red",  new Color(null, 255, 135, 135));
		rgb.put("light2 red",  new Color(null, 255, 180, 180));
		rgb.put("light red2",  new Color(null, 255, 100, 100));
		rgb.put("green",  new Color(null, 0, 255, 0));
		rgb.put("gray", new Color(null, 100, 100, 100));
		rgb.put("light gray", new Color(null, 160, 160, 160));
		rgb.put("light2 gray", new Color(null, 190, 190, 190));
		rgb.put("blue gray",  new Color(null, 102, 153, 204));

		rgb.put("dark green", new Color(null, 00, 0x64, 00));
		rgb.put("dark magenta", new Color(null, 0x8B, 00, 0x8B));
		rgb.put("dark blue", new Color(null, 0, 0, 0x8B));
		rgb.put("dark red",  new Color(null, 139, 0, 0));
		rgb.put("dark gray", new Color(null, 70, 70, 70));

		rgb.put("gray2", new Color(null, 150, 150, 180));
		rgb.put("gray3",  new Color(null, 120, 120, 180));
		rgb.put("dark orange",  new Color(null, 238, 140, 20));
	}

	public Color getColor(String name) {
		Color color = rgb.get(name);
		if (color == null) {
			color = new Color(null, 255, 255, 255);
		}
		return color;
	}

	public Color ac1 = new Color(null, 108, 192, 255);
	//public Color ac2 = new Color(null, 255, 167, 167);
	public Color ac2 = new Color(null, 242, 203, 97);
	public Color ac3 = new Color(null, 255, 130, 193);
	public Color acm = new Color(null, 150,150, 255);
	
	public Color act1_light = new Color(null, 220, 228, 255);
	public Color act2_light = new Color(null, 255, 255, 169);
	public Color act3_light = new Color(null, 255, 214, 255);
	
	public Color TOTAL_CHART_COLOR = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE);

	public Color getColor(int id) {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}		
		return display.getSystemColor(id);
	}
}
