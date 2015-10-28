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
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.model.AgentColorManager;
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
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.MenuUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.client.views.ScouterViewPart;
import scouter.lang.CounterKey;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class CounterRealDateView extends ScouterViewPart implements Refreshable {
	public static final String ID = CounterRealDateView.class.getName();

	protected String objName;
	protected int objHash;
	protected String counter;
	protected int serverId;

	protected RefreshThread thread;

	private IMemento memento;

	IWorkbenchWindow window;
	protected String objType;
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	public void setInput(int objHash, String objName, String objType, String counter, int serverId) throws Exception{
		this.objHash = objHash;
		this.objName = objName;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+" | (Today) [" + objName + "] " + counterDisplay + (!"".equals(counterUnit)?" ("+counterUnit+")":"");
		setViewTab(objType, counter, serverId);
		
		if (this.trace != null) {
			this.trace.setName(objName);
			this.trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			ChartUtil.addSolidLine(xyGraph, traceDataProvider, AgentColorManager.getInstance().assignColor(objType, objHash));
		}
		
		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objHash, objType, counter);

		if (thread == null) {
			thread = new RefreshThread(this, 10000);
			thread.start();
		}
		
		thread.setName(this.toString() + " - " 
				+ "objHash:"+objHash
				+ ", objName:"+objName
				+ ", objType:"+objType
				+ ", counter:"+counter
				+ ", serverId:"+serverId);
	}
	
	boolean isActive = false;
	public void refresh() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		MapPack out = null;
		isActive = false;
		try {
			MapPack param = CounterKey.toMapPacket(new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN));
			out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_TODAY, param);
			isActive = true;
			if (out == null) {
				return;
			}
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		final ListValue time = out.getList("time");
		final ListValue value = out.getList("value");

		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				
				if(isActive){
					setActive();
				}else{
					setInactive();
				}

				traceDataProvider.clearTrace();
				for (int i = 0; time != null && i < time.size(); i++) {
					long x = CastUtil.clong(time.get(i));
					double y = CastUtil.cdouble(value.get(i));
					traceDataProvider.addSample(new Sample(x, y));
				}
				// 시간 간격과 최대값 조정
				String date = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
				long stime = DateUtil.getTime(date, "yyyyMMdd");
				long etime = stime + DateUtil.MILLIS_PER_DAY;
				xyGraph.primaryXAxis.setRange(stime, etime);
				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					xyGraph.primaryYAxis.setRange(0, ChartUtil.getMax(traceDataProvider.iterator()));
				}
				canvas.redraw();
				xyGraph.repaint();

			}
		});

	}

	protected CircularBufferDataProvider traceDataProvider;
	protected XYGraph xyGraph;
	protected Trace trace;
	protected FigureCanvas canvas;

	public void createPartControl(Composite parent) {
		parent.setLayout(UIUtil.formLayout(0, 0));
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		
		canvas = new FigureCanvas(parent);
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));

		canvas.setLayoutData(UIUtil.formData(0, 0, 0, 0, 100, 0, 100, 0));
		
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterRealDateView.this.setContentDescription(desc);
					} else {
						CounterRealDateView.this.setContentDescription("");
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
		xyGraph.setShowLegend(false);
		xyGraph.setShowTitle(false);

		canvas.setContents(xyGraph);

		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);

		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);

		traceDataProvider = new CircularBufferDataProvider(true);
		traceDataProvider.setBufferSize(288);
		traceDataProvider.setCurrentXDataArray(new double[] {});
		traceDataProvider.setCurrentYDataArray(new double[] {});

		// create the trace
		trace = new Trace("TOTAL", xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);

		// set trace property
		trace.setPointStyle(PointStyle.NONE);
		trace.getXAxis().setFormatPattern("HH:mm");
		trace.getYAxis().setFormatPattern("#,##0");

		trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		trace.setTraceType(TraceType.AREA);
		trace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_CYAN));

		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");

		xyGraph.addTrace(trace);
		ChartUtil.addSolidLine(xyGraph, traceDataProvider, ColorUtil.getInstance().getColor(SWT.COLOR_DARK_CYAN));

		restoreState();
	}

	public void setFocus() {
		statusMessage = desc + " - setInput(int objHash:"+objHash+", String objName:"+objName+", String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
		super.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}

	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento = memento.createChild(ID);
		memento.putInteger("objHash", objHash);
		memento.putString("objName", objName);
		memento.putString("objType", objType);
		memento.putString("counter", counter);
		memento.putInteger("serverId", serverId);
	}

	private void restoreState() {
		if (memento == null)
			return;
		IMemento m = memento.getChild(ID);
		if(m == null)
			return;

		int objHash = CastUtil.cint(m.getInteger("objHash"));
		String objName = m.getString("objName");
		String objType = m.getString("objType");
		String counter = m.getString("counter");
		int serverId = CastUtil.cint(m.getInteger("serverId"));
		try {
			setInput(objHash, objName, objType, counter, serverId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}