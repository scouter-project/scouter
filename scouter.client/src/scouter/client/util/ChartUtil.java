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

import java.util.Iterator;
import java.util.List;

import org.csstudio.swt.xygraph.dataprovider.IDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;

import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.util.DateUtil;


public class ChartUtil {
	public static GridLayout gridlayout(int n) {
		GridLayout layout = new GridLayout();
		layout.numColumns = n;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	public static String getTimeFormat(long unit) {
		if (unit <= DateUtil.MILLIS_PER_HOUR)
			return "HH:mm:ss";
		if (unit <= DateUtil.MILLIS_PER_DAY)
			return "HH:mm";
		return "yyyy-MM-dd";
	}

	public static double getSplitTimeUnit(double val, int width) {
		int units = width / 15;
		if (units < 1)
			units = 1;

		val = val / (double) units;
		if (val <= 0.005)
			return 0.005;
		else {
			return rounding(val * 1000) / 1000;
		}
	}

	public static double getSplitUnit(double val, int height) {
		int units = height / 15;
		if (units < 1)
			units = 1;

		val = val / (double) units;
		if (val <= 0.005)
			return 0.005;
		else {
			return rounding(val * 1000) / 1000;
		}
	}

	public static double getMaxValue(double val) {
//		if (val <= 0.005)
//			return 0.005;
		if (val < 3)
			return 5;
		if (val < 8)
			return 10;
		if (val < 15)
			return 20;
		return rounding((val+(val/100)) * 1000) / 1000;
	}
	
	public static double getGroupMaxValue(double val) {
		if (val < 2)
			return 3;
		if (val < 3)
			return 5;
		if (val < 8)
			return 10;
		if (val < 15)
			return 20;
		return rounding(val * 1000) / 1000;
	}
	
	public static double getEqMaxValue(double val) {
		/*
		if (val < 10)
			return 10;
		if (val < 30)
			return 30;
		if (val < 50)
			return 50;
		return rounding(val * 100) / 100;
		*/

		// Add margin(width or height) for detailed active service counts.
		if (val < 7)
			return 10;
		if (val < 20)
			return 30;
		if (val < 40)
			return 60;
		if (val < 70)
			return 100;
		if (val < 300)
			return 500;
		return rounding(val * 100) / 100;
	}

	public static double getYaxisUnit(double val, int height) {
		int units = height / 15;
		if (units < 1)
			units = 1;

		val = val / (double) units;
		return getElapsedMaxValue(val * 1000) / 1000;
	}

	public static double getElapsedMaxValue(double val) {
		if (val <= 0.005)
			return 0.005;
		else {
			long value = (long) (val * 1000);
			long decVal = 1;
			for (long x = value; x > 10; x /= 10) {
				decVal *= 10;
			}
			if (value == decVal)
				return value / 1000;
			return (value / decVal + (value - value / decVal * decVal == 0 ? 0 : 1)) * decVal / 1000;
		}
	}

	public static double addElapsedUnit(double val) {
		if (val <= 0.005)
			return 0.005;
		else {
			long value = (long) val;
			if (value == 10)
				return 10;
			long decVal = 1;
			for (long x = value; x >= 10; x /= 10) {
				decVal *= 10;
			}
			return decVal;
		}
	}

	private static double rounding(double val) {
		long value = (long) val;
		long decVal = 1;
		for (long x = value; x >= 10; x /= 10) {
			decVal *= 10;
		}
		if (value > decVal * 5) {
			return decVal * 10;
		} else if (value > decVal * 2) {
			return decVal * 5;
		} else {
			return decVal * 2;
		}
	}

	public static long getTimeUnit(long interval) {
		if (interval <= 1000) {
			return 1000;
		} else if (interval <= 2 * 1000) {
			return 2000;
		} else if (interval <= 5 * 1000) {
			return 5 * 1000;
		} else if (interval <= 10 * 1000) {
			return 10 * 1000;
		} else if (interval <= 15 * 1000) {
			return 15 * 1000;
		} else if (interval <= 30 * 1000) {
			return 30 * 1000;
		} else if (interval <= 60 * 1000) {
			return 60 * 1000;
		} else if (interval <= 120 * 1000) {
			return 120 * 1000;
		} else if (interval <= 300 * 1000) {
			return 300 * 1000;
		} else if (interval <= 600 * 1000) {
			return 600 * 1000;
		} else if (interval <= 15 * 60 * 1000) {
			return 15 * 60 * 1000;
		} else if (interval <= 30 * 60 * 1000) {
			return 15 * 60 * 1000;
		} else if (interval <= 3600 * 1000) {
			return 3600 * 1000;
		} else if (interval <= 2 * 3600 * 1000) {
			return 2 * 3600 * 1000;
		} else if (interval <= 3 * 3600 * 1000) {
			return 3 * 3600 * 1000;
		} else if (interval <= 4 * 3600 * 1000) {
			return 4 * 3600 * 1000;
		} else if (interval <= 6 * 3600 * 1000) {
			return 6 * 3600 * 1000;
		} else if (interval <= 12 * 3600 * 1000) {
			return 12 * 3600 * 1000;
		}
		return interval;
	}

	public static double getMax(Iterator<ISample> iterator) {
		double max = 0;
		while (iterator.hasNext()) {
			ISample s = iterator.next();
			if (s.getYValue() > max)
				max = s.getYValue();
		}
//		if (max < 3)
//			return 5;
//		if (max < 8)
//			return 10;
//		if (max < 15)
//			return 20;
		return ChartUtil.getMaxValue(max);
	}
	
	public static double getMax(List<Double> values) {
		double max = 0;
		for (Double value : values) {
			double v = value.doubleValue();
			if (v > max)
				max = v;
		}
//		if (max < 3)
//			return 5;
//		if (max < 8)
//			return 10;
//		if (max < 15)
//			return 20;
		return ChartUtil.getMaxValue(max);
	}

	public static Trace addSolidLine(XYGraph xyGraph, IDataProvider dataProvider, Color color) {
		Trace lineTrace = new Trace("SOLID_LINE", xyGraph.primaryXAxis, xyGraph.primaryYAxis, dataProvider);
		lineTrace.setPointStyle(PointStyle.NONE);
		lineTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		lineTrace.setTraceType(TraceType.SOLID_LINE);
		lineTrace.setTraceColor(color);
		xyGraph.addTrace(lineTrace);
		return lineTrace;
	}
	
	public static boolean isShowLegendAllowSize(int width, int height) {
		boolean result = false;
		if (width >= 400 && height >= 400) {
			result = true;
		}
		return result;
	}
	
	public static boolean isShowDescriptionAllowSize(int height) {
		boolean result = false;
		if (height >= 200) {
			result = true;
		}
		return result;
	}
	
	public static void main(String[] args) {
		System.out.println(getMaxValue(20));

	}
}
