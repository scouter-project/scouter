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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
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
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class DbRealtimeTotalHitRatioView extends ViewPart implements Refreshable {
	
	public static final String ID = DbRealtimeTotalHitRatioView.class.getName();
	
	int serverId;
	
	RefreshThread thread;
	
	static long TIME_RANGE = DateUtil.MILLIS_PER_FIVE_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 2);
	
	FigureCanvas canvas;
	XYGraph xyGraph;
	
	Trace innoBufTrace;
	Trace keyCacheTrace;
	Trace queryCacheTrace;
	Trace threadCacheTrace;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		serverId = CastUtil.cint(secId);
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		this.setPartName("Hit Ratio[" + server.getName() + "]");
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
		
		CircularBufferDataProvider innoDbProvider = new CircularBufferDataProvider(true);
		innoDbProvider.setBufferSize(((int)(TIME_RANGE / REFRESH_INTERVAL) + 1));
		innoDbProvider.setCurrentXDataArray(new double[] {});
		innoDbProvider.setCurrentYDataArray(new double[] {});
		innoBufTrace = new Trace("InnoDB Buffer (AVG)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, innoDbProvider);
		innoBufTrace.setPointStyle(PointStyle.NONE);
		innoBufTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		innoBufTrace.setTraceType(TraceType.SOLID_LINE);
		innoBufTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_BLUE));
		xyGraph.addTrace(innoBufTrace);
		
		CircularBufferDataProvider keyProvider = new CircularBufferDataProvider(true);
		keyProvider.setBufferSize(((int)(TIME_RANGE / REFRESH_INTERVAL) + 1));
		keyProvider.setCurrentXDataArray(new double[] {});
		keyProvider.setCurrentYDataArray(new double[] {});
		keyCacheTrace = new Trace("Key Cache (AVG)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, keyProvider);
		keyCacheTrace.setPointStyle(PointStyle.NONE);
		keyCacheTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		keyCacheTrace.setTraceType(TraceType.SOLID_LINE);
		keyCacheTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_CYAN));
		xyGraph.addTrace(keyCacheTrace);
		
		CircularBufferDataProvider queryProvider = new CircularBufferDataProvider(true);
		queryProvider.setBufferSize(((int)(TIME_RANGE / REFRESH_INTERVAL) + 1));
		queryProvider.setCurrentXDataArray(new double[] {});
		queryProvider.setCurrentYDataArray(new double[] {});
		queryCacheTrace = new Trace("Query Cache (AVG)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, queryProvider);
		queryCacheTrace.setPointStyle(PointStyle.NONE);
		queryCacheTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		queryCacheTrace.setTraceType(TraceType.SOLID_LINE);
		queryCacheTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GRAY));
		xyGraph.addTrace(queryCacheTrace);
		
		CircularBufferDataProvider threadProvider = new CircularBufferDataProvider(true);
		threadProvider.setBufferSize(((int)(TIME_RANGE / REFRESH_INTERVAL) + 1));
		threadProvider.setCurrentXDataArray(new double[] {});
		threadProvider.setCurrentYDataArray(new double[] {});
		threadCacheTrace = new Trace("Thread Cache (AVG)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, threadProvider);
		threadCacheTrace.setPointStyle(PointStyle.NONE);
		threadCacheTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		threadCacheTrace.setTraceType(TraceType.SOLID_LINE);
		threadCacheTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GREEN));
		xyGraph.addTrace(threadCacheTrace);
		
		ScouterUtil.addHorizontalRangeListener(xyGraph.getPlotArea(), new OpenDigestTableAction(serverId), false);
		
		
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
		Pack p = null;
		try {
			MapPack param = new MapPack();
			ListValue objHashLv = AgentModelThread.getInstance().getLiveObjHashLV(serverId, CounterConstants.MARIA_PLUGIN);
			if (objHashLv.size() > 0) {
				param.put("objHash", objHashLv);
				p = tcp.getSingle(RequestCmd.DB_REALTIME_HIT_RATIO, param);
			}
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (p == null) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.inactive);
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);
					xyGraph.primaryYAxis.setRange(0, 100);
				}
			});
		} else {
			final MapPack m = (MapPack) p;
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);
					xyGraph.primaryYAxis.setRange(0, 100);
					if (m.size() == 0) {
						setTitleImage(Images.inactive);
						return;
					}
					setTitleImage(Images.active);
					float innodb_buf = m.getFloat("innodb_buffer");
					float key_cache = m.getFloat("key_cache");
					float query_cache = m.getFloat("query_cache");
					float thread_cache = m.getFloat("thread_cache");
					((CircularBufferDataProvider)innoBufTrace.getDataProvider()).addSample(new Sample(now, innodb_buf));
					((CircularBufferDataProvider)keyCacheTrace.getDataProvider()).addSample(new Sample(now, key_cache));
					((CircularBufferDataProvider)queryCacheTrace.getDataProvider()).addSample(new Sample(now, query_cache));
					((CircularBufferDataProvider)threadCacheTrace.getDataProvider()).addSample(new Sample(now, thread_cache));
				}
			});
		}
	}
}
