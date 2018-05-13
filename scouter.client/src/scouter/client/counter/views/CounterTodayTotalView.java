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
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import scouter.client.Images;
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
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class CounterTodayTotalView extends ScouterViewPart implements Refreshable {
	public static final String ID = CounterTodayTotalView.class.getName();

	protected String objType;
	protected String counter;
	private String mode;
	protected int serverId;

	protected RefreshThread thread;

	IWorkbenchWindow window;
	IToolBarManager man;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
		this.counter = ids[2];
		this.mode = CounterUtil.getTotalMode(objType, counter);
	}

	private String yesterday;
	boolean isActive = false;
	public void refresh() {
		String date =  DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId) - DateUtil.MILLIS_PER_DAY);
		if (date.equals(yesterday) == false) {
			yesterday = date;
			getYesterdayData(date);
		}
		final ArrayList<Pack> packs = new ArrayList<Pack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objType", objType);
			param.put("counter", counter);
			param.put("mode", CounterUtil.getTotalMode(objType, counter));
			isActive = false;
			tcp.process(RequestCmd.COUNTER_TODAY_ALL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					packs.add(in.readPack());
				}
			});
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		final Map<Long, Double> valueMap = ScouterUtil.getLoadTotalMap(counter, packs, mode, TimeTypeEnum.FIVE_MIN);
		if (valueMap.size() > 0) {
			isActive = true;
		}
		
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				if(isActive){
					setActive();
				}else{
					setInactive();
				}
				String date = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
				try {
					long stime = DateUtil.getTime(date, "yyyyMMdd");
					long etime = stime + DateUtil.MILLIS_PER_DAY;
					xyGraph.primaryXAxis.setRange(stime, etime);
					traceDataProvider.clearTrace();
					Set<Long> timeSet = valueMap.keySet();
					for (long time : timeSet) {
						traceDataProvider.addSample(new Sample(CastUtil.cdouble(time), CastUtil.cdouble(valueMap.get(time))));
					}
					if (CounterUtil.isPercentValue(objType, counter)) {
						xyGraph.primaryYAxis.setRange(0, 100);
					} else {
						double max = ChartUtil.getMax(traceDataProvider.iterator());
						max = Math.max(max, yesterdayMax);
						xyGraph.primaryYAxis.setRange(0, max);
					}
				} catch (Exception e) { e.printStackTrace(); }
				canvas.redraw();
				xyGraph.repaint();
			}
		});
	}
	
	double yesterdayMax;

	private void getYesterdayData(String date) {
		final ArrayList<Pack> values = new ArrayList<Pack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			param.put("objType", objType);
			param.put("counter", counter);

			tcp.process(RequestCmd.COUNTER_PAST_DATE_ALL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Pack p = in.readPack();
					values.add(p);
				};
			});
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		final Map<Long, Double> valueMap = ScouterUtil.getLoadTotalMap(counter, values, mode, TimeTypeEnum.FIVE_MIN);
		
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				yesterdayDataProvider.clearTrace();
				Set<Long> timeSet = valueMap.keySet();
				for (long time : timeSet) {
					yesterdayDataProvider.addSample(new Sample(CastUtil.cdouble(time + DateUtil.MILLIS_PER_DAY), CastUtil.cdouble(valueMap.get(time))));
				}
				yesterdayMax = ChartUtil.getMax(yesterdayDataProvider.iterator());
			}
		});
	}

	protected CircularBufferDataProvider traceDataProvider;
	protected CircularBufferDataProvider yesterdayDataProvider;
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
		man = getViewSite().getActionBars().getToolBarManager();
		
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				yesterday = null;
				yesterdayDataProvider.clearTrace();
				traceDataProvider.clearTrace();
				thread.interrupt();
			}
		});
		
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
						CounterTodayTotalView.this.setContentDescription(desc);
					} else {
						CounterTodayTotalView.this.setContentDescription("");
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
		//xyGraph.setShowTitle(false);

		canvas.setContents(xyGraph);

		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);

		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		ScouterUtil.addShowTotalValueListener(canvas, xyGraph);
		
		createDataProvider();
		createYesterdayProvider();
		
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.F5){
					yesterday = null;
					yesterdayDataProvider.clearTrace();
					traceDataProvider.clearTrace();
					thread.interrupt();
				}
			}
		});
		
		String date = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
		
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+" | (Today Total) [" + date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + "]" + counterDisplay+(!"".equals(counterUnit)?" ("+counterUnit+")":"");
		try {
			setViewTab(objType, counter, serverId);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		long from = DateUtil.yyyymmdd(date);
		long to = from + DateUtil.MILLIS_PER_DAY;

		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objType, counter, from, to);
		thread = new RefreshThread(this, 10000);
		thread.setName(this.toString() + " - " + "objType:"+objType + ", counter:"+counter + ", serverId:"+serverId);
		thread.start();
	}

	private void createDataProvider() {
		traceDataProvider = new CircularBufferDataProvider(true);
		traceDataProvider.setBufferSize(288);
		traceDataProvider.setCurrentXDataArray(new double[] {});
		traceDataProvider.setCurrentYDataArray(new double[] {});

		// create the trace
		trace = new Trace("TOTAL", xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);

		// set trace property
		trace.setPointStyle(PointStyle.NONE);
		trace.getXAxis().setFormatPattern("HH:mm:ss");
		trace.getYAxis().setFormatPattern("#,##0");

		trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		trace.setTraceType(TraceType.AREA);
		trace.setTraceColor(ColorUtil.getInstance().TOTAL_CHART_COLOR);

		// add the trace to xyGraph
		xyGraph.addTrace(trace);
		ChartUtil.addSolidLine(xyGraph, traceDataProvider, ColorUtil.getInstance().TOTAL_CHART_COLOR);
	}

	private void createYesterdayProvider() {
		// create the trace
		yesterdayDataProvider = new CircularBufferDataProvider(true);
		yesterdayDataProvider.setBufferSize(288);
		yesterdayDataProvider.setCurrentXDataArray(new double[] {});
		yesterdayDataProvider.setCurrentYDataArray(new double[] {});
		
		Trace yesterdayTrace = new Trace("yesterday_total", xyGraph.primaryXAxis, xyGraph.primaryYAxis, yesterdayDataProvider);

		// set trace property
		yesterdayTrace.setPointStyle(PointStyle.NONE);
		yesterdayTrace.getXAxis().setFormatPattern("HH:mm:ss");
		yesterdayTrace.getYAxis().setFormatPattern("#,##0");

		yesterdayTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		yesterdayTrace.setAreaAlpha(90);
		yesterdayTrace.setTraceType(TraceType.AREA);
		yesterdayTrace.setTraceColor(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));

		// add the trace to xyGraph
		xyGraph.addTrace(yesterdayTrace);
		//ChartUtil.addSolidLine(xyGraph, yesterdayDataProvider, Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
	}
	
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
}