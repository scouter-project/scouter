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
import java.util.Map;
import java.util.Set;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.model.RefreshThread;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.views.ScouterViewPart;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class CounterTodayGroupTotalView extends ScouterViewPart implements RefreshThread.Refreshable {

	public static final String ID = CounterTodayGroupTotalView.class.getName();
			
	protected RefreshThread thread;
	private String grpName;
	private String objType;
	private String counter;
	private String mode;
	private Server defaultServer = ServerManager.getInstance().getDefaultServer();
	protected XYGraph xyGraph;
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	protected Trace trace;
	private Map<Integer, ListValue> serverObjMap = new HashMap<Integer, ListValue>();
	protected FigureCanvas canvas;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		statusMessage = secId;
		String[] datas = secId.split("&");
		grpName = datas[0];
		objType = datas[1];
		counter = datas[2];
	}

	public void createPartControl(Composite parent) {
		String displayCounter = defaultServer.getCounterEngine().getCounterDisplayName(objType, counter);
		setPartName(grpName + " - " + displayCounter);
		String unit = defaultServer.getCounterEngine().getCounterUnit(objType, counter);
		mode = CounterUtil.getTotalMode(objType, counter);
		desc = grpName + " | (Today) Total " + displayCounter + ((StringUtil.isNotEmpty(unit) ? "(" + unit + ") " : " ") + mode);
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
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterTodayGroupTotalView.this.setContentDescription(desc);
					} else {
						CounterTodayGroupTotalView.this.setContentDescription("");
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
		xyGraph = new XYGraph();
		xyGraph.setShowTitle(false);
		xyGraph.setShowLegend(false);
		canvas.setContents(xyGraph);
		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);
		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		CircularBufferDataProvider provider = new CircularBufferDataProvider(true);
		provider.setBufferSize(288);
		trace = new Trace(grpName + "_TOTAL" , xyGraph.primaryXAxis, xyGraph.primaryYAxis, provider);
		trace.setPointStyle(PointStyle.NONE);
		trace.getXAxis().setFormatPattern("HH:mm:ss");
		trace.getYAxis().setFormatPattern("#,##0");
		trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		trace.setTraceType(TraceType.AREA);
		trace.setTraceColor(ColorUtil.getInstance().TOTAL_CHART_COLOR);
		xyGraph.addTrace(trace);
		ChartUtil.addSolidLine(xyGraph, provider, ColorUtil.getInstance().TOTAL_CHART_COLOR);
		ScouterUtil.addShowTotalValueListener(canvas, xyGraph);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				forceRefresh();
			}
		});
		thread = new RefreshThread(this, 10000);
		thread.start();
	}

	public void setFocus() {
		super.setFocus();
	}
	
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}
	
	boolean isActive = false;
	
	private void forceRefresh() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}
	
	public void refresh() {
		ScouterUtil.collectGroupObjcts(grpName, serverObjMap);
		isActive = false;
		Iterator<Integer> serverIds = serverObjMap.keySet().iterator();
		final List<Pack> result = new ArrayList<Pack>();
		while (serverIds.hasNext()) {
			int serverId = serverIds.next();
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				param.put("counter", counter);
				param.put("objHash", serverObjMap.get(serverId));
				tcp.process(RequestCmd.COUNTER_TODAY_GROUP, param, new INetReader() {
					public void process(DataInputX in) throws IOException {
						Pack p = in.readPack();
						if (p != null) {
							result.add(p);
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
		}
		if (result.size() > 0) {
			isActive = true;
		}
		final Map<Long, Double> valueMap = ScouterUtil.getLoadTotalMap(counter, result, mode, TimeTypeEnum.FIVE_MIN);
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				if (isActive) {
					setActive();
				} else {
					setInactive();
				}
				CircularBufferDataProvider provider = (CircularBufferDataProvider) trace.getDataProvider();
				provider.clearTrace();
				long stime = DateUtil.getTime(DateUtil.yyyymmdd(TimeUtil.getCurrentTime()), "yyyyMMdd");
				xyGraph.primaryXAxis.setRange(stime, stime + DateUtil.MILLIS_PER_DAY);
				Set<Long> timeSet = valueMap.keySet();
				for (long time : timeSet) {
					provider.addSample(new Sample(time, CastUtil.cdouble(valueMap.get(time))));
				}
				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					double max = ChartUtil.getMax(provider.iterator());
						xyGraph.primaryYAxis.setRange(0, max);
				}
			}
		});
	}
}
