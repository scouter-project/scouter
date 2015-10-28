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
import org.eclipse.ui.PartInitException;

import scouter.client.model.AgentColorManager;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.client.views.ScouterViewPart;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class CounterRealCountView extends ScouterViewPart implements Refreshable {
	public static final String ID = CounterRealCountView.class.getName();
	
	private IMemento memento;
	
	protected RefreshThread thread;

	protected String objType;
	protected String counter;
	protected int objHash;
	protected int serverId;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	public void setInput(String objType, String counter, int objHash, int serverId) throws Exception {
		this.objHash = objHash;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;
		
		String objName = StringUtil.trimToEmpty(TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId));
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+" | (Realtime) [" + objName + "] " + counterDisplay + (!"".equals(counterUnit)?" ("+counterUnit+")":"");
		this.trace.setName(objName);
		this.trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
		setViewTab(objType, counter, serverId);
		
		if (thread == null) {
			thread = new RefreshThread(this, 5000);
			thread.start();
		}
		
		thread.setName(this.toString() + " - " 
				+ "objType:"+objType
				+ ", counter:"+counter
				+ ", objHash:"+objHash
				+ ", serverId:"+serverId);
	}
	
	private boolean isActive = false;
	
	public void refresh() {
		isActive = false;
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		MapPack out = null;
		try {
			MapPack param = new MapPack();
			param.put("objHash", this.objHash);
			param.put("counter", this.counter);
			param.put("timetype", TimeTypeEnum.FIVE_MIN);
			out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_TODAY, param);
			
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		final long[] values = new long[(int)(DateUtil.MILLIS_PER_DAY / DateUtil.MILLIS_PER_HOUR)];
		if (out != null) {
			isActive = true;
			ListValue timeLv = out.getList("time");
			ListValue valueLv = out.getList("value");
			for (int i = 0; i < timeLv.size(); i++) {
				long time = timeLv.getLong(i);
				int index = (int) (DateUtil.getDateMillis(time) / DateUtil.MILLIS_PER_HOUR);
				values[index] += valueLv.getLong(i);
			}
		}
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				if (isActive == true) {
					setActive();
				} else {
					setInactive();
					return;
				}
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
				canvas.redraw();
				xyGraph.repaint();
			}
		});
	}

	protected CircularBufferDataProvider traceDataProvider;
	protected XYGraph xyGraph;
	protected Trace trace;
	protected FigureCanvas canvas;

	private int leftMargin = 0;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(UIUtil.formLayout(0, 0));
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		
		canvas = new FigureCanvas(parent);
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));

		canvas.setLayoutData(UIUtil.formData(0, leftMargin, 0, 0, 100, 0, 100, 0));
		
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterRealCountView.this.setContentDescription(desc);
					} else {
						CounterRealCountView.this.setContentDescription("");
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

		// create the trace
		trace = new Trace("temp", xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);

		// set trace property
		trace.setPointStyle(PointStyle.NONE);
		//trace.getXAxis().setFormatPattern("HH");
		trace.getYAxis().setFormatPattern("#,##0");

		trace.setLineWidth(15);
		trace.setTraceType(TraceType.BAR);
		trace.setAreaAlpha(200);

		// add the trace to xyGraph
		xyGraph.addTrace(trace);
		
		restoreState();
	}

	public void setFocus() {
		statusMessage = desc + " - setInput(String objType:"+objType+", String counter:"+counter+", int objHash:"+objHash+", int serverId:"+serverId+")";
		super.setFocus();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}
	
	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}

	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento = memento.createChild(ID);
		memento.putString("objType", objType);
		memento.putString("counter", counter);
		memento.putInteger("objHash", objHash);
		memento.putInteger("serverId", serverId);
	}

	private void restoreState() {
		if (memento == null)
			return;
		IMemento m = memento.getChild(ID);
		String objType = m.getString("objType");
		String counter = m.getString("counter");
		int objHash = CastUtil.cint(m.getInteger("objHash"));
		int serverId = CastUtil.cint(m.getInteger("serverId"));
		try {
			setInput(objType, counter, objHash, serverId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}