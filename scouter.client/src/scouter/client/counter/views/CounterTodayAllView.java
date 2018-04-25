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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import scouter.client.Images;
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
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.HashUtil;
import scouter.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CounterTodayAllView extends ScouterViewPart implements Refreshable, IObjectCheckListener {
	public static final String ID = CounterTodayAllView.class.getName();

	protected String objType;
	protected String counter;
	protected int serverId;

	protected RefreshThread thread;

	IWorkbenchWindow window;
	IToolBarManager man;
	Trace nearestTrace;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
		this.counter = ids[2];
	}

	boolean isActive = false;
	
	public void refresh() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		final ArrayList<MapPack> values = new ArrayList<MapPack>();
		try {
			MapPack param = new MapPack();
			param.put("objType", objType);
			param.put("counter", counter);
			isActive = false;
			tcp.process(RequestCmd.COUNTER_TODAY_ALL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					values.add((MapPack) in.readPack());
					isActive = true;
				}
			});
			
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				
				if(isActive){
					setActive();
				}else{
					setInactive();
				}
				
				String date = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
				try {
					double max = 0;
					long stime = DateUtil.getTime(date, "yyyyMMdd");
					long etime = stime + DateUtil.MILLIS_PER_DAY;
					xyGraph.primaryXAxis.setRange(stime, etime);
					long now = TimeUtil.getCurrentTime(serverId);
					for (MapPack pack : values) {
						int objHash = pack.getInt("objHash");
						//String objName = pack.getText("objName");
						ListValue time = pack.getList("time");
						ListValue value = pack.getList("value");
						CircularBufferDataProvider data = getDataProvider(objHash);
						data.clearTrace();
						for (int i = 0; time != null && i < time.size(); i++) {
							long x = CastUtil.clong(time.get(i));
							double y =  CastUtil.cdouble(value.get(i));
							if (x > now) {
								break;
							}
							data.addSample(new Sample(x, y));
						}
						max = Math.max(ChartUtil.getMax(data.iterator()), max);
					}
					if (CounterUtil.isPercentValue(objType, counter)) {
						xyGraph.primaryYAxis.setRange(0, 100);
					} else {
							xyGraph.primaryYAxis.setRange(0, max);
					}
				} catch (Exception e) { }
				canvas.redraw();
				xyGraph.repaint();
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
	
	protected Map<Integer, CircularBufferDataProvider> datas = new HashMap<Integer, CircularBufferDataProvider>();
	protected XYGraph xyGraph;
	protected Trace trace;
	protected FigureCanvas canvas;

	public void createPartControl(Composite parent) {
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		man = getViewSite().getActionBars().getToolBarManager();
		
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				for(int i = 0 ; i < traces.size() ; i++){
					xyGraph.removeTrace(traces.get(i));
				}
				traces.clear();
				datas.clear();
				thread.interrupt();
			}
		});
		
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		parent.setLayout(layout);
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		canvas = new FigureCanvas(parent);
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		
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
						CounterTodayAllView.this.setContentDescription(desc);
					} else {
						CounterTodayAllView.this.setContentDescription("");
					}
					r = canvas.getClientArea();
					lock = false;
				}
				xyGraph.setSize(r.width, r.height);
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

		ObjectSelectManager.getInstance().addObjectCheckStateListener(this);
		
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
							+ "\nTime : " + DateUtil.format(time, "HH:mm")
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
					for(int i = 0 ; i < traces.size() ; i++){
						xyGraph.removeTrace(traces.get(i));
					}
					traces.clear();
					datas.clear();
					thread.interrupt();
					break;
				}
			}
		});
		
		String date = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+" | (Today All) [" + date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + "]" + counterDisplay+(!"".equals(counterUnit)?" ("+counterUnit+")":"");
		try {
			setViewTab(objType, counter, serverId);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		long from = DateUtil.yyyymmdd(date);
		long to = from + DateUtil.MILLIS_PER_DAY;

		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objType, counter, from, to);
		thread = new RefreshThread(this, 10000);
		thread.setName(this.toString() + " - " + "objType:"+objType + ", counter:"+counter + ", serverId:"+serverId);
		thread.start();
	}
	
	ArrayList<Trace> traces = new ArrayList<Trace>();
	
	private CircularBufferDataProvider getDataProvider(int objHash) {
		CircularBufferDataProvider data = datas.get(objHash);
		if (data == null) {
			data = new CircularBufferDataProvider(true);
			datas.put(objHash, data);

			data.setBufferSize(288);
			data.setCurrentXDataArray(new double[] {});
			data.setCurrentYDataArray(new double[] {});
			String name = StringUtil.trimToEmpty(TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId));
			Trace trace = new Trace(name, xyGraph.primaryXAxis, xyGraph.primaryYAxis, data);
			trace.setPointStyle(PointStyle.NONE);
			trace.getXAxis().setFormatPattern("HH:mm:ss");
			trace.getYAxis().setFormatPattern("#,##0");

			trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace.setTraceType(TraceType.SOLID_LINE);
			trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));

			xyGraph.addTrace(trace);
			traces.add(trace);
		}
		return data;

	}

	public void setFocus() {
		statusMessage = desc + " - setInput(String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
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