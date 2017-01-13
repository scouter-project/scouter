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
package scouter.client.views;

import java.util.*;
import java.util.stream.Collectors;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.ServiceGroupColorManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.LinkedMap;

import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static scouter.client.util.ScouterUtil.nearestPointYValueFunc;

public abstract class AbstractServiceGroupTPSView extends ViewPart implements Refreshable {
	
	public final static String ID = AbstractServiceGroupTPSView.class.getName();
	
	private final static int BUFFER_SIZE = 200;
	protected RefreshThread thread;
	
	protected XYGraph xyGraph;
	protected FigureCanvas canvas;
	
	protected Map<String, Trace> traces = new HashMap<String, Trace>();
	private LinkedMap<String, StackValue> stackValueMap = new LinkedMap<String, StackValue>();
	
	private int manualRangeCount;
	private double manualY;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(UIUtil.formLayout(0, 0));
		canvas = new FigureCanvas(parent);
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.setLayoutData(UIUtil.formData(0, 0, 0, 0, 100, 0, 100, 0));
		canvas.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					xyGraph.setSize(r.width, r.height);
					lock = false;
				}
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		final DefaultToolTip toolTip = new DefaultToolTip(canvas, DefaultToolTip.RECREATE, true);
		toolTip.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		toolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		canvas.addMouseListener(new MouseListener() {
			
			String selectedName;
			
			public void mouseUp(MouseEvent e) {
				if (selectedName == null) {
					return;
				}
				Trace trace = traces.get(selectedName);
				trace.setTraceColor(ServiceGroupColorManager.getInstance().assignColor(selectedName));
				toolTip.hide();
				selectedName = null;
			}
			public void mouseDown(MouseEvent e) {
				double xValue = xyGraph.primaryXAxis.getPositionValue(e.x, false);
				double yValue = xyGraph.primaryYAxis.getPositionValue(e.y, false);
				if (xyGraph.primaryXAxis.getRange().getLower() > xValue || xyGraph.primaryXAxis.getRange().getUpper() < xValue) return;
				if (xyGraph.primaryYAxis.getRange().getLower() > yValue || xyGraph.primaryYAxis.getRange().getUpper() < yValue) return;
				
				List<Trace> sortedTraces = traces.values()
						.stream()
						.sorted(comparingDouble(nearestPointYValueFunc(xValue)).reversed())
						.collect(toList());
				
				ISample topSample = ScouterUtil.getNearestPoint(sortedTraces.get(0).getDataProvider(), xValue);
				double valueTime = topSample.getXValue();
				double total = topSample.getYValue();
				if (yValue > total) return;
				int i = 0;
				Trace selectedTrace = null;
				for (; i < sortedTraces.size(); i++) {
					Trace t = sortedTraces.get(i);
					double stackValue = ScouterUtil.getNearestValue(t.getDataProvider(), valueTime);
					if (stackValue < yValue) {
						i = i - 1;
						selectedTrace = sortedTraces.get(i);
						break;
					}
				}
				if (selectedTrace == null) {
					selectedTrace = sortedTraces.get(i-1);
				}
				selectedName = selectedTrace.getName();
				double value = ScouterUtil.getNearestValue(selectedTrace.getDataProvider(), valueTime);
				if (i < sortedTraces.size() - 1) {
					int j = i + 1;
					double nextStackValue = value;
					while (nextStackValue == value && j < sortedTraces.size()) {
						nextStackValue = ScouterUtil.getNearestValue(sortedTraces.get(j).getDataProvider(), valueTime);
						j++;
					}
					if (nextStackValue < value) {
						value = value - nextStackValue; 
					}
				}
				double percent = value * 100 / total;
				selectedTrace.setTraceColor(ColorUtil.getInstance().getColor("dark magenta"));
				toolTip.setText(DateUtil.format(CastUtil.clong(valueTime), "HH:mm:ss") 
						+ "\n" + selectedName 
						+ "\n" + FormatUtil.print(value, "#,###0.#") + "(" + FormatUtil.print(percent, "##0.0") + " %)");
				toolTip.show(new Point(e.x, e.y));
			}
			public void mouseDoubleClick(MouseEvent e) {}
		});
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				switch (e.keyCode) {
					case 16777217:// UP Key
						double max = xyGraph.primaryYAxis.getRange().getUpper();
						if (max > 10000) {
							manualY = max + 1000;
						} else if (max > 1000) {
							manualY = max + 100;
						} else if (max > 100) {
							manualY = max + 10;
						} else if (max == 3) {
							manualY = 5;
						} else {
							manualY = max + 5;
						}
						manualRangeCount = 5;
						xyGraph.primaryYAxis.setRange(0, manualY);
						break;
					case 16777218: // DOWN Key
						max = xyGraph.primaryYAxis.getRange().getUpper();
						if (max > 10000) {
							manualY = max - 1000;
						} else if (max > 1000) {
							manualY =max - 100;
						} else if (max > 100) {
							manualY =max - 10;
						} else {
							manualY = (max - 5) > 3 ? max -5 : 3;
						}
						manualRangeCount = 5;
						xyGraph.primaryYAxis.setRange(0, manualY);
						break;
				}
			}
		});
		xyGraph = new XYGraph();
		xyGraph.setShowLegend(true);
		xyGraph.setShowTitle(false);
		canvas.setContents(xyGraph);
		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);
		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));
		thread = new RefreshThread(this, 2000);
		thread.start();
	}

	public void setFocus() {}
	
	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}
	
	boolean stopRefresh = false;
	
	public void refresh() {
		if (stopRefresh) {
			return;
		}
		MapPack m = fetch();
		if (m == null) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.inactive);
					long now = TimeUtil.getCurrentTime();
					xyGraph.primaryXAxis.setRange(now - DateUtil.MILLIS_PER_FIVE_MINUTE, now + 1);
				}
			});
			return;
		}
		removeDeadGroup(m);
		 processThroughputData(m);
	}
	
	public abstract MapPack fetch();
	
	private void removeDeadGroup(MapPack m) {
		ListValue nameLv = m.getList("name");

		ArrayList<String> grpSet = new ArrayList<String>();
		Enumeration<String> enu =stackValueMap.keys();
		while(enu.hasMoreElements()){
			grpSet.add(enu.nextElement());
		}
		for (int i = 0; i < nameLv.size(); i++) {
			String name = nameLv.getString(i);
			grpSet.remove(name);
		}
		for (String dead : grpSet) {
			stackValueMap.remove(dead);
			final Trace t = traces.get(dead);
			if (t == null) continue;
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					xyGraph.removeTrace(t);
				}
			});
			traces.remove(dead);
		}
	}
	
	private void processThroughputData(MapPack m) {
		ListValue nameLv = m.getList("name");
		ListValue countLv = m.getList("count");
		final long now = m.getLong("time");
		final long stime = now - DateUtil.MILLIS_PER_FIVE_MINUTE;
		for (int i = 0; i < nameLv.size(); i++) {
			String name = nameLv.getString(i);
			double value = CastUtil.cdouble(countLv.get(i)) / 30.0d;
			if (stackValueMap.containsKey(name)) {
				StackValue sv = stackValueMap.get(name);
				sv.actualValue = value;
				sv.lastUpdateTime = now;
			} else {
				StackValue sv = new StackValue();
				sv.actualValue = value;
				sv.lastUpdateTime = now;
				stackValueMap.putFirst(name, sv);
			}
		}
		Enumeration<String> itr = stackValueMap.keys();
		final LinkedMap<String, StackValue> tempMap = new LinkedMap<String, StackValue>();
		double stackValue = 0.0;
		while (itr.hasMoreElements()) {
			String name = itr.nextElement();
			StackValue sv = stackValueMap.get(name);
			if (sv.lastUpdateTime == now) {
				stackValue += sv.actualValue;
				sv.stackedValue = stackValue;
				tempMap.putFirst(name, sv);
			}
		}
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				setTitleImage(Images.active);
				xyGraph.primaryXAxis.setRange(stime, now + 1);
				Enumeration<String> itr = tempMap.keys();
				while (itr.hasMoreElements()) {
					String name = itr.nextElement();
					StackValue sv = tempMap.get(name);
					CircularBufferDataProvider provider = (CircularBufferDataProvider) getTrace(name).getDataProvider();
					provider.addSample(new Sample(now, sv.stackedValue));
				}
				xyGraph.primaryYAxis.setRange(0, getMaxValue());
			}
		});
	}
	
	private Trace getTrace(String name) {
		Trace trace = traces.get(name);
		if (trace == null) {
			CircularBufferDataProvider provider = new CircularBufferDataProvider(true);
			provider.setBufferSize(BUFFER_SIZE);
			trace = new Trace(name, xyGraph.primaryXAxis, xyGraph.primaryYAxis, provider);
			trace.setPointStyle(PointStyle.NONE);
			trace.getXAxis().setFormatPattern("HH:mm:ss");
			trace.getYAxis().setFormatPattern("#,##0");
			trace.setTraceType(TraceType.AREA);
			trace.setAreaAlpha(255);
			trace.setTraceColor(ServiceGroupColorManager.getInstance().assignColor(name));
			xyGraph.addTrace(trace);
			traces.put(name, trace);
		}
		return trace;
	}
	
	private double getMaxValue() {
		Range xRange = xyGraph.primaryXAxis.getRange();
		double lower = xRange.getLower();
		double upper = xRange.getUpper();
		if (manualRangeCount > 0 && manualY > 0) {
			manualRangeCount--;
			return manualY;
		}
		double max = 0.0;
		Iterator<String> itr = traces.keySet().iterator();
		while (itr.hasNext()) {
			String name = itr.next();
			CircularBufferDataProvider data = (CircularBufferDataProvider) traces.get(name).getDataProvider();
			if (data != null) {
				for (int inx = 0; inx < data.getSize(); inx++) {
					Sample sample = (Sample) data.getSample(inx);
					double x = sample.getXValue();
					if(x < lower || x > upper) {
						continue;
					}
					double y = sample.getYValue();
					if (y > max) {
						max = y;
					}
				}
			}
		}
		return ChartUtil.getGroupMaxValue(max);
	}
	
	static class StackValue {
		double actualValue;
		double stackedValue;
		long lastUpdateTime;
	}
}
