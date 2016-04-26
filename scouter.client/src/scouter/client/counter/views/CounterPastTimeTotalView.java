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
import java.util.Locale;
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
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.counter.actions.OpenPastTimeTotalAction;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.AfterMinuteUnit;
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
import scouter.client.util.UIUtil;
import scouter.client.views.ScouterViewPart;
import scouter.io.DataInputX;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class CounterPastTimeTotalView extends ScouterViewPart implements CalendarDialog.ILoadCalendarDialog {
	public static final String ID = CounterPastTimeTotalView.class.getName();
	
	protected String objType;
	protected String counter;
	private String mode;
	protected int serverId;
	protected long startTime;
	protected long endTime;
	
	Label serverText, sDateText, sTimeText, eTimeText;
	CalendarDialog calDialog;
	Composite headerComp;

	IWorkbenchWindow window;
	IToolBarManager man;
	
	boolean actionReg = false;
	
	public void setInput(long stime, long etime, String objType, String counter, int serverId) throws Exception {
		this.startTime = stime;
		this.endTime = etime;
		this.objType = objType;
		this.counter = counter;
		this.mode = CounterUtil.getTotalMode(objType, counter);
		this.serverId = serverId;
		
		setViewTab(objType, counter, serverId);
		
		this.xyGraph.primaryXAxis.setRange(stime, etime);

		Server server = ServerManager.getInstance().getServer(serverId);
		
		serverText.setText("ⓢ"+((server == null)? "?":server.getName())+" |");
		sDateText.setText(DateUtil.format(stime, "yyyy-MM-dd"));
		sTimeText.setText(DateUtil.format(stime, "hh:mm a", Locale.ENGLISH));
		eTimeText.setText(DateUtil.format(etime, "hh:mm a", Locale.ENGLISH));
		
		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objType, counter);
		
		traceDataProvider.setBufferSize((int) ((etime - stime) / TimeTypeEnum.getTime(TimeTypeEnum.REALTIME) + 10));
		
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				load();
			}
		});
	}
	
	private void load() {
		final ArrayList<Pack> values = new ArrayList<Pack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("stime", startTime);
			param.put("etime", endTime);
			param.put("objType", objType);
			param.put("counter", counter);

			tcp.process(RequestCmd.COUNTER_PAST_TIME_ALL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Pack p = in.readPack();
					values.add(p);
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
				double max = 0;
				traceDataProvider.clearTrace();
				Set<Long> timeSet = valueMap.keySet();
				for (long time : timeSet) {
					traceDataProvider.addSample(new Sample(CastUtil.cdouble(time), CastUtil.cdouble(valueMap.get(time))));
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
	}

	private void duplicateView() {
		Server server = ServerManager.getInstance().getServer(serverId);
		String counterDisplay = "";
		if(server != null){
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
		}
		
		Action duplicateAction = new OpenPastTimeTotalAction(window, counterDisplay, objType, counter, Images.getCounterImage(objType, counter, serverId), startTime, endTime, serverId);
		duplicateAction.run();
	}
	
	protected CircularBufferDataProvider traceDataProvider;
	protected XYGraph xyGraph;
	protected Trace trace;
	protected FigureCanvas canvas;
	
	public void createPartControl(Composite parent) {
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		man = getViewSite().getActionBars().getToolBarManager();
		
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				ExUtil.asyncRun(new Runnable() {
					public void run() {
						load();
					}
				});
			}
		});

		man.add(new Separator());
		man.add(new Action("Duplicate", ImageUtil.getImageDescriptor(Images.copy)) {
			public void run() {
				ExUtil.exec(new Runnable() {
					public void run() {
						duplicateView();
					}
				});
			}
		});
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gLayout = new GridLayout(1, true);
		gLayout.horizontalSpacing = 0;
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		composite.setLayout(gLayout);
		createUpperMenu(composite);
		
		Composite chartComposite = new Composite(composite, SWT.NONE);
		chartComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		chartComposite.setLayout(UIUtil.formLayout(0, 0));
		
		chartComposite.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		
		canvas = new FigureCanvas(chartComposite);
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
						setDesc();
					} else {
						setContentDescription("");
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

		canvas.setContents(xyGraph);

		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);

		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		
//		xyGraph.primaryXAxis.setTitle("");
//		xyGraph.primaryYAxis.setTitle("");
		
		traceDataProvider = new CircularBufferDataProvider(true);
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
					ExUtil.asyncRun(new Runnable() {
						public void run() {
							try {
								load();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		});
	}

	private void createUpperMenu(Composite composite) {
		
		headerComp = new Composite(composite, SWT.NONE);
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerComp.setLayout(UIUtil.formLayout(0, 0));
		
		long initialTime = TimeUtil.getCurrentTime(serverId) - DatePeriodUnit.A_DAY.getTime();
		
		Button manualBtn = new Button(headerComp, SWT.PUSH);
		manualBtn.setImage(Images.CTXMENU_RDC);
		manualBtn.setText("Manual");
		manualBtn.setLayoutData(UIUtil.formData(null, -1, 0, 2, 100, -5, null, -1));
		manualBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					Display display = Display.getCurrent();
					if (display == null) {
						display = Display.getDefault();
					}
					
					calDialog = new CalendarDialog(display, CounterPastTimeTotalView.this);
					calDialog.showWithTime(UIUtil.getMousePosition(), startTime);
					
					break;
				}
			}
		});
		
		eTimeText = new Label(headerComp, SWT.NONE);
		eTimeText.setLayoutData(UIUtil.labelFormData(manualBtn));
		eTimeText.setText(DateUtil.format(initialTime + AfterMinuteUnit.FIVE_MIN.getTime(), "hh:mm a", Locale.ENGLISH));
		
		Label label = new Label(headerComp, SWT.NONE);
		label.setLayoutData(UIUtil.labelFormData(eTimeText));
        label.setText("~");
		
		sTimeText = new Label(headerComp, SWT.NONE);
		sTimeText.setLayoutData(UIUtil.labelFormData(label));
		sTimeText.setText(DateUtil.format(initialTime, "hh:mm a", Locale.ENGLISH));
		
        sDateText = new Label(headerComp, SWT.NONE);
		sDateText.setLayoutData(UIUtil.labelFormData(sTimeText));
		sDateText.setText(DateUtil.format(initialTime, "yyyy-MM-dd"));
		
		serverText = new Label(headerComp, SWT.NONE | SWT.RIGHT);
		serverText.setLayoutData(UIUtil.formData(0, 0, 0, 7, sDateText, -5, null, -1));
		serverText.setText("ⓢ");
		
	}
	
	public void setFocus() {
		statusMessage = desc + " - setInput(long stime:"+startTime+", long etime:"+endTime+", String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
		super.setFocus();
	}

	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}

	public void onPressedOk(long startTime, long endTime) {
		try {
			setInput(startTime, endTime, objType, counter, serverId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onPressedOk(String date) {
	}
	public void onPressedCancel() {
	}
	
	public enum DatePeriodUnit {
		A_MONTH ("1 Month", 30 * 24 * 60 * 60 * 1000),
		A_WEEK ("1 Week",  7 * 24 * 60 * 60 * 1000),
		A_DAY ("1 Day", 24 * 60 * 60 * 1000);
		
		private String label;
		private long time;
		
		private DatePeriodUnit(String label,  long time) {
			this.label = label;
			this.time = time;
		}
		
		public String getLabel() {
			return this.label;
		}
		
		public long getTime() {
			return this.time;
		}
		
		public static DatePeriodUnit fromString(String text) {
			if (text != null) {
				for (DatePeriodUnit b : DatePeriodUnit.values()) {
					if (text.equalsIgnoreCase(b.label)) {
						return b;
					}
				}
			}
			return null;
		}
	}
	
	

}