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

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import org.csstudio.swt.xygraph.dataprovider.IDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.figures.PlotArea;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.undo.ZoomType;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import scouter.client.Images;
import scouter.client.constants.MenuStr;
import scouter.client.group.GroupManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.io.DataInputX;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.LinkedMap;

public class ScouterUtil {

	public static String listToComma(List list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append(String.valueOf(list.get(i)));
			if (i < list.size() - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public static double getRealTotalValue(String counter, List<Pack> result, String mode) {
		// if (CounterConstants.WAS_ELAPSED_TIME.equalsIgnoreCase(counter)) {
		// return getRealElapsedAvgValue(result);
		// }
		int count = 0;
		double value = 0.0;
		for (Pack p : result) {
			MapPack m = (MapPack) p;
			ListValue valueLv = m.getList("value");
			for (int i = 0; i < valueLv.size(); i++) {
				value += CastUtil.cdouble(valueLv.get(i));
				count++;
			}
		}
		if (count > 0 && "avg".equals(mode)) {
			value = value / count;
		}
		return value;
	}

	private static double getRealElapsedAvgValue(List<Pack> result) {
		double totalValue = 0.0d;
		double tpsSum = 0.0d;
		for (Pack p : result) {
			MapPack m = (MapPack) p;
			ListValue objHashLv = m.getList("objHash");
			ListValue valueLv = m.getList("value");
			for (int i = 0; i < objHashLv.size(); i++) {
				int objHash = (int) objHashLv.getLong(i);
				double elapsed = CastUtil.cdouble(valueLv.get(i));
				AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
				if (agent == null) {
					continue;
				}
				final List<Double> tpsValue = new ArrayList<Double>(1);
				TcpProxy tcp = TcpProxy.getTcpProxy(agent.getServerId());
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("counter", CounterConstants.WAS_TPS);
					param.put("timetype", TimeTypeEnum.REALTIME);
					tcp.process(RequestCmd.COUNTER_REAL_TIME, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							Value v = in.readValue();
							if (v != null && v.getValueType() != ValueEnum.NULL) {
								tpsValue.add(CastUtil.cdouble(v));
							}
						}
					});

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				double tps = tpsValue.get(0);
				totalValue += (elapsed * tps);
				tpsSum += tps;
			}
		}
		if (totalValue == 0 || tpsSum == 0) {
			return 0;
		}
		return totalValue / tpsSum;
	}

	public static Map<Long, Double> getLoadTotalMap(String counter, List<Pack> result, String mode, byte timeTypeCode) {
		// if (CounterConstants.WAS_ELAPSED_TIME.equalsIgnoreCase(counter)) {
		// return getLoadElapsedAvgMap(result, timeTypeCode);
		// }
		long now = TimeUtil.getCurrentTime();
		TimedSeries<Integer, Double> sereis = new TimedSeries<Integer, Double>();
		for (Pack p : result) {
			MapPack m = (MapPack) p;
			int objHash = m.getInt("objHash");
			ListValue timeLv = m.getList("time");
			ListValue valueLv = m.getList("value");
			for (int i = 0; i < timeLv.size(); i++) {
				long time = CastUtil.clong(timeLv.get(i));
				double value = CastUtil.cdouble(valueLv.get(i));
				if (time > now) {
					break;
				}
				sereis.add(objHash, time, value);
			}
		}
		Map<Long, Double> tempMap = new HashMap<Long, Double>();
		if (sereis.getSeriesCount() > 0) {
			long stime = sereis.getMinTime();
			long etime = sereis.getMaxTime();
			boolean isAvg = "avg".equals(mode);
			while (stime <= etime) {
				double sum = 0.0d;
				List<Double> list = sereis.getInTimeList(stime, getKeepTime(timeTypeCode));
				for (int i = 0; i < list.size(); i++) {
					sum += list.get(i);
				}
				if (isAvg) {
					if (list.size() > 0) {
						tempMap.put(stime, sum / list.size());
					}
				} else {
					tempMap.put(stime, sum);
				}
				stime += TimeTypeEnum.getTime(timeTypeCode);
			}
		}
		return new TreeMap<Long, Double>(tempMap);
	}
	
	public static Map<Long, Double> getLoadMinOrMaxMap(List<Pack> result, String mode, byte timeTypeCode) {
		long now = TimeUtil.getCurrentTime();
		TimedSeries<Integer, Double> sereis = new TimedSeries<Integer, Double>();
		for (Pack p : result) {
			MapPack m = (MapPack) p;
			int objHash = m.getInt("objHash");
			ListValue timeLv = m.getList("time");
			ListValue valueLv = m.getList("value");
			for (int i = 0; i < timeLv.size(); i++) {
				long time = CastUtil.clong(timeLv.get(i));
				double value = CastUtil.cdouble(valueLv.get(i));
				if (time > now) {
					break;
				}
				sereis.add(objHash, time, value);
			}
		}
		Map<Long, Double> tempMap = new HashMap<Long, Double>();
		if (sereis.getSeriesCount() > 0) {
			long stime = sereis.getMinTime();
			long etime = sereis.getMaxTime();
			boolean isMinMode = "min".equalsIgnoreCase(mode);
			while (stime <= etime) {
				double pivot = isMinMode ? Double.MAX_VALUE : Double.MIN_VALUE;
				List<Double> list = sereis.getInTimeList(stime, getKeepTime(timeTypeCode));
				for (int i = 0; i < list.size(); i++) {
					double v = list.get(i);
					if (isMinMode && pivot > v) {
						pivot = v;
						continue;
					} 
					if (!isMinMode && v > pivot) {
						pivot = v;
						continue;
					}
				}
				if (list.size() > 0) {
					tempMap.put(stime, pivot);
				}
				stime += TimeTypeEnum.getTime(timeTypeCode);
			}
		}
		return new TreeMap<Long, Double>(tempMap);
	}

	private static long getKeepTime(byte timeType) {
		switch (timeType) {
		case TimeTypeEnum.REALTIME:
			return 10000;
		case TimeTypeEnum.ONE_MIN:
			return DateUtil.MILLIS_PER_MINUTE + 3000;
		case TimeTypeEnum.FIVE_MIN:
			return DateUtil.MILLIS_PER_MINUTE * 5 + 3000;
		case TimeTypeEnum.TEN_MIN:
			return DateUtil.MILLIS_PER_MINUTE * 10 + 3000;
		case TimeTypeEnum.HOUR:
			return DateUtil.MILLIS_PER_HOUR + 3000;
		default:
			return 30 * 10000;
		}
	}

	public static void addShowTotalValueListener(FigureCanvas canvas, final XYGraph xyGraph) {
		final DefaultToolTip toolTip = new DefaultToolTip(canvas, DefaultToolTip.RECREATE, true);
		toolTip.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		toolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		canvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				toolTip.hide();
			}

			public void mouseDown(MouseEvent e) {
				double x = xyGraph.primaryXAxis.getPositionValue(e.x, false);
				double y = xyGraph.primaryYAxis.getPositionValue(e.y, false);
				if (x < 0 || y < 0) {
					return;
				}
				double minDistance = 30.0d;
				long time = 0;
				double value = 0;
				Trace t = xyGraph.getPlotArea().getTraceList().get(0);
				if (t == null) {
					return;
				}
				ISample s = getNearestPoint(t.getDataProvider(), x);
				if (s != null) {
					int x2 = xyGraph.primaryXAxis.getValuePosition(s.getXValue(), false);
					int y2 = xyGraph.primaryYAxis.getValuePosition(s.getYValue(), false);
					double distance = ScouterUtil.getPointDistance(e.x, e.y, x2, y2);
					if (minDistance > distance) {
						minDistance = distance;
						time = (long) s.getXValue();
						value = s.getYValue();
					}
				}
				if (t != null) {
					toolTip.setText("Time : " + DateUtil.format(time, "HH:mm:ss")
							+ "\nValue : " +  FormatUtil.print(value, "#,###.##"));
					toolTip.show(new Point(e.x, e.y));
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});
	}

	public static void collectGroupObjcts(String grpName, Map<Integer, ListValue> serverObjMap) {
		serverObjMap.clear();
		Set<Integer> objHashs = GroupManager.getInstance().getObjectsByGroup(grpName);
		for (int objHash : objHashs) {
			AgentObject agentObj = AgentModelThread.getInstance().getAgentObject(objHash);
			if (agentObj == null) {
				continue;
			}
			int serverId = agentObj.getServerId();
			ListValue lv = serverObjMap.get(serverId);
			if (lv == null) {
				lv = new ListValue();
				serverObjMap.put(serverId, lv);
			}
			lv.add(objHash);
		}
	}

	public static String getFullObjName(int objHash) {
		try {
			AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
			String objName = agent.getObjName();
			return objName;
		} catch (Exception e) {
		}
		return "";
	}

	public static String getShortObjName(int objHash) {
		try {
			AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
			String objName = agent.getObjName();
			if (objName.lastIndexOf("/") > -1) {
				objName = objName.substring(objName.lastIndexOf("/") + 1, objName.length());
			}
			return objName;
		} catch (Exception e) {
		}
		return "";
	}

	static Set<String> liveMenuSet = new HashSet<String>();
	static {
		liveMenuSet.add(CounterConstants.REAL_TIME);
		liveMenuSet.add(CounterConstants.TODAY);
		liveMenuSet.add(CounterConstants.REAL_TIME_ALL);
		liveMenuSet.add(CounterConstants.REAL_TIME_TOTAL);
		liveMenuSet.add(CounterConstants.TODAY_ALL);
		liveMenuSet.add(CounterConstants.TODAY_TOTAL);
	}

	public static boolean isLiveMenu(String menu) {
		boolean result = false;
		if (liveMenuSet.contains(menu)) {
			result = true;
		}
		return result;
	}

	public static String getActionName(String key) {
		if (CounterConstants.REAL_TIME_ALL.equals(key) || CounterConstants.PAST_TIME_ALL.equals(key)) {
			return MenuStr.TIME_ALL;
		} else if (CounterConstants.REAL_TIME_TOTAL.equals(key) || CounterConstants.PAST_TIME_TOTAL.equals(key)) {
			return MenuStr.TIME_TOTAL;
		} else if (CounterConstants.TODAY_ALL.equals(key) || CounterConstants.PAST_DATE_ALL.equals(key)) {
			return MenuStr.DAILY_ALL;
		} else if (CounterConstants.TODAY_TOTAL.equals(key) || CounterConstants.PAST_DATE_TOTAL.equals(key)) {
			return MenuStr.DAILY_TOTAL;
		} else {
			return "";
		}
	}

	public static ImageDescriptor getActionIconName(String key) {
		if (CounterConstants.REAL_TIME_ALL.equals(key) || CounterConstants.TODAY_ALL.equals(key)) {
			return ImageUtil.getImageDescriptor(Images.all);
		} else if (CounterConstants.REAL_TIME_TOTAL.equals(key) || CounterConstants.TODAY_TOTAL.equals(key)) {
			return ImageUtil.getImageDescriptor(Images.sum);
		} else if (CounterConstants.PAST_TIME_ALL.equals(key) || CounterConstants.PAST_DATE_ALL.equals(key)) {
			return ImageUtil.getImageDescriptor(Images.all);
		} else if (CounterConstants.PAST_TIME_TOTAL.equals(key) || CounterConstants.PAST_DATE_TOTAL.equals(key)) {
			return ImageUtil.getImageDescriptor(Images.sum);
		} else {
			return null;
		}
	}

	public static String mapPackToTableString(MapPack m) {
		if (m == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		ArrayList<String> keyList = new ArrayList<String>();
		HashMap<Integer, Integer> maxLenMap = new HashMap<Integer, Integer>();
		LinkedMap<Integer, ArrayList<String>> valueMap = new LinkedMap<Integer, ArrayList<String>>();
		int width = 1;
		Iterator<String> itr = m.keys();
		int index = 0;
		while (itr.hasNext()) {
			String key = itr.next();
			Value v = m.get(key);
			int maxLen = key.length();
			if (v instanceof ListValue) {
				ListValue lv = (ListValue) v;
				for (int i = 0; i < lv.size(); i++) {
					String s = lv.get(i).toString();
					if (s.length() > maxLen) {
						maxLen = s.length();
					}
					ArrayList<String> list = valueMap.get(i);
					if (list == null) {
						list = new ArrayList<String>();
						valueMap.put(i, list);
					}
					list.add(s);
				}
				width += (maxLen + 3);
				maxLenMap.put(index, maxLen);
				keyList.add(key);
				index++;
			}
		}
		if (keyList.size() > 0) {
			// 1. draw header
			sb.append("\n+");
			for (int i = 0; i < width - 2; i++) {
				sb.append("-");
			}
			sb.append("+\n");

			sb.append("|");
			for (int i = 0; i < keyList.size(); i++) {
				int maxLen = maxLenMap.get(i);
				String key = keyList.get(i);
				sb.append(" " + key);
				int gap = maxLen - key.length();
				while (gap > 0) {
					sb.append(" ");
					gap--;
				}
				sb.append(" |");
			}
			sb.append("\n");

			for (int i = 0; i < width; i++) {
				sb.append("-");
			}
			sb.append("\n");

			// 2. draw content
			while (valueMap.size() > 0) {
				ArrayList<String> list = valueMap.removeFirst();
				sb.append("|");
				for (int i = 0; i < list.size(); i++) {
					int maxLen = maxLenMap.get(i);
					String s = list.get(i);
					sb.append(" " + s);
					int gap = maxLen - s.length();
					while (gap > 0) {
						sb.append(" ");
						gap--;
					}
					sb.append(" |");
				}
				sb.append("\n");
				if (valueMap.size() == 0) {
					sb.append("+");
					for (int i = 0; i < width - 2; i++) {
						sb.append("-");
					}
					sb.append("+\n");
				} else {
					for (int i = 0; i < width; i++) {
						sb.append("-");
					}
					sb.append("\n");
				}
			}
		}
		return sb.toString();
	}

	public static double getNearestValue(IDataProvider provider, double time) {
		int high = provider.getSize() - 1;
		int low = 0;
		while (true) {
			int mid = (high + low) / 2;
			ISample s = provider.getSample(mid);
			double x = s.getXValue();
			if (x == time) {
				return s.getYValue();
			} else {
				if (x > time) {
					high = mid;
				} else {
					low = mid;
				}
				if ((high - low) <= 1) {
					ISample highSample = provider.getSample(high);
					ISample lowSample = provider.getSample(low);
					if (highSample == null && lowSample == null) {
						return 0.0d;
					}
					if (highSample == null) {
						return lowSample.getYValue();
					}
					if (lowSample == null) {
						return highSample.getYValue();
					}
					double highGap = highSample.getXValue() - time;
					double lowGqp = time - lowSample.getXValue();
					if (highGap < lowGqp) {
						return highSample.getYValue();
					} else {
						return lowSample.getYValue();
					}
				}
			}
		}
	}
	
	public static ISample getNearestPoint(IDataProvider provider, double time) {
		int high = provider.getSize() - 1;
		int low = 0;
		while (high >= low) {
			int mid = (high + low) / 2;
			ISample s = provider.getSample(mid);
			double x = s.getXValue();
			if (x == time) {
				return s;
			} else {
				if (x > time) {
					high = mid;
				} else {
					low = mid;
				}
				if ((high - low) <= 1) {
					ISample highSample = provider.getSample(high);
					ISample lowSample = provider.getSample(low);
					if (highSample == null && lowSample == null) {
						return null;
					}
					if (highSample == null) {
						return lowSample;
					}
					if (lowSample == null) {
						return highSample;
					}
					double highGap = highSample.getXValue() - time;
					double lowGqp = time - lowSample.getXValue();
					if (highGap < lowGqp) {
						return highSample;
					} else {
						return lowSample;
					}
				}
			}
		}
		return null;
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = "KMGTPE".charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %s", bytes / Math.pow(unit, exp), pre);
	}

	public static String humanReadableByteCount(double bytes, boolean si) {
		int unit = 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = "KMGTPE".charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %s", bytes / Math.pow(unit, exp), pre);
	}
	
	public static void addHorizontalRangeListener(PlotArea plotArea, PropertyChangeListener listener, boolean withZoom) {
		plotArea.setZoomType(ZoomType.HORIZONTAL_ZOOM);
		plotArea.enableZoom(withZoom);
		plotArea.addPropertyChangeListener("horizontal_range", listener);
	}
	
	public static double getPointDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}
	
	public static DoubleFunction<Comparator<Trace>> comparatorByTime = (time) -> (t1, t2) -> {
		ISample sample1 = ScouterUtil.getNearestPoint(t1.getDataProvider(), time);
		ISample sample2 = ScouterUtil.getNearestPoint(t2.getDataProvider(), time);
		return Double.compare(sample2.getYValue(), sample1.getYValue());
	};

   public static ToDoubleFunction<Trace> nearestPointYValueFunc(double time) {
	   return t -> ScouterUtil.getNearestValue(t.getDataProvider(), time);
   }
}
