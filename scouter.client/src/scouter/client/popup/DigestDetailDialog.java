package scouter.client.popup;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.hibernate.jdbc.util.BasicFormatterImpl;

import scouter.client.model.DigestModel;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.SpinnerThread;
import scouter.client.util.SqlFormatUtil;
import scouter.client.util.UIUtil;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

public class DigestDetailDialog {
	
	double PICO = Math.pow(10, -12);
	
	ArrayList<Resource> resources = new ArrayList<Resource>();
	
	TabFolder tabFolder;
	DigestModel model;
	String date;
	long stime;
	long etime;
	int serverId;
	
	TabItem queryDetailItem;
	TabItem exampleQueryItem;
	TabItem graphsItem;
	
	public void show(DigestModel model, long stime, long etime, int serverId) {
		this.model = model;
		this.date = DateUtil.yyyymmdd(stime);
		this.stime = stime;
		this.etime = etime;
		this.serverId = serverId;
		final Shell dialog = new Shell(Display.getDefault(), SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setText("Digest Detail");
		dialog.setLayout(new GridLayout(1, true));
		tabFolder = new TabFolder(dialog, SWT.BORDER);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryDetailItem = new TabItem(tabFolder, SWT.NULL);
		queryDetailItem.setText("Query Detail");
		queryDetailItem.setControl(getQueryDetailControl(tabFolder));
		exampleQueryItem = new TabItem(tabFolder, SWT.NULL);
		exampleQueryItem.setText("Example Query");
		exampleQueryItem.setControl(getSpinnerControl(tabFolder));
		graphsItem = new TabItem(tabFolder, SWT.NULL);
		graphsItem.setText("Graphs");
		graphsItem.setControl(getSpinnerControl(tabFolder));
		
		Button closeBtn = new Button(dialog, SWT.PUSH);
		GridData gr = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		gr.widthHint = 100;
		closeBtn.setLayoutData(gr);
		closeBtn.setText("&Close");
		closeBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (Resource r : resources) {
					if (r.isDisposed() == false) {
						r.dispose();
					}
				}
				dialog.close();
			}
		});
		
		dialog.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				for (Resource r : resources) {
					if (r.isDisposed() == false) {
						r.dispose();
					}
				}
			}
		});
		
		loadExampleQuery();
		loadGraphsData();
		
		dialog.pack();
		dialog.open();
	}
	
	private Control getQueryDetailControl(Composite parent) {
		Font boldFont = new Font(Display.getDefault(), "Arial", 9, SWT.BOLD);
		resources.add(boldFont);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		StyledText sqlText = new StyledText(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 500;
		gd.heightHint = 150;
		sqlText.setLayoutData(gd);
		String digest = TextProxy.maria.getLoadText(date, model.digestHash, serverId);
		if (digest != null)	digest = new BasicFormatterImpl().format(TextProxy.maria.getLoadText(date, model.digestHash, serverId));
		SqlFormatUtil.applyStyledFormat(sqlText, digest);
		Label label1 = new Label(composite, SWT.NONE);
		label1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label1.setText("Execution Time Statistics");
		label1.setFont(boldFont);
		Table exTimeTable = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		exTimeTable.setLayoutData(gd);
		exTimeTable.setHeaderVisible(true);
		TableColumn maxTimeCol = new TableColumn(exTimeTable, SWT.NONE);
		maxTimeCol.setWidth(125);
		maxTimeCol.setText("Max Time");
		TableColumn minTimeCol = new TableColumn(exTimeTable, SWT.NONE);
		minTimeCol.setWidth(125);
		minTimeCol.setText("Min Time");
		TableColumn avgTimeCol = new TableColumn(exTimeTable, SWT.NONE);
		avgTimeCol.setWidth(125);
		avgTimeCol.setText("Avg Time");
		TableColumn totalTimeCol = new TableColumn(exTimeTable, SWT.NONE);
		totalTimeCol.setWidth(125);
		totalTimeCol.setText("Total Time");
		TableItem exTimeItem = new TableItem(exTimeTable, SWT.NONE);
		exTimeItem.setText(
				new String[] {FormatUtil.print(model.maxResponseTime * PICO, "#,##0.00#"), 
						FormatUtil.print(model.minResponseTime * PICO, "#,##0.00#"),
						FormatUtil.print(model.avgResponseTime * PICO, "#,##0.00#"),
						FormatUtil.print(model.sumResponseTime * PICO, "#,##0.00#")}
				);
		
		label1 = new Label(composite, SWT.NONE);
		label1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label1.setText("Row Statistics");
		label1.setFont(boldFont);
		Table rowTable = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		rowTable.setLayoutData(gd);
		rowTable.setHeaderVisible(true);
		TableColumn rowsAffectedCol = new TableColumn(rowTable, SWT.NONE);
		rowsAffectedCol.setWidth(165);
		rowsAffectedCol.setText("Rows Affected");
		TableColumn rowsSentCol = new TableColumn(rowTable, SWT.NONE);
		rowsSentCol.setWidth(165);
		rowsSentCol.setText("Rows Sent");
		TableColumn rowsExaminedCol = new TableColumn(rowTable, SWT.NONE);
		rowsExaminedCol.setWidth(165);
		rowsExaminedCol.setText("Rows Examined");
		TableItem rowItem = new TableItem(rowTable, SWT.NONE);
		rowItem.setText(
				new String[] {FormatUtil.print(model.rowsAffected, "#,##0"), 
						FormatUtil.print(model.rowsSent, "#,##0"),
						FormatUtil.print(model.rowsExamined, "#,##0")}
				);
		
		label1 = new Label(composite, SWT.NONE);
		label1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label1.setText("Execution Summary");
		label1.setFont(boldFont);
		Table sumTable = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		sumTable.setLayoutData(gd);
		sumTable.setHeaderVisible(true);
		TableColumn execSum = new TableColumn(sumTable, SWT.NONE);
		execSum.setWidth(100);
		execSum.setText("Executions");
		TableColumn errorCol = new TableColumn(sumTable, SWT.NONE);
		errorCol.setWidth(100);
		errorCol.setText("Errors");
		TableColumn warnCol = new TableColumn(sumTable, SWT.NONE);
		warnCol.setWidth(100);
		warnCol.setText("Warnings");
		TableColumn tableScansCol = new TableColumn(sumTable, SWT.NONE);
		tableScansCol.setWidth(100);
		tableScansCol.setText("Table Scans");
		TableColumn badIndexCol = new TableColumn(sumTable, SWT.NONE);
		badIndexCol.setWidth(100);
		badIndexCol.setText("Bad Index Used");
		TableItem sumItem = new TableItem(sumTable, SWT.NONE);
		sumItem.setText(
				new String[] {FormatUtil.print(model.execution, "#,##0"), 
						FormatUtil.print(model.errorCnt, "#,##0"),
						FormatUtil.print(model.warnCnt, "#,##0"),
						FormatUtil.print(model.noIndexUsed, "#,##0"),
						FormatUtil.print(model.noGoodIndexUsed, "#,##0")}
				);
		
		label1 = new Label(composite, SWT.NONE);
		label1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label1.setText("Time Span");
		label1.setFont(boldFont);
		Label timeLbl = new Label(composite, SWT.NONE);
		timeLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		timeLbl.setText(DateUtil.timestamp(stime) + " ~ " + DateUtil.timestamp(etime));
		label1 = new Label(composite, SWT.NONE);
		label1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label1.setText("First Seen");
		label1.setFont(boldFont);
		Label firstLbl = new Label(composite, SWT.NONE);
		firstLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		firstLbl.setText(DateUtil.timestamp(model.firstSeen));
		return composite;
	}
	
	private Control getSpinnerControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		SpinnerThread spinnerThread = new SpinnerThread(composite, new GridData(SWT.CENTER, SWT.CENTER, true, true));
		spinnerThread.start();
		return composite;
	}
	
	private Control getExampleQuery(Composite parent, MapPack m) {
		int sqlHash = m.getInt("sql_text");
		long time = m.getLong("time");
		long threadId = m.getLong("thread_id");
		long timer = m.getLong("timer");
		long lock_time = m.getLong("lock_time");
		
		
		Font boldFont = new Font(Display.getDefault(), "Arial", 9, SWT.BOLD);
		resources.add(boldFont);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		StyledText sqlText = new StyledText(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 500;
		gd.heightHint = 150;
		sqlText.setLayoutData(gd);
		String sql = TextProxy.maria.getLoadText(date, sqlHash, serverId);
		if (StringUtil.isEmpty(sql) == false) {
			sql = new BasicFormatterImpl().format(TextProxy.maria.getLoadText(date, sqlHash, serverId));
			SqlFormatUtil.applyStyledFormat(sqlText, sql);
		}
		
		Label label1 = new Label(composite, SWT.NONE);
		label1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label1.setText("Execution Time");
		label1.setFont(boldFont);
		Label timerLbl = new Label(composite, SWT.NONE);
		timerLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		timerLbl.setText(FormatUtil.print(timer  * PICO, "#,##0.00#") + " sec");
		
		Label label2 = new Label(composite, SWT.NONE);
		label2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label2.setText("Date");
		label2.setFont(boldFont);
		Label dateLbl = new Label(composite, SWT.NONE);
		dateLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		dateLbl.setText(DateUtil.format(time, "yyyy-MM-dd HH:mm:ss"));
		
		Label label3 = new Label(composite, SWT.NONE);
		label3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label3.setText("Thread ID");
		label3.setFont(boldFont);
		Label threadLbl = new Label(composite, SWT.NONE);
		threadLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		threadLbl.setText(CastUtil.cString(threadId));
		
		Label label4 = new Label(composite, SWT.NONE);
		label4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		label4.setText("Lock Time");
		label4.setFont(boldFont);
		Label lockLbl = new Label(composite, SWT.NONE);
		lockLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		lockLbl.setText(FormatUtil.print(lock_time  * PICO, "#,##0.00#") + " sec");
		return composite;
	}
	
	XYGraph exTimeGraph;
	XYGraph exCntGraph;
	XYGraph rowsGraph;
	
	private Control getGraphsControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		composite.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		composite.setBackgroundMode(SWT.INHERIT_FORCE);
		
		final FigureCanvas exTimeCanvas = new FigureCanvas(composite);
		exTimeCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		exTimeCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		exTimeGraph = new XYGraph();
		exTimeCanvas.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = exTimeCanvas.getClientArea();
				exTimeGraph.setSize(r.width, r.height);
			}
		});
		exTimeGraph.setShowLegend(true);
		exTimeGraph.setShowTitle(true);
		exTimeGraph.setTitle("Execution Time (ms)");
		exTimeCanvas.setContents(exTimeGraph);
		exTimeGraph.primaryXAxis.setDateEnabled(true);
		exTimeGraph.primaryXAxis.setShowMajorGrid(true);
		exTimeGraph.primaryYAxis.setAutoScale(true);
		exTimeGraph.primaryYAxis.setShowMajorGrid(true);
		exTimeGraph.primaryXAxis.setTitle("");
		exTimeGraph.primaryYAxis.setTitle("");
		exTimeGraph.primaryXAxis.setRange(stime, etime);
		
		final FigureCanvas exCntCanvas = new FigureCanvas(composite);
		exCntCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		exCntCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		exCntGraph = new XYGraph();
		exTimeCanvas.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = exCntCanvas.getClientArea();
				exCntGraph.setSize(r.width, r.height);
			}
		});
		exCntGraph.setShowLegend(false);
		exCntGraph.setShowTitle(true);
		exCntGraph.setTitle("Executions");
		exCntCanvas.setContents(exCntGraph);
		exCntGraph.primaryXAxis.setDateEnabled(true);
		exCntGraph.primaryXAxis.setShowMajorGrid(true);
		exCntGraph.primaryYAxis.setAutoScale(true);
		exCntGraph.primaryYAxis.setShowMajorGrid(true);
		exCntGraph.primaryXAxis.setTitle("");
		exCntGraph.primaryYAxis.setTitle("");
		exCntGraph.primaryXAxis.setRange(stime, etime);
		
		final FigureCanvas rowsCanvas = new FigureCanvas(composite);
		rowsCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		rowsCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rowsGraph = new XYGraph();
		rowsCanvas.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = rowsCanvas.getClientArea();
				rowsGraph.setSize(r.width, r.height);
			}
		});
		rowsGraph.setShowLegend(true);
		rowsGraph.setShowTitle(true);
		rowsGraph.setTitle("Rows Affected/Sent");
		rowsCanvas.setContents(rowsGraph);
		rowsGraph.primaryXAxis.setDateEnabled(true);
		rowsGraph.primaryXAxis.setShowMajorGrid(true);
		rowsGraph.primaryYAxis.setAutoScale(true);
		rowsGraph.primaryYAxis.setShowMajorGrid(true);
		rowsGraph.primaryXAxis.setTitle("");
		rowsGraph.primaryYAxis.setTitle("");
		rowsGraph.primaryXAxis.setRange(stime, etime);
		
		return composite;
	}
	
	private void loadExampleQuery() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				Pack p = null;
				try {
					MapPack param = new MapPack();
					ListValue objHashLv = new ListValue();
					if (model.objHash == 0) {
						for (DigestModel d : model.getChildArray()) {
							objHashLv.add(d.objHash);
						}
					} else {
						objHashLv.add(model.objHash);
					}
					param.put("objHash", objHashLv);
					param.put("date", date);
					param.put("stime", stime);
					param.put("etime", etime);
					param.put("digest", model.digestHash);
					p = tcp.getSingle(RequestCmd.DB_MAX_TIMER_WAIT_THREAD, param);
				} catch (Exception e) {
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (p != null && ((MapPack)p).size() > 0) {
					final MapPack m = (MapPack) p;
					ExUtil.exec(tabFolder, new Runnable() {
						public void run() {
							Control spinnerControl = exampleQueryItem.getControl();
							exampleQueryItem.setControl(getExampleQuery(tabFolder, m));
							if (spinnerControl != null && spinnerControl.isDisposed() == false) {
								spinnerControl.dispose();
							}
						}
					});
				} else {
					ExUtil.exec(tabFolder, new Runnable() {
						public void run() {
							exampleQueryItem.dispose();
						}
					});
				}
			}
		});
	}
	
	private void loadGraphsData() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				List<Pack> avgList = loadDigestCounter("AVG_TIMER_WAIT");
				List<Pack> minList = loadDigestCounter("MIN_TIMER_WAIT");
				List<Pack> maxList = loadDigestCounter("MAX_TIMER_WAIT");
				
				final Map<Long, Double> avgMap = ScouterUtil.getLoadTotalMap("AVG_TIMER_WAIT", avgList, "avg", TimeTypeEnum.REALTIME);
				final Map<Long, Double> minMap = ScouterUtil.getLoadMinOrMaxMap(minList, "min", TimeTypeEnum.REALTIME);
				final Map<Long, Double> maxMap = ScouterUtil.getLoadMinOrMaxMap(maxList, "max", TimeTypeEnum.REALTIME);
				
				List<Pack> execList = loadDigestCounter("COUNT_STAR");
				execList = changeToDeltaValue(execList);
				final Map<Long, Double> execMap = ScouterUtil.getLoadTotalMap("COUNT_STAR", execList, "sum", TimeTypeEnum.REALTIME);
				
				List<Pack> rowsAffectedList = loadDigestCounter("SUM_ROWS_AFFECTED");
				rowsAffectedList = changeToDeltaValue(rowsAffectedList);
				final Map<Long, Double> rowsAffectedMap = ScouterUtil.getLoadTotalMap("SUM_ROWS_AFFECTED", rowsAffectedList, "avg", TimeTypeEnum.REALTIME);
				
				List<Pack> rowsSentList = loadDigestCounter("SUM_ROWS_SENT");
				rowsSentList = changeToDeltaValue(rowsSentList);
				final Map<Long, Double> rowsSentMap = ScouterUtil.getLoadTotalMap("SUM_ROWS_SENT", rowsSentList, "avg", TimeTypeEnum.REALTIME);
				
				ExUtil.exec(tabFolder, new Runnable() {
					
					public void run() {
						Control spinnerControl = graphsItem.getControl();
						Control control = getGraphsControl(tabFolder);
						makeTrace("Average", exTimeGraph, avgMap, TraceType.SOLID_LINE, ColorUtil.getInstance().getColor(SWT.COLOR_DARK_RED), PICO * 1000);
						makeTrace("Min", exTimeGraph, minMap, TraceType.SOLID_LINE, ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GREEN), PICO * 1000);
						double max = makeTrace("Max", exTimeGraph, maxMap, TraceType.SOLID_LINE, ColorUtil.getInstance().getColor(SWT.COLOR_DARK_BLUE), PICO * 1000);
						exTimeGraph.primaryYAxis.setRange(0, ChartUtil.getMaxValue(max));
						max = makeTrace("Executions", exCntGraph, execMap, TraceType.AREA, ColorUtil.getInstance().getColor(SWT.COLOR_DARK_RED), 1);
						exCntGraph.primaryYAxis.setRange(0, ChartUtil.getMaxValue(max));
						max = makeTrace("Rows Affected", rowsGraph, rowsAffectedMap, TraceType.AREA, ColorUtil.getInstance().getColor(SWT.COLOR_DARK_RED), 1);
						double max2 = makeTrace("Rows Sent", rowsGraph, rowsSentMap, TraceType.AREA, ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GREEN), 1);
						rowsGraph.primaryYAxis.setRange(0, ChartUtil.getMaxValue(Math.max(max, max2)));
						graphsItem.setControl(control);
						if (spinnerControl != null && spinnerControl.isDisposed() == false) {
							spinnerControl.dispose();
						}
					}
					
					private double makeTrace(String name, XYGraph xyGraph, Map<Long, Double> valueMap, TraceType type, Color color, double weight) {
						CircularBufferDataProvider provider = new CircularBufferDataProvider(true);
						provider.setBufferSize(valueMap.size());
						provider.setCurrentXDataArray(new double[] {});
						provider.setCurrentYDataArray(new double[] {});
						final Trace trace = new Trace(name, xyGraph.primaryXAxis, xyGraph.primaryYAxis, provider);
						trace.setPointStyle(PointStyle.NONE);
						trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
						trace.setTraceType(type);
						trace.setTraceColor(color);
						trace.setAreaAlpha(90);
						xyGraph.addTrace(trace);
						Set<Long> timeSet = valueMap.keySet();
						double max = 0;
						for (long time : timeSet) {
							double v = CastUtil.cdouble(valueMap.get(time)) * weight;
							if (v > max) {
								max = v;
							}
							provider.addSample(new Sample(CastUtil.cdouble(time), v));
						}
						return max;
					}
				});
			}
			
			private List<Pack> loadDigestCounter(String column) {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				List<Pack> list = null;
				try {
					MapPack param = new MapPack();
					ListValue objHashLv = new ListValue();
					if (model.objHash == 0) {
						for (DigestModel d : model.getChildArray()) {
							objHashLv.add(d.objHash);
						}
					} else {
						objHashLv.add(model.objHash);
					}
					param.put("objHash", objHashLv);
					param.put("date", date);
					param.put("stime", stime);
					param.put("etime", etime);
					param.put("digest", model.digestHash);
					param.put("column", column);
					list = tcp.process(RequestCmd.DB_LOAD_DIGEST_COUNTER, param);
				} catch (Exception e) {
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				return list;
			}
			
			private List<Pack> changeToDeltaValue(List<Pack> packList) {
				List<Pack> newList = new ArrayList<Pack>();
				for (Pack p : packList) {
					MapPack m = (MapPack) p;
					ListValue timeLv = m.getList("time");
					ListValue valueLv = m.getList("value");
					ListValue newValueLv = new ListValue();
					long beforeTime = 0L;
					double beforeValue = 0L;
					for (int i = 0; i < timeLv.size(); i++) {
						long time = timeLv.getLong(i);
						double value = valueLv.getDouble(i);
						double newValue = 0L;
						if (i > 0) {
							newValue = (value - beforeValue) / ((time - beforeTime) / 1000.0d);
						}
						newValueLv.add(newValue);
						beforeTime = time;
						beforeValue = value;
					}
					m.put("value", newValueLv);
					newList.add(m);
				}
				return newList;
			}
			
		});
	}
}
