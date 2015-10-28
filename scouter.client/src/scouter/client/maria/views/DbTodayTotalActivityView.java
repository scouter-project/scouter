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
package scouter.client.maria.views;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.maria.actions.OpenDigestTableAction;
import scouter.client.model.AgentDailyListProxy;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class DbTodayTotalActivityView extends ViewPart implements Refreshable {
	
	public static final String ID = DbTodayTotalActivityView.class.getName();
	
	int serverId;
	
	RefreshThread thread;
	
	static long TIME_RANGE = DateUtil.MILLIS_PER_DAY;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 10);
	static int BUFFER_SIZE = (int)(TIME_RANGE / DateUtil.MILLIS_PER_FIVE_MINUTE);
	
	FigureCanvas canvas;
	XYGraph xyGraph;
	
	Trace callTrace;
	Trace selectTrace;
	Trace insertTrace;
	Trace updateTrace;
	Trace deleteTrace;
	
	boolean fixRange = false;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		serverId = CastUtil.cint(secId);
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		this.setPartName("DB Activity[" + server.getName() + "]");
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
			public void controlMoved(ControlEvent arg0) {
			}
			
			public void controlResized(ControlEvent arg0) {
				Rectangle r = canvas.getClientArea();
				xyGraph.setSize(r.width, r.height);
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
		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
		xyGraph.primaryYAxis.setFormatPattern("#,##0");
		
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		CircularBufferDataProvider callProvider = new CircularBufferDataProvider(true);
		callProvider.setBufferSize(BUFFER_SIZE);
		callProvider.setCurrentXDataArray(new double[] {});
		callProvider.setCurrentYDataArray(new double[] {});
		callTrace = new Trace("Call (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, callProvider);
		callTrace.setPointStyle(PointStyle.NONE);
		callTrace.setTraceType(TraceType.AREA);
		callTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		callTrace.setAreaAlpha(255);
		callTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_BLUE));
		xyGraph.addTrace(callTrace);
		
		CircularBufferDataProvider selectProvider = new CircularBufferDataProvider(true);
		selectProvider.setBufferSize(BUFFER_SIZE);
		selectProvider.setCurrentXDataArray(new double[] {});
		selectProvider.setCurrentYDataArray(new double[] {});
		selectTrace = new Trace("Select (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, selectProvider);
		selectTrace.setPointStyle(PointStyle.NONE);
		selectTrace.setTraceType(TraceType.AREA);
		selectTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		selectTrace.setAreaAlpha(255);
		selectTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_CYAN));
		xyGraph.addTrace(selectTrace);
		
		CircularBufferDataProvider insertProvider = new CircularBufferDataProvider(true);
		insertProvider.setBufferSize(BUFFER_SIZE);
		insertProvider.setCurrentXDataArray(new double[] {});
		insertProvider.setCurrentYDataArray(new double[] {});
		insertTrace = new Trace("Insert (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, insertProvider);
		insertTrace.setPointStyle(PointStyle.NONE);
		insertTrace.setTraceType(TraceType.AREA);
		insertTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		insertTrace.setAreaAlpha(255);
		insertTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GRAY));
		xyGraph.addTrace(insertTrace);
		
		CircularBufferDataProvider updateProvider = new CircularBufferDataProvider(true);
		updateProvider.setBufferSize(BUFFER_SIZE);
		updateProvider.setCurrentXDataArray(new double[] {});
		updateProvider.setCurrentYDataArray(new double[] {});
		updateTrace = new Trace("Update (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, updateProvider);
		updateTrace.setPointStyle(PointStyle.NONE);
		updateTrace.setTraceType(TraceType.AREA);
		updateTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		updateTrace.setAreaAlpha(255);
		updateTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GREEN));
		xyGraph.addTrace(updateTrace);
		
		CircularBufferDataProvider deleteProvider = new CircularBufferDataProvider(true);
		deleteProvider.setBufferSize(BUFFER_SIZE);
		deleteProvider.setCurrentXDataArray(new double[] {});
		deleteProvider.setCurrentYDataArray(new double[] {});
		deleteTrace = new Trace("Delete (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, deleteProvider);
		deleteTrace.setPointStyle(PointStyle.NONE);
		deleteTrace.setTraceType(TraceType.AREA);
		deleteTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		deleteTrace.setAreaAlpha(255);
		deleteTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_MAGENTA));
		xyGraph.addTrace(deleteTrace);
		
		ScouterUtil.addHorizontalRangeListener(xyGraph.getPlotArea(), new OpenDigestTableAction(serverId), true);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		Action fixRangeAct = new Action("Pin Range", IAction.AS_CHECK_BOX) {
			public void run() {
				fixRange = isChecked();
			}
		};
		fixRangeAct.setImageDescriptor(ImageUtil.getImageDescriptor(Images.pin));
		man.add(fixRangeAct);
		
		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
	}

	public void setFocus() {
		
	}
	
	AgentDailyListProxy agentDailyProxy = new AgentDailyListProxy();

	public void refresh() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;
		final String date = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
		try {
			MapPack param = new MapPack();
			param.put("objHash", agentDailyProxy.getObjHashLv(date, serverId, CounterConstants.MARIA_PLUGIN));
			param.put("sdate", date);
			param.put("edate", date);
			p = tcp.getSingle(RequestCmd.DB_DAILY_ACTIVITY, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (p == null) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.inactive);
					if (fixRange == false) {
						DbTodayTotalActivityView.this.setContentDescription(date.substring(0, 4) + "-" + date.substring(5, 6) + "-" + date.substring(7, 8));
						long stime = DateUtil.yyyymmdd(date);
						long etime = stime + DateUtil.MILLIS_PER_DAY - 1;
						xyGraph.primaryXAxis.setRange(stime, etime);
					}
				}
			});
		} else {
			MapPack m = (MapPack) p;
			final ListValue timeLv = m.getList("time");
			final ListValue callLv = m.getList("call");
			final ListValue selectLv = m.getList("select");
			final ListValue insertLv = m.getList("insert");
			final ListValue updateLv = m.getList("update");
			final ListValue deleteLv = m.getList("delete");
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.active);
					CircularBufferDataProvider callProvider = (CircularBufferDataProvider)callTrace.getDataProvider();
					CircularBufferDataProvider selectProvider = (CircularBufferDataProvider)selectTrace.getDataProvider();
					CircularBufferDataProvider insertProvider = (CircularBufferDataProvider)insertTrace.getDataProvider();
					CircularBufferDataProvider updateProvider = (CircularBufferDataProvider)updateTrace.getDataProvider();
					CircularBufferDataProvider deleteProvider = (CircularBufferDataProvider)deleteTrace.getDataProvider();
					callProvider.clearTrace();
					selectProvider.clearTrace();
					insertProvider.clearTrace();
					updateProvider.clearTrace();
					deleteProvider.clearTrace();
					for (int i = 0; i < timeLv.size(); i++) {
						long time = timeLv.getLong(i);
						double value = deleteLv.getDouble(i);
						deleteProvider.addSample(new Sample(time, value));
						value += updateLv.getDouble(i);
						updateProvider.addSample(new Sample(time, value));
						value += insertLv.getDouble(i);
						insertProvider.addSample(new Sample(time, value));
						value += selectLv.getDouble(i);
						selectProvider.addSample(new Sample(time, value));
						value += callLv.getDouble(i);
						callProvider.addSample(new Sample(time, value));
					}
					if (fixRange == false) {
						DbTodayTotalActivityView.this.setContentDescription(date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8));
						long stime = DateUtil.yyyymmdd(date);
						long etime = stime + DateUtil.MILLIS_PER_DAY - 1;
						xyGraph.primaryXAxis.setRange(stime, etime);
						double max = ChartUtil.getMax(callProvider.iterator());
						xyGraph.primaryYAxis.setRange(0, max);
					}
				}
			});
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}
}
