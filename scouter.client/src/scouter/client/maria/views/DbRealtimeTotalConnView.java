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
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class DbRealtimeTotalConnView extends ViewPart implements Refreshable {
	
	public static final String ID = DbRealtimeTotalConnView.class.getName();
	
	int serverId;
	
	RefreshThread thread;
	
	static long TIME_RANGE = DateUtil.MILLIS_PER_FIVE_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 2);
	
	FigureCanvas canvas;
	XYGraph xyGraph;
	
	Trace totalTrace;
	Trace activeTrace;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		serverId = CastUtil.cint(secId);
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		this.setPartName("Connections[" + server.getName() + "]");
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
		
		CircularBufferDataProvider totalProvider = new CircularBufferDataProvider(true);
		totalProvider.setBufferSize(((int)(TIME_RANGE / REFRESH_INTERVAL) + 1));
		totalProvider.setCurrentXDataArray(new double[] {});
		totalProvider.setCurrentYDataArray(new double[] {});
		
		totalTrace = new Trace("Total (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, totalProvider);
		totalTrace.setPointStyle(PointStyle.NONE);
		totalTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		totalTrace.setTraceType(TraceType.SOLID_LINE);
		totalTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_RED));
		xyGraph.addTrace(totalTrace);
		
		CircularBufferDataProvider activeProvider = new CircularBufferDataProvider(true);
		activeProvider.setBufferSize(((int)(TIME_RANGE / REFRESH_INTERVAL) + 1));
		activeProvider.setCurrentXDataArray(new double[] {});
		activeProvider.setCurrentYDataArray(new double[] {});
		activeTrace = new Trace("Running (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, activeProvider);
		activeTrace.setPointStyle(PointStyle.NONE);
		activeTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		activeTrace.setTraceType(TraceType.SOLID_LINE);
		activeTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GREEN));
		xyGraph.addTrace(activeTrace);
		
		ScouterUtil.addHorizontalRangeListener(xyGraph.getPlotArea(), new OpenDigestTableAction(serverId), false);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		Action openDailyView = new Action("Open Daily View", ImageUtil.getImageDescriptor(Images.calendar)) {
			public void run() {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DbTodayTotalConnView.ID, ""+serverId, IWorkbenchPage.VIEW_ACTIVATE);
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
				v = tcp.getSingleValue(RequestCmd.DB_REALTIME_CONNECTIONS, param);
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
			MapValue value = (MapValue) v;
			final long total = value.getLong("total");
			final long active = value.getLong("active");
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.active);
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);
					((CircularBufferDataProvider)totalTrace.getDataProvider()).addSample(new Sample(now, total));
					((CircularBufferDataProvider)activeTrace.getDataProvider()).addSample(new Sample(now, active));
					double max = ChartUtil.getMax(((CircularBufferDataProvider)totalTrace.getDataProvider()).iterator());
					xyGraph.primaryYAxis.setRange(0, max);
				}
			});
		}
	}
}
