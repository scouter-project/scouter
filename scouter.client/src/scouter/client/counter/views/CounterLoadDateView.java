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
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.model.AgentColorManager;
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
import scouter.client.views.ScouterViewPart;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class CounterLoadDateView extends ScouterViewPart {
	public static final String ID = CounterLoadDateView.class.getName();

	protected String date;
	protected int objHash;
	protected String objName;
	protected String objType;
	protected String counter;
	protected int serverId;
	
	IWorkbenchWindow window;
	Action act;

	public void setInput(String date, int objHash, String objName, String objType, String counter, int serverId) throws Exception{
		this.date = date;
		this.objHash = objHash;
		this.objName = objName;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;
		
		String date2 = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+ " | (Past) [" + objName + "][" + date2 + "] " + counterDisplay+(!"".equals(counterUnit)?" ("+counterUnit+")":"");
		setViewTab(objType, counter, serverId);
		
		
		if (this.trace != null) {
			this.trace.setName(objName);
			this.trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			ChartUtil.addSolidLine(xyGraph, provider, AgentColorManager.getInstance().assignColor(objType, objHash));
		}
		
		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objHash, objType, counter);
		
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				load();
			}
		});
	}

	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}

	public void load() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		MapPack out = null;
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			param.put("objHash", objHash);
			param.put("objType", objType);
			param.put("counter", counter);

			out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_PAST_DATE, param);
			
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

				provider.clearTrace();
				for (int i = 0; time != null && i < time.size(); i++) {
					long tm = CastUtil.clong((time.get(i)));
					double va = CastUtil.clong(value.get(i));
					provider.addSample(new Sample(tm, va));
				}
				try {
					long stime = DateUtil.getTime(date, "yyyyMMdd");
					long etime = stime + DateUtil.MILLIS_PER_DAY;
					xyGraph.primaryXAxis.setRange(stime, etime);
				} catch (Exception e) {
					ConsoleProxy.error(e.toString());
				}
				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					xyGraph.primaryYAxis.setRange(0, ChartUtil.getMax(provider.iterator()));
				}
				canvas.redraw();
				xyGraph.repaint();

			}
		});

	}

	protected CircularBufferDataProvider provider;
	protected XYGraph xyGraph;
	protected Trace trace;
	protected FigureCanvas canvas;

	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		parent.setLayout(layout);
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		
		canvas = new FigureCanvas(parent);
		canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterLoadDateView.this.setContentDescription(desc);
					} else {
						CounterLoadDateView.this.setContentDescription("");
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

		provider = new CircularBufferDataProvider(true);
		provider.setBufferSize(288);
		provider.setCurrentXDataArray(new double[] {});
		provider.setCurrentYDataArray(new double[] {});

		// create the trace
		trace = new Trace("TOTAL", xyGraph.primaryXAxis, xyGraph.primaryYAxis, provider);

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
	}
	
	public void setFocus() {
		statusMessage = desc+" - setInput(String date:"+date+", int objHash:"+objHash+", String objName:"+objName+", String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
		super.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
	}
}