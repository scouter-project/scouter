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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.counter.actions.OpenDailyServiceCountAction;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.views.ScouterViewPart;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

public class CounterTodayCountView extends ScouterViewPart implements Refreshable {
	public static final String ID = CounterTodayCountView.class.getName();
	
	protected RefreshThread thread;

	protected String objType;
	protected String counter;
	private String mode;
	protected int serverId;

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
	private boolean isActive = false;
	double yesterdayMax;
	
	private void getYesterdayData(String date) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		List<Pack> out = null;
		try {
			MapPack param = new MapPack();
			param.put("counter", this.counter);
			param.put("date", date);
			param.put("objType", this.objType);
			out = tcp.process(RequestCmd.COUNTER_PAST_DATE_ALL, param);
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		final long[] values = new long[(int)(DateUtil.MILLIS_PER_DAY / DateUtil.MILLIS_PER_HOUR)];
		if (out != null) {
			Map<Long, Double> valueMap = ScouterUtil.getLoadTotalMap(counter, out, mode, TimeTypeEnum.FIVE_MIN);
			Iterator<Long> itr = valueMap.keySet().iterator();
			while (itr.hasNext()) {
				long time = itr.next();
				int index = (int) (DateUtil.getDateMillis(time) / DateUtil.MILLIS_PER_HOUR);
				values[index] += valueMap.get(time);
			}
		}
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				CircularBufferDataProvider yesterdayProvider = (CircularBufferDataProvider) yesterdayTrace.getDataProvider();
				yesterdayProvider.clearTrace();
				for (int i = 0; i < values.length; i++) {
					yesterdayProvider.addSample(new Sample(CastUtil.cdouble(i) + 0.5d, CastUtil.cdouble(values[i])));
				}
				yesterdayMax = ChartUtil.getMax(yesterdayProvider.iterator());
			}
		});
	}
	
	public void refresh() {
		String date =  DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId) - DateUtil.MILLIS_PER_DAY);
		if (date.equals(yesterday) == false) {
			yesterday = date;
			getYesterdayData(date);
		}
		isActive = false;
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		List<Pack> out = null;
		try {
			MapPack param = new MapPack();
			param.put("counter", this.counter);
			param.put("objType", this.objType);
			out = tcp.process(RequestCmd.COUNTER_TODAY_ALL, param);
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		final long[] values = new long[(int)(DateUtil.MILLIS_PER_DAY / DateUtil.MILLIS_PER_HOUR)];
		if (out != null && out.size() > 0) {
			Map<Long, Double> valueMap = ScouterUtil.getLoadTotalMap(counter, out, mode, TimeTypeEnum.FIVE_MIN);
			Iterator<Long> itr = valueMap.keySet().iterator();
			while (itr.hasNext()) {
				long time = itr.next();
				int index = (int) (DateUtil.getDateMillis(time) / DateUtil.MILLIS_PER_HOUR);
				values[index] += valueMap.get(time);
			}
			isActive = true;
		}
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				if (isActive == true) {
					setActive();
					traceDataProvider.clearTrace();
					for (int i = 0; i < values.length; i++) {
						traceDataProvider.addSample(new Sample(CastUtil.cdouble(i) + 0.5d, CastUtil.cdouble(values[i])));
					}
				} else {
					setInactive();
				}
				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					double max = ChartUtil.getMax(traceDataProvider.iterator());
					max = Math.max(max, yesterdayMax);
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
	protected Trace yesterdayTrace;
	protected FigureCanvas canvas;

	int xAxisUnitWidth;
	int lineWidth;
	
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
						CounterTodayCountView.this.setContentDescription(desc);
					} else {
						CounterTodayCountView.this.setContentDescription("");
					}
					r = canvas.getClientArea();
					lock = false;
				}
				if(xyGraph == null)
					return;
				xyGraph.setSize(r.width, r.height);
				lineWidth = r.width / 30;
				trace.setLineWidth(lineWidth);
				yesterdayTrace.setLineWidth(lineWidth);
				xAxisUnitWidth= xyGraph.primaryXAxis.getValuePosition(1, false) - xyGraph.primaryXAxis.getValuePosition(0, false);
			}

			public void controlMoved(ControlEvent e) {
			}
		});
		
		canvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				writedValueMode = false;
				canvas.redraw();
			}
			public void mouseDown(MouseEvent e) {
				writeValue(e.x);
			}
			public void mouseDoubleClick(MouseEvent e) {}
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
		trace = new Trace("TotalCount", xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);

		// set trace property
		trace.setPointStyle(PointStyle.NONE);
		//trace.getXAxis().setFormatPattern("HH");
		trace.getYAxis().setFormatPattern("#,##0");

		trace.setLineWidth(15);
		trace.setTraceType(TraceType.BAR);
		trace.setAreaAlpha(200);
		trace.setTraceColor(ColorUtil.getInstance().TOTAL_CHART_COLOR);

		// add the trace to xyGraph
		xyGraph.addTrace(trace);
		
		CircularBufferDataProvider yetserdaytraceDataProvider = new CircularBufferDataProvider(true);
		yetserdaytraceDataProvider.setBufferSize(24);
		yetserdaytraceDataProvider.setCurrentXDataArray(new double[] {});
		yetserdaytraceDataProvider.setCurrentYDataArray(new double[] {});
		
		yesterdayTrace = new Trace("YesterdayTotalCount", xyGraph.primaryXAxis, xyGraph.primaryYAxis, yetserdaytraceDataProvider);
		yesterdayTrace.setPointStyle(PointStyle.NONE);
		yesterdayTrace.getYAxis().setFormatPattern("#,##0");
		yesterdayTrace.setLineWidth(15);
		yesterdayTrace.setTraceType(TraceType.BAR);
		yesterdayTrace.setAreaAlpha(90);
		yesterdayTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GRAY));
		xyGraph.addTrace(yesterdayTrace);
		
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String objectDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			objectDisplay = server.getCounterEngine().getDisplayNameObjectType(objType);
			counterUnit = ""+server.getCounterEngine().getCounterUnit(objType, counter);
		}
		desc = "ⓢ"+svrName+" | (Today) [" + objectDisplay + "] " + counterDisplay+(!"".equals(counterUnit)?" ("+counterUnit+")":"");
		try {
			setViewTab(objType, counter, serverId);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		// Add context menu
		MenuManager mgr = new MenuManager(); 
		mgr.setRemoveAllWhenShown(true);
		final IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		mgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				mgr.add(new OpenDailyServiceCountAction(win, objType, counter, Images.bar, serverId));
			}
		});
		Menu menu = mgr.createContextMenu(canvas); 
		canvas.setMenu(menu); 
		
		thread = new RefreshThread(this, 5000);
		thread.setName(this.toString() + " - "	+ "objType:"+objType + ", counter:"+counter + ", serverId:"+serverId);
		thread.start();
	}
	
	boolean writedValueMode = false;
	int lastWritedX;
	
	public void writeValue(int ex) {
		double x = xyGraph.primaryXAxis.getPositionValue(ex, false);
		int index = (int) x;
		if (index < 0) {
			return;
		}
		Sample sample = (Sample) trace.getDataProvider().getSample(index);
		if (sample != null) {
			double y = sample.getYValue();
			int height = xyGraph.primaryYAxis.getValuePosition(y, false);
			int startX = xyGraph.primaryXAxis.getValuePosition((int) x, false);
			GC gc = new GC(canvas);
			Font font = new Font(null, "Verdana", 10, SWT.BOLD);
			gc.setFont(font);
			String value = FormatUtil.print(y, "#,###");
			Point textSize = gc.textExtent(value);
			gc.drawText(value, startX + (xAxisUnitWidth - textSize.x) / 2  , height - 20, true);
			int ground = xyGraph.primaryYAxis.getValuePosition(0, false);
			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_MAGENTA));
			gc.drawRectangle(startX + (xAxisUnitWidth - lineWidth) / 2, height, lineWidth, ground - height);
			gc.fillRectangle(startX + (xAxisUnitWidth - lineWidth) / 2 , height, lineWidth, ground - height);
			gc.dispose();
			writedValueMode = true;
			lastWritedX = ex;
		}
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