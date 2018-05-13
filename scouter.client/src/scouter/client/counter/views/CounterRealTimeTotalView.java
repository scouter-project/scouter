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

import java.io.IOException;
import java.util.ArrayList;
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
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.INetReader;
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
import scouter.client.util.ImageUtil;
import scouter.client.util.MenuUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.views.ScouterViewPart;
import scouter.io.DataInputX;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.DoubleValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class CounterRealTimeTotalView extends ScouterViewPart implements Refreshable {
	public static final String ID = CounterRealTimeTotalView.class.getName();

	protected String objType;
	protected String counter;
	protected int serverId;
	private String mode;

	protected RefreshThread thread = null;

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
		this.counter = ids[2];
		mode = CounterUtil.getTotalMode(objType, counter);
	}
	
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
						CounterRealTimeTotalView.this.setContentDescription(desc);
					} else {
						CounterRealTimeTotalView.this.setContentDescription("");
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
		
		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
		xyGraph.primaryYAxis.setFormatPattern("#,##0");
		
		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));

		traceDataProvider = new CircularBufferDataProvider(true);
		traceDataProvider.setBufferSize(155);
		traceDataProvider.setCurrentXDataArray(new double[] {});
		traceDataProvider.setCurrentYDataArray(new double[] {});

		// create the trace
		trace = new Trace("TOTAL", xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);

		// set trace property
		trace.setPointStyle(PointStyle.NONE);

		trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		trace.setTraceType(TraceType.AREA);
		trace.setTraceColor(ColorUtil.getInstance().TOTAL_CHART_COLOR);

		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		// add the trace to xyGraph
		xyGraph.addTrace(trace);
		ChartUtil.addSolidLine(xyGraph, traceDataProvider, ColorUtil.getInstance().TOTAL_CHART_COLOR);
		ScouterUtil.addShowTotalValueListener(canvas, xyGraph);
		
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.F5){
					manualRefresh = true;
					thread.interrupt();
				}
			}
		});
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				manualRefresh = true;
				thread.interrupt();
			}
		});
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+" | (Current Total) " + counterDisplay + (!"".equals(counterUnit)?" ("+counterUnit+")":"");
		try {
			setViewTab(objType, counter, serverId);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objType, counter, 0, 0);
		thread = new RefreshThread(this, 2000);
		thread.setName(this.toString() + " - "	+ "objType:"+objType + ", counter:"+counter + ", serverId:"+serverId);
		thread.start();
	}

	boolean manualRefresh = false;
	boolean isActive = false;
	public void refresh() {
		if (manualRefresh) {
			manualRefresh = false;
			getPrevTotalPerf();
		}
		MapPack map = new MapPack();
		map.put("objType", objType);
		map.put("counter", counter);

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		final DoubleValue value = new DoubleValue();
		isActive = false;
		List<Pack> result = null;
		try {
			result = tcp.process(RequestCmd.COUNTER_REAL_TIME_ALL, map);
		} catch(Throwable t){
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		if (result.size() > 0) {
			value.value = ScouterUtil.getRealTotalValue(counter, result, mode);
			isActive = true;
		}
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				
				if(isActive){
					setActive();
				}else{
					setInactive();
				}
				
				long now = TimeUtil.getCurrentTime(serverId);
				traceDataProvider.addSample(new Sample(now, value.value));
				xyGraph.primaryXAxis.setRange(now - DateUtil.MILLIS_PER_MINUTE * 5, now + 1);
				
				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					double max = ChartUtil.getMax(traceDataProvider.iterator());
					xyGraph.primaryYAxis.setRange(0, max);
				}
				canvas.redraw();
				xyGraph.repaint();
			}
		});

	}

	private void getPrevTotalPerf() {
		final ArrayList<Pack> values = new ArrayList<Pack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			long etime = TimeUtil.getCurrentTime(serverId);
			long stime = etime - DateUtil.MILLIS_PER_MINUTE * 5;
			MapPack param = new MapPack();
			param.put("stime", stime);
			param.put("etime", etime);
			param.put("objType", objType);
			param.put("counter", counter);

			tcp.process(RequestCmd.COUNTER_PAST_TIME_ALL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Pack mpack =  in.readPack();
					values.add(mpack);
				};
			});
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		final Map<Long, Double> valueMap = ScouterUtil.getLoadTotalMap(counter, values, mode, TimeTypeEnum.REALTIME);
		
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
					traceDataProvider.clearTrace();
					Set<Long> timeSet = valueMap.keySet();
					for (long time : timeSet) {
						double value = CastUtil.cdouble(valueMap.get(time));
						traceDataProvider.addSample(new Sample(time, value));
					}
			}
		});
	}

	protected CircularBufferDataProvider traceDataProvider;
	protected XYGraph xyGraph;
	protected Trace trace;
	protected FigureCanvas canvas;
	GridData data;

	public void setFocus() {
		statusMessage = desc + " - setInput(String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
		super.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}

	public void addValue(long now, double value) {
		traceDataProvider.addSample(new Sample(now, value));
	}
}