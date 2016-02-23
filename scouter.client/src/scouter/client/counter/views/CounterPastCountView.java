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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.counter.actions.OpenDailyServiceCountAction;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.views.ScouterViewPart;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class CounterPastCountView extends ScouterViewPart {
	public static final String ID = CounterPastCountView.class.getName();
	
	protected String objType;
	protected String counter;
	protected String date;
	private String mode;
	protected int serverId;

	int xAxisUnitWidth;
	int lineWidth;
	
	public void setInput(final String date, final String objType, final String counter, final int serverId) throws Exception {
		this.date = date;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;
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
		desc = "ⓢ"+svrName+ " | (Past) [" + objectDisplay + "][" + date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + "] Past-date " + counterDisplay+(!"".equals(counterUnit)?" ("+counterUnit+")":"");
		setViewTab(objType, counter, serverId);
		MenuManager mgr = new MenuManager(); 
		mgr.setRemoveAllWhenShown(true);
		final IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		mgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				mgr.add(new OpenDailyServiceCountAction(win, objType, counter, Images.bar, serverId, date));
			}
		});
		Menu menu = mgr.createContextMenu(canvas); 
		canvas.setMenu(menu); 
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
		canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterPastCountView.this.setContentDescription(desc);
					} else {
						CounterPastCountView.this.setContentDescription("");
					}
					r = canvas.getClientArea();
					lock = false;
				}
				if(xyGraph == null)
					return;
				xyGraph.setSize(r.width, r.height);
				lineWidth = r.width / 30;
				trace.setLineWidth(lineWidth);
				xAxisUnitWidth= xyGraph.primaryXAxis.getValuePosition(1, false) - xyGraph.primaryXAxis.getValuePosition(0, false);
			}

			public void controlMoved(ControlEvent e) {
			}
		});
		
		canvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
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
		trace = new Trace("temp", xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);

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
	}

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
		}
	}
	
	public void setFocus() {
		statusMessage = desc + " - setInput(String date:"+date+", String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
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
			List<Pack> out = null;
			try {
				MapPack param = new MapPack();
				param.put("date", date);
				param.put("counter", counter);
				param.put("objType", objType);
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