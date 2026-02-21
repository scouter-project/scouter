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
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;

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

	public static RGB[] default_rgb_map_dark = {
		new RGB(100, 160, 255),
		new RGB(50, 210, 170),
		new RGB(100, 230, 230),
		new RGB(150, 180, 240),
		new RGB(200, 170, 220),
		new RGB(200, 220, 230),
		new RGB(150, 180, 255),
		new RGB(210, 170, 210),
		new RGB(80, 120, 255),
		new RGB(60, 220, 80),
		new RGB(170, 100, 255),
		new RGB(80, 200, 210),
		new RGB(90, 130, 255)
	};

	public static RGB[] getDefaultRgbMap() {
		return isDarkMode() ? default_rgb_map_dark : default_rgb_map;
	}
	
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
		rgb.put("brown",  new Color(null, 165, 42, 42));
		rgb.put("gunlee",  new Color(null, 0, 128, 128));
		rgb.put("gunlee2",  new Color(null, 0, 102, 204));
		rgb.put("gunlee3",  new Color(null, 51, 204, 204));
		rgb.put("gunlee4",  new Color(null, 0, 204, 255));
		rgb.put("maroon",  new Color(null, 128, 0, 0));
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
	
	public Color TOTAL_CHART_COLOR = new Color(null, 0, 0, 139);

	public Color getColor(int id) {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display.getSystemColor(id);
	}

	// Dark mode support methods
	public static boolean isDarkMode() {
		boolean prefDarkMode = PManager.getInstance().getBoolean(PreferenceConstants.P_DARK_MODE);
		if (prefDarkMode) {
			return true;
		}
		return detectSystemDarkMode();
	}

	private static volatile Boolean darkModeCache = null;

	private static boolean detectSystemDarkMode() {
		if (darkModeCache != null) {
			return darkModeCache;
		}
		try {
			Display display = Display.getCurrent();
			if (display == null) {
				// Non-UI thread: return cached value or false
				return darkModeCache != null ? darkModeCache : false;
			}
			// UI thread: detect and cache
			org.eclipse.swt.widgets.Shell[] shells = display.getShells();
			if (shells != null && shells.length > 0) {
				Color bg = shells[0].getBackground();
				if (bg != null) {
					double luminance = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
					darkModeCache = luminance < 0.5;
					return darkModeCache;
				}
			}
			Color sysBg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
			if (sysBg != null) {
				double luminance = (0.299 * sysBg.getRed() + 0.587 * sysBg.getGreen() + 0.114 * sysBg.getBlue()) / 255.0;
				darkModeCache = luminance < 0.5;
				return darkModeCache;
			}
		} catch (Exception e) {
		}
		return false;
	}

	// Theme color cache
	private static Color chartBackground;
	private static Color chartBackgroundDark;
	private static Color chartForeground;
	private static Color chartForegroundDark;
	private static Color chartGridNarrow;
	private static Color chartGridNarrowDark;
	private static Color chartGridWide;
	private static Color chartGridWideDark;
	private static Color xlogIgnoreArea;
	private static Color xlogIgnoreAreaDark;
	private static Color chartBorder;
	private static Color chartBorderDark;
	private static Color filteredBackground;
	private static Color filteredBackgroundDark;
	private static Color axisGrid;
	private static Color axisGridDark;

	public static Color getChartBackground() {
		if (isDarkMode()) {
			if (chartBackgroundDark == null) {
				chartBackgroundDark = new Color(null, 30, 30, 35);
			}
			return chartBackgroundDark;
		} else {
			if (chartBackground == null) {
				chartBackground = new Color(null, 255, 255, 255);
			}
			return chartBackground;
		}
	}

	public static Color getChartForeground() {
		if (isDarkMode()) {
			if (chartForegroundDark == null) {
				chartForegroundDark = new Color(null, 200, 200, 210);
			}
			return chartForegroundDark;
		} else {
			if (chartForeground == null) {
				chartForeground = new Color(null, 0, 0, 0);
			}
			return chartForeground;
		}
	}

	public static Color getChartGridNarrow() {
		if (isDarkMode()) {
			if (chartGridNarrowDark == null) {
				chartGridNarrowDark = new Color(null, 55, 55, 70);
			}
			return chartGridNarrowDark;
		} else {
			if (chartGridNarrow == null) {
				chartGridNarrow = new Color(null, 220, 228, 255);
			}
			return chartGridNarrow;
		}
	}

	public static Color getChartGridWide() {
		if (isDarkMode()) {
			if (chartGridWideDark == null) {
				chartGridWideDark = new Color(null, 70, 70, 90);
			}
			return chartGridWideDark;
		} else {
			if (chartGridWide == null) {
				chartGridWide = new Color(null, 200, 208, 255);
			}
			return chartGridWide;
		}
	}

	public static Color getXLogIgnoreArea() {
		if (isDarkMode()) {
			if (xlogIgnoreAreaDark == null) {
				xlogIgnoreAreaDark = new Color(null, 45, 45, 50);
			}
			return xlogIgnoreAreaDark;
		} else {
			if (xlogIgnoreArea == null) {
				xlogIgnoreArea = new Color(null, 234, 234, 234);
			}
			return xlogIgnoreArea;
		}
	}

	public static Color getChartBorderColor() {
		if (isDarkMode()) {
			if (chartBorderDark == null) {
				chartBorderDark = new Color(null, 100, 100, 120);
			}
			return chartBorderDark;
		} else {
			if (chartBorder == null) {
				chartBorder = new Color(null, 0, 0, 0);
			}
			return chartBorder;
		}
	}

	public static Color getFilteredBackground() {
		if (isDarkMode()) {
			if (filteredBackgroundDark == null) {
				filteredBackgroundDark = new Color(null, 30, 40, 50);
			}
			return filteredBackgroundDark;
		} else {
			if (filteredBackground == null) {
				filteredBackground = new Color(null, 240, 255, 255);
			}
			return filteredBackground;
		}
	}

	public static Color getAxisGridColor() {
		if (isDarkMode()) {
			if (axisGridDark == null) {
				axisGridDark = new Color(null, 60, 60, 75);
			}
			return axisGridDark;
		} else {
			if (axisGrid == null) {
				axisGrid = new Color(null, 200, 200, 200);
			}
			return axisGrid;
		}
	}
}
