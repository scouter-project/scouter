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

import au.com.bytecode.opencsv.CSVWriter;
import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import scouter.client.Images;
import scouter.client.counter.actions.OpenPastTimeAllAction;
import scouter.client.model.AgentColorManager;
import scouter.client.model.TextProxy;
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
import scouter.client.util.TimedSeries;
import scouter.client.util.UIUtil;
import scouter.client.views.ScouterViewPart;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CounterPastTimeAllView extends ScouterViewPart implements CalendarDialog.ILoadCalendarDialog {
	public static final String ID = CounterPastTimeAllView.class.getName();
	
	protected String objType;
	protected String counter;
	protected long startTime;
	protected long endTime;
	protected int serverId;
	
	Label serverText, sDateText, sTimeText, eTimeText;
	CalendarDialog calDialog;
	Composite headerComp;
	Trace nearestTrace;

	IWorkbenchWindow window;
	IToolBarManager man;
	
	boolean actionReg = false;
	
	public void setInput(long stime, long etime, String objType, String counter, int serverId) throws Exception {
		this.startTime = stime;
		this.endTime = etime;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;
		setViewTab(objType, counter, serverId, false);

		this.xyGraph.primaryXAxis.setRange(stime, etime);

		Server server = ServerManager.getInstance().getServer(serverId);
		
		String counterDisplay = "";
		if(server != null){
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			desc = "(Past All) [" + DateUtil.format(stime, "yyyy-MM-dd HH:mm:ss") + " ~ " + DateUtil.format(etime, "HH:mm:ss") + "]" + counterDisplay;
		}
		
		serverText.setText("ⓢ"+((server == null)? "?":server.getName())+" |");
		sDateText.setText(DateUtil.format(stime, "yyyy-MM-dd"));
		sTimeText.setText(DateUtil.format(stime, "hh:mm a", Locale.ENGLISH));
		eTimeText.setText(DateUtil.format(etime, "hh:mm a", Locale.ENGLISH));
		
		Iterator<Trace> itr = traces.values().iterator();
		while (itr.hasNext()) {
			Trace tr = itr.next();
			this.xyGraph.removeTrace(tr);
		}
		traces.clear();
		
		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objType, counter, stime, etime);

		ExUtil.asyncRun(new Runnable() {
			public void run() {
				load(CounterPastTimeAllView.this.counter);
			}
		});
	}
	
	private void load(final String counter) {
		final ArrayList<MapPack> values = new ArrayList<MapPack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("stime", startTime);
			param.put("etime", endTime);
			param.put("objType", objType);
			param.put("counter", counter);

			tcp.process(RequestCmd.COUNTER_PAST_TIME_ALL, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					MapPack mpack = (MapPack) in.readPack();
					values.add(mpack);
				};
			});
			
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				double max = 0;
				for (MapPack mpack : values) {
					int objHash = mpack.getInt("objHash");
					
					//String insName = mpack.getText("objName");
					ListValue time = mpack.getList("time");
					ListValue value = mpack.getList("value");
					Trace trace = intern(objHash);
					CircularBufferDataProvider provider = (CircularBufferDataProvider) trace.getDataProvider();
					
					provider.clearTrace();
					for (int i = 0; time != null && i < time.size(); i++) {
						long x = time.getLong(i);
						double y = value.getDouble(i);
						provider.addSample(new Sample(x, y));
					}
					max = Math.max(ChartUtil.getMax(provider.iterator()), max);
				}
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
		
		Action duplicateAction = new OpenPastTimeAllAction(window, counterDisplay, objType, counter, Images.getCounterImage(objType, counter, serverId), startTime, endTime, serverId);
		duplicateAction.run();
	}
	
	protected XYGraph xyGraph;
	protected FigureCanvas canvas;

	public void createPartControl(Composite parent) {
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				ExUtil.exec(new Runnable() {
					public void run() {
						try {
							setInput(startTime, endTime, objType, counter, serverId);
						} catch (Exception e) {
							e.printStackTrace();
						}
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
		
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
		menuManager.add(new Action("Export CSV", ImageUtil.getImageDescriptor(Images.csv)) {
			public void run() {
				FileDialog dialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
				dialog.setOverwrite(true);
				String filename = "[" + DateUtil.format(startTime, "yyyyMMdd_HHmmss") + "-" 
				+ DateUtil.format(endTime, "HHmmss") + "]" + objType + "_" + counter + ".csv";
				dialog.setFileName(filename);
				dialog.setFilterExtensions(new String[] { "*.csv", "*.*" });
				dialog.setFilterNames(new String[] { "CSV File(*.csv)", "All Files" });
				String fileSelected = dialog.open();
				if (fileSelected != null) {
					new ExportDataTask(fileSelected).schedule();
				}
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
					if (ChartUtil.isShowLegendAllowSize(r.width, r.height)) {
						xyGraph.setShowLegend(true);
					} else {
						xyGraph.setShowLegend(false);
					}
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterPastTimeAllView.this.setContentDescription(desc);
					} else {
						CounterPastTimeAllView.this.setContentDescription("");
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

		canvas.setContents(xyGraph);

		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);
		xyGraph.primaryYAxis.setAutoScale(true);

		xyGraph.primaryYAxis.setShowMajorGrid(true);
		
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		final DefaultToolTip toolTip = new DefaultToolTip(canvas, DefaultToolTip.RECREATE, true);
		toolTip.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		toolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		canvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				if (nearestTrace != null) {
					nearestTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
					nearestTrace = null;
				}
				toolTip.hide();
			}
			
			public void mouseDown(MouseEvent e) {
				double x = xyGraph.primaryXAxis.getPositionValue(e.x, false);
				double y = xyGraph.primaryYAxis.getPositionValue(e.y, false);
				if (x < 0 || y < 0) {
					return;
				}
				double minDistance = 30.0d;
				long time = 0;
				double value = 0;
				for (Trace t : traces.values()) {
					ISample s = ScouterUtil.getNearestPoint(t.getDataProvider(), x);
					if (s != null) {
						int x2 = xyGraph.primaryXAxis.getValuePosition(s.getXValue(), false);
						int y2 = xyGraph.primaryYAxis.getValuePosition(s.getYValue(), false);
						double distance = ScouterUtil.getPointDistance(e.x, e.y, x2, y2);
						if (minDistance > distance) {
							minDistance = distance;
							nearestTrace = t;
							time = (long) s.getXValue();
							value = s.getYValue();
						}
					}
				}
				if (nearestTrace != null) {
					int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
					nearestTrace.setLineWidth(width + 2);
					toolTip.setText(nearestTrace.getName()
							+ "\nTime : " + DateUtil.format(time, "HH:mm:ss")
							+ "\nValue : " +  FormatUtil.print(value, "#,###.##"));
					toolTip.show(new Point(e.x, e.y));
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
		});
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.F5){
					ExUtil.asyncRun(new Runnable() {
						public void run() {
							try {
								setInput(startTime, endTime, objType, counter, serverId);
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
					
					calDialog = new CalendarDialog(display, CounterPastTimeAllView.this);
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
		statusMessage = desc+" - setInput(long stime:"+startTime+", long etime:"+endTime+", String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
		super.setFocus();
	}

	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}

	private Map<Integer, Trace> traces = new HashMap<Integer, Trace>();

	private synchronized Trace intern(int objHash) {
		Trace trace = traces.get(objHash);
		if (trace != null)
			return trace;

		CircularBufferDataProvider traceDataProvider = new CircularBufferDataProvider(true);
		traceDataProvider.setBufferSize((int) ((endTime - startTime) / (DateUtil.MILLIS_PER_SECOND * 2)) + 1);
		traceDataProvider.setCurrentXDataArray(new double[] {});
		traceDataProvider.setCurrentYDataArray(new double[] {});
		
		String name = StringUtil.trimToEmpty(TextProxy.object.getLoadText(DateUtil.yyyymmdd(startTime), objHash, serverId));

		// create the trace
		trace = new Trace(name, xyGraph.primaryXAxis, xyGraph.primaryYAxis, traceDataProvider);

		// set trace property
		trace.setPointStyle(PointStyle.NONE);

		trace.getXAxis().setTitle("");
		trace.getYAxis().setTitle("");
		trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		trace.getXAxis().setFormatPattern("HH:mm");
		trace.getYAxis().setFormatPattern("#,##0");
		trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));

		xyGraph.addTrace(trace);

		traces.put(objHash, trace);

		return trace;
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
	
	class ExportDataTask extends Job {
		
		String filePath;

		public ExportDataTask(String filePath) {
			super("Export...");
			this.filePath = filePath;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				TimedSeries<String, Double> sereis = new TimedSeries<String, Double>();
				Range xRange = xyGraph.primaryXAxis.getRange();
				long lower = (long) xRange.getLower();
				long upper = (long) xRange.getUpper();
				List<Trace> traceList = xyGraph.getPlotArea().getTraceList();
				List<String> titleValues = new ArrayList<String>();
				titleValues.add("Time");
				for (Trace t : traceList) {
					titleValues.add(t.getName());
					CircularBufferDataProvider provider = (CircularBufferDataProvider) t.getDataProvider();
					for (int inx = 0; inx < provider.getSize(); inx++) {
						Sample sample = (Sample) provider.getSample(inx);
						double x = sample.getXValue();
						if(x < lower || x > upper) {
							continue;
						}
						double y = sample.getYValue();
						sereis.add(t.getName(), (long)x, y);
					}
				}
				List<String[]> values = new ArrayList<String[]>();
				values.add(titleValues.toArray(new String[titleValues.size()]));
				while (lower < upper) {
					List<String> value = new ArrayList<String>();
					value.add(DateUtil.format(lower, "HH:mm:ss"));
					for (int i = 1; i < titleValues.size(); i++) {
						String objName = titleValues.get(i);
						Double d = sereis.getInTime(objName, lower, 3000);
						if (d != null) {
							value.add(FormatUtil.print(d.doubleValue(), "#,###.##"));
						} else {
							value.add("");
						}
					}
					values.add(value.toArray(new String[value.size()]));
					lower += 2000;
				}
				CSVWriter cw = new CSVWriter(new FileWriter(filePath));
				cw.writeAll(values);
				cw.flush();
				cw.close();
			} catch (Exception e) {
				e.printStackTrace();
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		}
		
	}
	
}