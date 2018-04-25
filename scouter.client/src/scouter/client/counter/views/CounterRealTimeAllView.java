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
package scouter.client.counter.views;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import scouter.client.Images;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.AgentColorManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.TextProxy;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.MenuUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.views.ScouterViewPart;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.HashUtil;
import scouter.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CounterRealTimeAllView extends ScouterViewPart implements Refreshable, IObjectCheckListener {
	public static final String ID = CounterRealTimeAllView.class.getName();

	protected RefreshThread thread = null;

	private int serverId;
	private String objType;
	private String counter;

	private boolean manualRefresh = false;
	
	boolean isActive = false;
	
	protected XYGraph xyGraph;
	protected Map<Integer, CircularBufferDataProvider> datas = new HashMap<Integer, CircularBufferDataProvider>();
	protected FigureCanvas canvas;
	Trace nearestTrace;
	
	ArrayList<Trace> traces = new ArrayList<Trace>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
		this.counter = ids[2];
	}

	
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		parent.setLayout(layout);
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		canvas = new FigureCanvas(parent);
		canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					if (ChartUtil.isShowLegendAllowSize(r.width, r.height)) {
						xyGraph.setShowLegend(true);
					} else {
						xyGraph.setShowLegend(false);
					}
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterRealTimeAllView.this.setContentDescription(desc);
					} else {
						CounterRealTimeAllView.this.setContentDescription("");
					}
					r = canvas.getClientArea();
					xyGraph.setSize(r.width, r.height);
					lock = false;
				}
			}
			
			public void controlMoved(ControlEvent e) {
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
		
		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
		xyGraph.primaryYAxis.setFormatPattern("#,##0");
		
		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));
		
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
				for (Trace t : traces) {
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
				case SWT.F5:
					manualRefresh = true;
					for (int i = 0; i < traces.size(); i++) {
						xyGraph.removeTrace(traces.get(i));
					}
					thread.interrupt();
					break;
				}
			}
		});
		
		IToolBarManager man =  getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				manualRefresh = true;
				for (int i = 0; i < traces.size(); i++) {
					xyGraph.removeTrace(traces.get(i));
				}
				thread.interrupt();
			}
		});
		
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if (server != null) {
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ" + svrName + " | (Current All) " + counterDisplay + (!"".equals(counterUnit) ? " (" + counterUnit + ")" : "");
		try {
			setViewTab(objType, counter, serverId);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objType, counter, 0, 0);
		ObjectSelectManager.getInstance().addObjectCheckStateListener(this);
		thread = new RefreshThread(this, 2000);
		thread.setName(this.toString() + " - " + "objType:" + objType + ", counter:" + counter + ", serverId:" + serverId);
		thread.start();
	}
	
	public void refresh() {
		if (manualRefresh) {
			manualRefresh = false;
			traces.clear();
			datas.clear();
			getPrevAllPerf();
		}
		final HashMap<Integer, Value> values = new HashMap<Integer, Value>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();

			param.put("objType", objType);
			param.put("counter", counter);

			MapPack out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_REAL_TIME_ALL, param);
			isActive = false;
			if (out != null) {
				ListValue objHash = out.getList("objHash");
				ListValue v = out.getList("value");
				for (int i = 0; i < objHash.size(); i++) {
					values.put(CastUtil.cint(objHash.get(i)), v.get(i));
					isActive = true;
				}
			}
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}

		ExUtil.exec(canvas, new Runnable() {
			public void run() {

				if (isActive) {
					setActive();
				} else {
					setInactive();
				}

				long now = TimeUtil.getCurrentTime(serverId);
				xyGraph.primaryXAxis.setRange(now - DateUtil.MILLIS_PER_MINUTE * 5, now + 1);
				Iterator<Integer> itr = values.keySet().iterator();
				while (itr.hasNext()) {
					int objHash = itr.next();
					Value value = values.get(objHash);
					if (value != null && value.getValueType() != ValueEnum.NULL) {
						CircularBufferDataProvider data = getDataProvider(objHash);
						data.addSample(new Sample(now, CastUtil.cdouble(value)));
					}

				}
				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					double max = getMaxValue();
					xyGraph.primaryYAxis.setRange(0, max);
				}
				CounterRealTimeAllView.this.redraw();
			}
		});

		checkSettingChange();
	}
	
	private void checkSettingChange() {
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
				synchronized (traces) {
					for (Trace t : traces) {
						if (nearestTrace == null && t.getLineWidth() != width) {
							t.setLineWidth(width);
						}
						int objHash = HashUtil.hash(t.getName());
						AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
						if (agent == null || agent.isAlive() == false || agent.getColor() == null) {
							return;
						}
						if (t.getTraceColor() != agent.getColor()) {
							t.setTraceColor(agent.getColor());
						}
					}
				}
			}
		});
	}

	private void getPrevAllPerf() {
		final ArrayList<MapPack> values = new ArrayList<MapPack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			long etime = TimeUtil.getCurrentTime(serverId);
			long stime = etime - DateUtil.MILLIS_PER_MINUTE * 5;
			param.put("stime", stime);
			param.put("etime", etime);
			param.put("objType", objType);
			param.put("counter", counter);
			tcp.process(RequestCmd.COUNTER_PAST_TIME_ALL, param,
					new INetReader() {
						public void process(DataInputX in) throws IOException {
							MapPack mpack = (MapPack) in.readPack();
							values.add(mpack);
						};
					});
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}

		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				for (MapPack mpack : values) {
					int objHash = mpack.getInt("objHash");
					ListValue time = mpack.getList("time");
					ListValue value = mpack.getList("value");
					if (time == null || time.size() < 1) {
						continue;
					}
					CircularBufferDataProvider provider = getDataProvider(objHash);
					provider.clearTrace();
					for (int i = 0; time != null && i < time.size(); i++) {
						long x = time.getLong(i);
						double y = value.getDouble(i);
						provider.addSample(new Sample(x, y));
					}
				}
			}
		});
	}

	private CircularBufferDataProvider getDataProvider(int objHash) {
		CircularBufferDataProvider data = datas.get(objHash);
		if (data == null) {
			data = new CircularBufferDataProvider(true);
			datas.put(objHash, data);

			data.setBufferSize(155);
			data.setCurrentXDataArray(new double[] {});
			data.setCurrentYDataArray(new double[] {});
			String name = StringUtil.trimToEmpty(TextProxy.object.getLoadText(
					DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash,
					serverId));
			final Trace trace = new Trace(name, xyGraph.primaryXAxis, xyGraph.primaryYAxis, data);
			trace.setPointStyle(PointStyle.NONE);

			trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace.setTraceType(TraceType.SOLID_LINE);
			trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			
			xyGraph.addTrace(trace);
			traces.add(trace);
		}
		return data;
	}

	private double getMaxValue() {
		Iterator<Integer> objHashs = datas.keySet().iterator();
		double max = 0.0;
		Range xRange = xyGraph.primaryXAxis.getRange();
		double lower = xRange.getLower();
		double upper = xRange.getUpper();
		while (objHashs.hasNext()) {
			int objHash = objHashs.next();
			CircularBufferDataProvider data = datas.get(objHash);
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
		return ChartUtil.getMaxValue(max);
	}

	public void setFocus() {
		statusMessage = desc + " - setInput(String objType:" + objType
				+ ", String counter:" + counter + ", int serverId:" + serverId
				+ ")";
		super.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
		ObjectSelectManager.getInstance().removeObjectCheckStateListener(this);
	}

	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}

	public void notifyChangeState() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				for (Trace t : traces) {
					String objName = t.getName();
					if (ObjectSelectManager.getInstance().isUnselectedObject(HashUtil.hash(objName))) {
						t.setVisible(false);
					} else {
						t.setVisible(true);
					}
				}
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						redraw();
					}
				});
			}
		});
	}
}
