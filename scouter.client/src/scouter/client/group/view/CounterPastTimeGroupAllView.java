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
package scouter.client.group.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.IDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.group.GroupManager;
import scouter.client.model.AgentColorManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.TextProxy;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.client.views.ScouterViewPart;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.HashUtil;
import scouter.util.StringUtil;

public class CounterPastTimeGroupAllView extends ScouterViewPart implements CalendarDialog.ILoadCalendarDialog, IObjectCheckListener {

	public static final String ID = CounterPastTimeGroupAllView.class.getName();
			
	private String grpName;
	private String objType;
	private String counter;
	private long etime = TimeUtil.getCurrentTime();
	private long stime = etime - DateUtil.MILLIS_PER_FIVE_MINUTE;
	private Server defaultServer = ServerManager.getInstance().getDefaultServer();
	protected XYGraph xyGraph;
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	protected Map<Integer, Trace> traces = new HashMap<Integer, Trace>();
	private Map<Integer, ListValue> serverObjMap = new HashMap<Integer, ListValue>();
	protected FigureCanvas canvas;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] datas = secId.split("&");
		grpName = datas[0];
		objType = datas[1];
		counter = datas[2];
	}

	public void createPartControl(Composite parent) {
		String displayCounter = defaultServer.getCounterEngine().getCounterDisplayName(objType, counter);
		setPartName(grpName + " - " + displayCounter);
		setTitleImage(Images.getCounterImage(objType, counter, defaultServer.getId()));
		String unit = defaultServer.getCounterEngine().getCounterUnit(objType, counter);
		statusMessage = grpName + " | (PastTime) All " + displayCounter + (StringUtil.isNotEmpty(unit) ? "(" + unit + ")" : "");
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gLayout = new GridLayout(1, true);
		gLayout.horizontalSpacing = 0;
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		composite.setLayout(gLayout);
		createUpperMenu(composite);
		
		Composite chartComposite = new Composite(composite, SWT.NONE);
		chartComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		chartComposite.setLayout(UIUtil.formLayout(0, 0));
		
		chartComposite.setLayout(UIUtil.formLayout(0, 0));
		chartComposite.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		canvas = new FigureCanvas(chartComposite);
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		canvas.setLayoutData(UIUtil.formData(0, 0, 0, 0, 100, 0, 100, 0));
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
					r = canvas.getClientArea();
					xyGraph.setSize(r.width, r.height);
					lock = false;
				}
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					forceRefresh();
				}
			}
		});
		final DefaultToolTip toolTip = new DefaultToolTip(canvas, DefaultToolTip.RECREATE, true);
		toolTip.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		toolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		canvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				onDeselectObject();
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
				Trace nearestTrace = null;
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
					onSelectObject(HashUtil.hash(nearestTrace.getName()), nearestTrace.getName(), objType);
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
		});
		xyGraph = new XYGraph();
		xyGraph.setShowTitle(false);
		canvas.setContents(xyGraph);
		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);
		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				forceRefresh();
			}
		});
		man.add(new Separator());
		man.add(new Action("Duplicate", ImageUtil.getImageDescriptor(Images.copy)) {
			public void run() {
				ExUtil.exec(new Runnable() {
					public void run() {
						try {
							window.getActivePage().showView(
									CounterPastTimeGroupAllView.ID, grpName + "&" + objType + "&" + counter + "&" + TimeUtil.getCurrentTime(),
									IWorkbenchPage.VIEW_ACTIVATE);
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		ObjectSelectManager.getInstance().addObjectCheckStateListener(this);
		forceRefresh();
	}
	
	Label serverText, sDateText, sTimeText, eTimeText;
	CalendarDialog calDialog;
	Composite headerComp;
	private void createUpperMenu(Composite composite) {
		headerComp = new Composite(composite, SWT.NONE);
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerComp.setLayout(UIUtil.formLayout(0, 0));
		
		Button manualBtn = new Button(headerComp, SWT.PUSH);
		manualBtn.setImage(Images.CTXMENU_RDC);
		manualBtn.setText("Manual");
		manualBtn.setLayoutData(UIUtil.formData(null, -1, 0, 2, 100, -5, null, -1));
		manualBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					Display display = Display.getCurrent();
					if (display == null) {
						display = Display.getDefault();
					}
					
					calDialog = new CalendarDialog(display, CounterPastTimeGroupAllView.this);
					calDialog.showWithTime(UIUtil.getMousePosition(), stime);
					break;
				}
			}
		});
		
		eTimeText = new Label(headerComp, SWT.NONE);
		eTimeText.setLayoutData(UIUtil.labelFormData(manualBtn));
		
		Label label = new Label(headerComp, SWT.NONE);
		label.setLayoutData(UIUtil.labelFormData(eTimeText));
        label.setText("~");
		
		sTimeText = new Label(headerComp, SWT.NONE);
		sTimeText.setLayoutData(UIUtil.labelFormData(label));
		
        sDateText = new Label(headerComp, SWT.NONE);
		sDateText.setLayoutData(UIUtil.labelFormData(sTimeText));
		
		serverText = new Label(headerComp, SWT.NONE | SWT.RIGHT);
		serverText.setLayoutData(UIUtil.formData(0, 0, 0, 7, sDateText, -5, null, -1));
		
		setHeadText();
	}
	
	private void setHeadText() {
		serverText.setText(grpName + " |");
		sDateText.setText(DateUtil.format(stime, "yyyy-MM-dd"));
		sTimeText.setText(DateUtil.format(stime, "hh:mm a", Locale.ENGLISH));
		eTimeText.setText(DateUtil.format(etime, "hh:mm a", Locale.ENGLISH));
	}
	
	public void onPressedOk(long startTime, long endTime) {
		stime = startTime;
		etime = endTime;
		forceRefresh();
		setHeadText();
	}
	public void onPressedOk(String date) {}
	public void onPressedCancel() {}

	public void setFocus() {
		super.setFocus();
	}
	
	public void dispose() {
		ObjectSelectManager.getInstance().removeObjectCheckStateListener(this);
	}
	
	private double getMaxValue() {
		Iterator<Integer> objHashs = traces.keySet().iterator();
		double max = 0.0;
		while (objHashs.hasNext()) {
			int objHash = objHashs.next();
			CircularBufferDataProvider data = (CircularBufferDataProvider) traces.get(objHash).getDataProvider();
			if (data != null) {
				for (int inx = 0; inx < data.getSize(); inx++) {
					Sample sample = (Sample) data.getSample(inx);
					double y = sample.getYValue();
					if (y > max) {
						max = y;
					}
				}
			}
		}
		return ChartUtil.getMaxValue(max);
	}
	
	private IDataProvider getDataProvider(int objHash) {
		Trace trace = traces.get(objHash);
		if (trace == null) {
			CircularBufferDataProvider provider = new CircularBufferDataProvider(true);
			provider.setBufferSize((int) ((etime - stime) / 2000L) + 1);
			String name = StringUtil.trimToEmpty(TextProxy.object.getText(objHash));
			trace = new Trace(name, xyGraph.primaryXAxis, xyGraph.primaryYAxis, provider);
			trace.setPointStyle(PointStyle.NONE);
			trace.getXAxis().setFormatPattern("HH:mm:ss");
			trace.getYAxis().setFormatPattern("#,##0");
			trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace.setTraceType(TraceType.SOLID_LINE);
			trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			xyGraph.addTrace(trace);
			traces.put(objHash, trace);
		}
		return trace.getDataProvider();
	}
	
	private void forceRefresh() {
		for (Trace trace : traces.values()) {
			xyGraph.removeTrace(trace);
		}
		traces.clear();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				load();
			}
		});
	}
	
	private GroupManager manager = GroupManager.getInstance();
	
	private void collectObj() {
		serverObjMap.clear();
		Set<Integer> objHashs = manager.getObjectsByGroup(grpName);
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
	
	private void load() {
		collectObj();
		Iterator<Integer> serverIds = serverObjMap.keySet().iterator();
		final List<Pack> result = new ArrayList<Pack>();
		while (serverIds.hasNext()) {
			int serverId = serverIds.next();
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				param.put("stime", stime);
				param.put("etime", etime);
				param.put("counter", counter);
				param.put("objHash", serverObjMap.get(serverId));
				tcp.process(RequestCmd.COUNTER_PAST_TIME_GROUP, param, new INetReader() {
					public void process(DataInputX in) throws IOException {
						Pack p = in.readPack();
						result.add(p);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
		}
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				xyGraph.primaryXAxis.setRange(stime, etime);
				for (Pack pack : result) {
					if ((pack instanceof MapPack) == false) {
						continue;
					}
					MapPack m = (MapPack) pack;
					int objHash = m.getInt("objHash");
					ListValue time = m.getList("time");
					ListValue value = m.getList("value");
					if (time == null || time.size() < 1) {
						continue;
					}
					CircularBufferDataProvider provider = (CircularBufferDataProvider) getDataProvider(objHash);
					provider.clearTrace();
					for (int i = 0; time != null && i < time.size(); i++) {
						long x = time.getLong(i);
						double y = value.getDouble(i);
						provider.addSample(new Sample(x, y));
					}
				}
				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					double max = getMaxValue();
					xyGraph.primaryYAxis.setRange(0, max);
				}
			}
		});
	}
	
	boolean selectedMode = false;

	public void onSelectObject(int objHash, final String objName, String objType) {
		if (objType.equals(this.objType) == false) {
			return;
		}
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
				for (Trace t : traces.values()) {
					if (t.getName().equals(objName)) {
						t.setLineWidth(width + 2);
						selectedMode = true;
						break;
					}
				}
			}
		});
	}

	public void onDeselectObject() {
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
				for (Trace t : traces.values()) {
					t.setLineWidth(width);
				}
				selectedMode = false;
			}
		});
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
				for (Integer objHash : traces.keySet()) {
					if (ObjectSelectManager.getInstance().isUnselectedObject(objHash)) {
						traces.get(objHash).setVisible(false);
					} else {
						traces.get(objHash).setVisible(true);
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
