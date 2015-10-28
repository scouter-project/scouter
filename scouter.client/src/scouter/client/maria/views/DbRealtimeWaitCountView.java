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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.maria.actions.OpenDbLockListAction;
import scouter.client.model.AgentColorManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.value.NumberValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class DbRealtimeWaitCountView extends ViewPart implements Refreshable {
	
	public static final String ID = DbRealtimeWaitCountView.class.getName();
	
	int serverId;
	int objHash;
	
	RefreshThread thread;
	
	Trace trace;
	Trace pointTrace;
	
	static long TIME_RANGE = DateUtil.MILLIS_PER_FIVE_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 2);
	
	FigureCanvas canvas;
	XYGraph xyGraph;
	
	MapPack param = new MapPack();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
		objHash = CastUtil.cint(ids[1]);
	}

	public void createPartControl(Composite parent) {
		AgentObject ao = AgentModelThread.getInstance().getAgentObject(objHash);
		this.setPartName("WaitCount[" + ao.getObjName() + "]");
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
		canvas.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				double x = xyGraph.primaryXAxis.getPositionValue(e.x, false);
				((CircularBufferDataProvider)pointTrace.getDataProvider()).addSample(new Sample(x, xyGraph.primaryYAxis.getRange().getUpper()));
				new OpenDbLockListAction(serverId, objHash, (long)x).run();
			}
		});
		
		xyGraph = new XYGraph();
		xyGraph.setShowLegend(false);
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
		
		CircularBufferDataProvider avgProvider = new CircularBufferDataProvider(true);
		avgProvider.setBufferSize(((int)(TIME_RANGE / REFRESH_INTERVAL) + 1));
		avgProvider.setCurrentXDataArray(new double[] {});
		avgProvider.setCurrentYDataArray(new double[] {});
		trace = new Trace("WAIT_COUNT_TRACE", xyGraph.primaryXAxis, xyGraph.primaryYAxis, avgProvider);
		trace.setPointStyle(PointStyle.NONE);
		trace.setTraceType(TraceType.SOLID_LINE);
		trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		trace.setTraceColor(AgentColorManager.getInstance().assignColor(ao.getObjType(), objHash));
		xyGraph.addTrace(trace);
		
		CircularBufferDataProvider pointProvider = new CircularBufferDataProvider(true);
		pointProvider.setBufferSize(1);
		pointProvider.setCurrentXDataArray(new double[] {});
		pointProvider.setCurrentYDataArray(new double[] {});
		pointTrace = new Trace("POINT_TRACE", xyGraph.primaryXAxis, xyGraph.primaryYAxis, pointProvider);
		pointTrace.setPointStyle(PointStyle.NONE);
		pointTrace.setTraceType(TraceType.BAR);
		pointTrace.setLineWidth(1);
		pointTrace.setTraceColor(ColorUtil.getInstance().getColor("red"));
		xyGraph.addTrace(pointTrace);
		
		param.put("objHash", objHash);
		param.put("counter", CounterConstants.DB_WAIT_COUNT);
		param.put("timetype", TimeTypeEnum.REALTIME);
		
		this.setContentDescription("Double-click to see the detail list at that time.");
		
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
			v = tcp.getSingleValue(RequestCmd.COUNTER_REAL_TIME, param);
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
			final NumberValue nv = (NumberValue) v;
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.active);
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);
					((CircularBufferDataProvider)trace.getDataProvider()).addSample(new Sample(now, nv.doubleValue()));
					double max = ChartUtil.getMax(((CircularBufferDataProvider)trace.getDataProvider()).iterator());
					xyGraph.primaryYAxis.setRange(0, max);
				}
			});
		}
	}
}
