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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.maria.actions.OpenDigestTableAction;
import scouter.client.model.AgentModelThread;
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
import scouter.lang.value.DecimalValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.LinkedList;
import scouter.util.LinkedList.ENTRY;

public class DbRealtimeTotalActivityView extends ViewPart implements Refreshable {

public static final String ID = DbRealtimeTotalActivityView.class.getName();
	
	int serverId;
	
	RefreshThread thread;
	
	static long TIME_RANGE = DateUtil.MILLIS_PER_FIVE_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 2);
	static int BUFFER_SIZE = (int)(TIME_RANGE / REFRESH_INTERVAL) + 1;
	
	FigureCanvas canvas;
	XYGraph xyGraph;
	
	Trace callTrace;
	Trace selectTrace;
	Trace insertTrace;
	Trace updateTrace;
	Trace deleteTrace;
	
	boolean isStackView = true;
	
	LinkedList<ValueLog> valueLogs = new LinkedList<ValueLog>();
	
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
		
		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));
		
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
		
		ScouterUtil.addHorizontalRangeListener(xyGraph.getPlotArea(), new OpenDigestTableAction(serverId), false);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		Action stackViewAct = new Action("Area Mode", IAction.AS_CHECK_BOX) {
			public void run() {
				isStackView = isChecked();
				changeMode();
			}
		};
		stackViewAct.setImageDescriptor(ImageUtil.getImageDescriptor(Images.sum));
		stackViewAct.setChecked(true);
		man.add(stackViewAct);
		Action openDailyView = new Action("Open Daily View", ImageUtil.getImageDescriptor(Images.calendar)) {
			public void run() {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DbTodayTotalActivityView.ID, ""+serverId, IWorkbenchPage.VIEW_ACTIVATE);
				} catch (PartInitException e) {
					ConsoleProxy.errorSafe(e.toString());
				}
			}
		};
		man.add(openDailyView);
		
		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
	}

	public void setFocus() {
		
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}

	public void refresh() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Value v = null;
		try {
			MapPack param = new MapPack();
			ListValue objHashLv = AgentModelThread.getInstance().getLiveObjHashLV(serverId, CounterConstants.MARIA_PLUGIN);
			if (objHashLv.size() > 0) {
				param.put("objHash", objHashLv);
				v = tcp.getSingleValue(RequestCmd.DB_REALTIME_ACTIVITY, param);
			}
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (v == null) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.inactive);
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);
				}
			});
		} else {
			MapValue mv = (MapValue) v;
			final DecimalValue callValue = new DecimalValue(mv.getLong("call"));
			final DecimalValue selectValue = new DecimalValue(mv.getLong("select"));
			final DecimalValue insertValue = new DecimalValue(mv.getLong("insert"));
			final DecimalValue updateValue = new DecimalValue(mv.getLong("update"));
			final DecimalValue deleteValue = new DecimalValue(mv.getLong("delete"));
			
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.active);
					long now = TimeUtil.getCurrentTime(serverId) / REFRESH_INTERVAL * REFRESH_INTERVAL;
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);
					ValueLog valueLog = new ValueLog();
					valueLog.time = now;
					valueLog.delete = deleteValue.value;
					valueLog.update = updateValue.value;
					valueLog.insert = insertValue.value;
					valueLog.select = selectValue.value;
					valueLog.call = callValue.value;
					valueLogs.add(valueLog);
					if (valueLogs.size() > BUFFER_SIZE) {
						valueLogs.removeFirst();
					}
					if (isStackView) {
						updateValue.value += deleteValue.value;
						insertValue.value += updateValue.value;
						selectValue.value += insertValue.value;
						callValue.value += selectValue.value;
					}
					((CircularBufferDataProvider)callTrace.getDataProvider()).addSample(new Sample(now, callValue.value));
					((CircularBufferDataProvider)selectTrace.getDataProvider()).addSample(new Sample(now, selectValue.value));
					((CircularBufferDataProvider)insertTrace.getDataProvider()).addSample(new Sample(now, insertValue.value));
					((CircularBufferDataProvider)updateTrace.getDataProvider()).addSample(new Sample(now, updateValue.value));
					((CircularBufferDataProvider)deleteTrace.getDataProvider()).addSample(new Sample(now, deleteValue.value));
					xyGraph.primaryYAxis.setRange(0, getMaxYValue());
				}
				
				private double getMaxYValue() {
					if (isStackView) {
						return ChartUtil.getMax(((CircularBufferDataProvider)callTrace.getDataProvider()).iterator());
					}
					double value = ChartUtil.getMax(((CircularBufferDataProvider)callTrace.getDataProvider()).iterator());
					value = Math.max(value, ChartUtil.getMax(((CircularBufferDataProvider)selectTrace.getDataProvider()).iterator()));
					value = Math.max(value, ChartUtil.getMax(((CircularBufferDataProvider)insertTrace.getDataProvider()).iterator()));
					value = Math.max(value, ChartUtil.getMax(((CircularBufferDataProvider)updateTrace.getDataProvider()).iterator()));
					value = Math.max(value, ChartUtil.getMax(((CircularBufferDataProvider)deleteTrace.getDataProvider()).iterator()));
					return value;
				}
			});
		}
	}
	
	protected void changeMode() {
		CircularBufferDataProvider delProvider = (CircularBufferDataProvider)deleteTrace.getDataProvider();
		CircularBufferDataProvider upProvider = (CircularBufferDataProvider)updateTrace.getDataProvider();
		CircularBufferDataProvider inProvider = (CircularBufferDataProvider)insertTrace.getDataProvider();
		CircularBufferDataProvider selProvider = (CircularBufferDataProvider)selectTrace.getDataProvider();
		CircularBufferDataProvider callProvider = (CircularBufferDataProvider)callTrace.getDataProvider();
		delProvider.clearTrace();
		upProvider.clearTrace();
		inProvider.clearTrace();
		selProvider.clearTrace();
		callProvider.clearTrace();
		int size = valueLogs.size();
		if (size > 0) {
			ENTRY<ValueLog> entry = valueLogs.getFirst();
			if (isStackView) {
				do {
					ValueLog log = entry.item;
					double x = log.time;
					double delValue = log.delete;
					double upValue = delValue + log.update;
					double inValue = upValue + log.insert;
					double selValue = inValue + log.select;
					double callValue = selValue + log.call;
					callProvider.addSample(new Sample(x, callValue));
					selProvider.addSample(new Sample(x, selValue));
					inProvider.addSample(new Sample(x, inValue));
					upProvider.addSample(new Sample(x, upValue));
					delProvider.addSample(new Sample(x, delValue));
				} while ((entry = entry.next) != null);
			} else {
				do {
					ValueLog log = entry.item;
					double x = log.time;
					double delValue = log.delete;
					double upValue = log.update;
					double inValue = log.insert;
					double selValue = log.select;
					double callValue = log.call;
					callProvider.addSample(new Sample(x, callValue));
					selProvider.addSample(new Sample(x, selValue));
					inProvider.addSample(new Sample(x, inValue));
					upProvider.addSample(new Sample(x, upValue));
					delProvider.addSample(new Sample(x, delValue));
				} while ((entry = entry.next) != null);
			}
		}
		if (isStackView) {
			deleteTrace.setTraceType(TraceType.AREA);
			updateTrace.setTraceType(TraceType.AREA);
			insertTrace.setTraceType(TraceType.AREA);
			selectTrace.setTraceType(TraceType.AREA);
			callTrace.setTraceType(TraceType.AREA);
		} else {
			deleteTrace.setTraceType(TraceType.SOLID_LINE);
			updateTrace.setTraceType(TraceType.SOLID_LINE);
			insertTrace.setTraceType(TraceType.SOLID_LINE);
			selectTrace.setTraceType(TraceType.SOLID_LINE);
			callTrace.setTraceType(TraceType.SOLID_LINE);
		}
	}
	
	private static class ValueLog {
		long time;
		double delete;
		double update;
		double insert;
		double select;
		double call;
	}
}
