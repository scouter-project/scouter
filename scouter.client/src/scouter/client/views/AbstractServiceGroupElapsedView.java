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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.ServiceGroupColorManager;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
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

public abstract class AbstractServiceGroupElapsedView extends ViewPart implements Refreshable {
	
	public final static String ID = AbstractServiceGroupElapsedView.class.getName();
	
	private final static int BUFFER_SIZE = 200;
	protected RefreshThread thread;
	
	protected XYGraph xyGraph;
	protected FigureCanvas canvas;
	
	protected Map<String, Trace> traces = new HashMap<String, Trace>();
	Trace nearestTrace = null;
	
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
			public void mouseUp(MouseEvent e) {
				if (nearestTrace != null) {
					nearestTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
					nearestTrace = null;
				}
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
				for (Trace t : traces.values()) {
					ISample s = ScouterUtil.getNearestPoint(t.getDataProvider(), x);
					if (s != null) {
						int x2 = xyGraph.primaryXAxis.getValuePosition(s.getXValue(), false);
						int y2 = xyGraph.primaryYAxis.getValuePosition(s.getYValue(), false);
						double distance = ScouterUtil.getPointDistance(e.x, e.y, x2, y2);
						if (minDistance > distance) {
							minDistance = distance;
							nearestTrace = t;
							time = (long) s.getXValue();
							value = s.getYValue();
						}
					}
				}
				if (nearestTrace != null) {
					int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
					nearestTrace.setLineWidth(width + 2);
					toolTip.setText(nearestTrace.getName()
							+ "\nTime : " + DateUtil.format(time, "HH:mm:ss")
							+ "\nValue : " +  FormatUtil.print(value, "#,###.##"));
					toolTip.show(new Point(e.x, e.y));
				}
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
		processElapsedData(m);
	}
	
	public abstract MapPack fetch();
	
	private void removeDeadGroup(MapPack m) {
		ListValue nameLv = m.getList("name");

		ArrayList<String> grpSet = new ArrayList<String>();
		Iterator<String> enu = traces.keySet().iterator();
		while(enu.hasNext()){
			grpSet.add(enu.next());
		}
		for (int i = 0; i < nameLv.size(); i++) {
			String name = nameLv.getString(i);
			grpSet.remove(name);
		}
		for (String dead : grpSet) {
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
	
	private void processElapsedData(final MapPack m) {
		final ListValue nameLv = m.getList("name");
		final ListValue elapsedLv = m.getList("elapsed");
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				setTitleImage(Images.active);
				long now = m.getLong("time");
				long stime = now - DateUtil.MILLIS_PER_FIVE_MINUTE;
				xyGraph.primaryXAxis.setRange(stime, now + 1);
				for (int i = 0; i < nameLv.size(); i++) {
					String name = nameLv.getString(i);
					double value = CastUtil.cdouble(elapsedLv.get(i));
					CircularBufferDataProvider provider = (CircularBufferDataProvider) getTrace(name).getDataProvider();
					provider.addSample(new Sample(now, value));
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
			trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace.setTraceType(TraceType.SOLID_LINE);
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
}
