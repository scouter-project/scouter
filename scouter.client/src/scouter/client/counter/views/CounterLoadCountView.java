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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import scouter.client.model.AgentColorManager;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.views.ScouterViewPart;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class CounterLoadCountView extends ScouterViewPart {
	public static final String ID = CounterLoadCountView.class.getName();
	
	protected String objType;
	protected String counter;
	protected String date;
	protected int objHash;
	protected int serverId;
	
	public void setInput(String date, String objType, String counter, int objHash, int serverId) throws Exception {
		this.date = date;
		this.objHash = objHash;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;

		String objName = StringUtil.trimToEmpty(TextProxy.object.getLoadText(date, objHash, serverId));
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+ " | (Past) [" + objName + "][" + date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + "] Past-date " + counterDisplay+(!"".equals(counterUnit)?" ("+counterUnit+")":"");
		this.trace.setName(objName);
		this.trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
		setViewTab(objType, counter, serverId);
		new LoadJob("Load_" + counter).schedule();
	}
	
	protected CircularBufferDataProvider traceDataProvider;
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
						CounterLoadCountView.this.setContentDescription(desc);
					} else {
						CounterLoadCountView.this.setContentDescription("");
					}
					r = canvas.getClientArea();
					lock = false;
				}
				if(xyGraph == null)
					return;
				xyGraph.setSize(r.width, r.height);
				trace.setLineWidth(r.width / 30);
			}

			public void controlMoved(ControlEvent e) {
			}
		});
		
		xyGraph = new XYGraph();
		xyGraph.setShowLegend(false);

		canvas.setContents(xyGraph);

		xyGraph.primaryXAxis.setDateEnabled(false);
		xyGraph.primaryXAxis.setShowMajorGrid(false);

		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		traceDataProvider = new CircularBufferDataProvider(true);
		traceDataProvider.setBufferSize(24);
		traceDataProvider.setCurrentXDataArray(new double[] {});
		traceDataProvider.setCurrentYDataArray(new double[] {});
		
		this.xyGraph.primaryXAxis.setRange(0, 24);

		trace = new Trace("temp", xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);
		trace.setPointStyle(PointStyle.NONE);
		trace.getYAxis().setFormatPattern("#,##0");

		trace.setLineWidth(15);
		trace.setTraceType(TraceType.BAR);
		trace.setAreaAlpha(200);

		// add the trace to xyGraph
		xyGraph.addTrace(trace);
	}
	
	public void setFocus() {
		statusMessage = desc+" - setInput(String date:"+date+", String objType:"+objType+", String counter:"+counter+", int objHash:"+objHash+", int serverId:"+serverId+")";
		super.setFocus();
	}

	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}

	class LoadJob extends Job {

		public LoadJob(String name) {
			super(name);
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			MapPack out = null;
			try {
				MapPack param = new MapPack();
				param.put("date", date);
				param.put("objHash", objHash);
				param.put("counter", counter);
				out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_PAST_DATE, param);
			} catch (Throwable t) {
				ConsoleProxy.errorSafe(t.toString());
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			final long[] values = new long[(int)(DateUtil.MILLIS_PER_DAY / DateUtil.MILLIS_PER_HOUR)];
			if (out != null) {
				ListValue timeLv = out.getList("time");
				ListValue valueLv = out.getList("value");
				for (int i = 0; i < timeLv.size(); i++) {
					long time = timeLv.getLong(i);
					int index = (int) (DateUtil.getDateMillis(time) / DateUtil.MILLIS_PER_HOUR);
					values[index] += valueLv.getLong(i);
				}
			}
			
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					double max = 0;
					traceDataProvider.clearTrace();
					for (int i = 0; i < values.length; i++) {
						traceDataProvider.addSample(new Sample(CastUtil.cdouble(i) + 0.5d, CastUtil.cdouble(values[i])));
					}
					max = Math.max(ChartUtil.getMax(traceDataProvider.iterator()), max);
					if (CounterUtil.isPercentValue(objType, counter)) {
						xyGraph.primaryYAxis.setRange(0, 100);
					} else {
						xyGraph.primaryYAxis.setRange(0, max);
					}
					redraw();
				}
			});
			return Status.OK_STATUS;
		}
	}

	

}