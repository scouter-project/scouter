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
package scouter.client.cubrid.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.cubrid.ActiveDbInfo;
import scouter.client.cubrid.CubridMenuUtil;
import scouter.client.cubrid.CubridSingleItem;
import scouter.client.cubrid.CubridSingleItem.InfoType;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.popup.DualCalendarDialog;
import scouter.client.popup.DualCalendarDialog.ILoadDualCounterDialog;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

public class CubridSingleDailyPeriodMultiView extends ViewPart implements Refreshable {

	public static final String ID = CubridSingleDailyPeriodMultiView.class.getName();

	int serverId;
	CubridSingleItem viewType;

	long TIME_RANGE;

	FigureCanvas canvas;
	XYGraph xyGraph;

	Combo dbListCombo;
	Combo dbCounterCombo;
	Menu contextMenu;
	String selectionDB;
	boolean isDefaultView = false;

	Trace nearestTrace;
	ArrayList<Trace> traces = new ArrayList<>();
	protected Map<Integer, CircularBufferDataProvider> datas = new HashMap<Integer, CircularBufferDataProvider>();

	long pastStime, pastEtime;
	String pastSdate, pastEdate;

	RefreshThread thread;

	Label serverText, sDateText, eDateText;
	DualCalendarDialog calDialog;
	Combo periodCombo;
	Composite headerComp;
	Button applyBtn;

	int prvActiveDBHash;

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		serverId = CastUtil.cint(ids[0]);
		selectionDB = CastUtil.cString(ids[1]);
		int ordinal = CastUtil.cint(ids[2]);
		pastSdate = CastUtil.cString(ids[3]);
		pastEdate = CastUtil.cString(ids[4]);

		viewType = CubridSingleItem.values()[ordinal];
		pastStime = DateUtil.getTime(pastSdate, "yyyyMMdd");
		pastEtime = DateUtil.getTime(pastEdate, "yyyyMMdd") + DateUtil.MILLIS_PER_DAY;
		TIME_RANGE = pastEtime - pastStime;
		
		if (selectionDB.equals("default")) {
			isDefaultView = true;
		}

	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		this.setPartName("Daily Period - " + viewType.getTitle() + " [" + server.getName() + "]");

		// FormLayout layout = new FormLayout();
		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = 5;
		layout.marginWidth = 5;

		dbCounterCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		dbCounterCombo.setLayoutData(new GridData(SWT.LEFT | SWT.FILL));
		for (CubridSingleItem counterName : CubridSingleItem.values()) {
			dbCounterCombo.add(counterName.getTitle());
			if (counterName.ordinal() == viewType.ordinal()) {
				dbCounterCombo.select(viewType.ordinal());
			}
		}
		
		if (!isDefaultView) {
			dbCounterCombo.setEnabled(false);
		}

		dbCounterCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ExUtil.syncExec(new Runnable() {
					public void run() {
						viewType = CubridSingleItem.values()[dbCounterCombo.getSelectionIndex()];
						applyBtn.setEnabled(true);
						if (viewType.getInfoType() == InfoType.BROKER_INFO) {
							dbListCombo.setEnabled(false);
							dbListCombo.removeAll();
							dbListCombo.add("BROKER_INFO");
							dbListCombo.select(0);
							if (ActiveDbInfo.getInstance().getDbList().isEmpty()) {
								selectionDB = ActiveDbInfo.getInstance().getDbList().get(0);
							}
						} else {
							dbListCombo.removeAll();
							if (isDefaultView) {
								dbListCombo.setEnabled(true);
							}
							prvActiveDBHash = -1;
							checkDBList();
						}
					}
				});
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		dbListCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		dbListCombo.setLayoutData(new GridData(SWT.LEFT | SWT.FILL));

		if (!isDefaultView) {
			dbListCombo.add(selectionDB);
			dbListCombo.select(0);
			dbListCombo.setEnabled(false);
		}
		
		if (viewType.getInfoType() == InfoType.BROKER_INFO) {
			dbListCombo.setEnabled(false);
			dbListCombo.removeAll();
			dbListCombo.add(InfoType.BROKER_INFO.getTitle());
			dbListCombo.select(0);
		} 
		
		dbListCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ExUtil.syncExec(new Runnable() {
					public void run() {
						applyBtn.setEnabled(true);
						selectionDB = dbListCombo.getText();
					}
				});
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		Button dbRefresh = new Button(parent, SWT.PUSH);
		dbRefresh.setLayoutData(new GridData(SWT.LEFT));
		dbRefresh.setImage(Images.refresh);
		dbRefresh.setToolTipText("refresh DB List");
		dbRefresh.setVisible(false);
		dbRefresh.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (viewType.getInfoType() != InfoType.BROKER_INFO) {
					checkDBList();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
		GridData gdata = new GridData();
		gdata.horizontalAlignment = GridData.END;
		composite.setLayoutData(gdata);
		createUpperMenu(composite);

		parent.setLayout(layout);
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		canvas = new FigureCanvas(parent);
		GridData gdataXyGraph = new GridData(GridData.FILL_BOTH);
		gdataXyGraph.horizontalSpan = 4;
		canvas.setLayoutData(gdataXyGraph);
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent arg0) {
			}

			public void controlResized(ControlEvent arg0) {
				Rectangle r = canvas.getClientArea();
				xyGraph.setSize(r.width, r.height);
			}
		});

		xyGraph = new XYGraph();
		xyGraph.setShowLegend(true);
		xyGraph.setShowTitle(false);
		canvas.setContents(xyGraph);

		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);
		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);

		if (viewType.isPercent()) {
			xyGraph.primaryYAxis.setRange(0, 100);
		}
		
		xyGraph.primaryXAxis.setFormatPattern("yyyy-MM-dd\n  HH:mm:ss");
		xyGraph.primaryYAxis.setFormatPattern("#,##0");

		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");

		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));

		// CircularBufferDataProvider provider = new
		// CircularBufferDataProvider(true);
		// int bufferSize = (int)(TIME_RANGE / REFRESH_INTERVAL) + 1;
		// System.out.println("bufferSize : " + bufferSize);
		// provider.setBufferSize(bufferSize);
		// provider.setCurrentXDataArray(new double[] {});
		// provider.setCurrentYDataArray(new double[] {});
		// final Trace trace = new Trace("", xyGraph.primaryXAxis,
		// xyGraph.primaryYAxis, provider);
		// trace.setName(viewType.getTitle());
		// trace.setPointStyle(PointStyle.NONE);
		// trace.setTraceType(TraceType.SOLID_LINE);
		// trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		// trace.setAreaAlpha(255);
		// trace.setTraceColor(ColorUtil.getInstance().getColor(viewType.getColor()));
		// xyGraph.addTrace(trace);
		// traces.add(trace);

		final DefaultToolTip toolTip = new DefaultToolTip(canvas, DefaultToolTip.RECREATE, true);
		toolTip.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		toolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		canvas.addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(MouseEvent arg0) {

			}
		});

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

				for (Trace t : traces) {
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
					toolTip.setText(nearestTrace.getName() + "\nTime : " + DateUtil.format(time, "HH:mm:ss")
							+ "\nValue : " + FormatUtil.print(value, "#,###.##"));
					toolTip.show(new Point(e.x, e.y));
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		CubridMenuUtil.createAddViewContextMenu(getSite().getWorkbenchWindow(), serverId, canvas);

		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("refresh", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				ExUtil.asyncRun(new Runnable() {
					public void run() {
						longPastLoad();
					}
				});
			}
		});

		thread = new RefreshThread(this, (int) DateUtil.MILLIS_PER_SECOND);
		thread.start();
	}

	private void createUpperMenu(Composite composite) {
		headerComp = new Composite(composite, SWT.NONE);
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerComp.setLayout(UIUtil.formLayout(0, 0));

		applyBtn = new Button(headerComp, SWT.PUSH);
		applyBtn.setLayoutData(UIUtil.formData(null, -1, 0, 2, 100, -5, null, -1));
		applyBtn.setText("Apply");
		applyBtn.setEnabled(false);
		applyBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					((Button) event.widget).setEnabled(false);
					try {
						modifyData();
						longPastLoad();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}
		});

		Button manualBtn = new Button(headerComp, SWT.PUSH);
		manualBtn.setImage(Images.CTXMENU_RDC);
		manualBtn.setText("Manual");
		manualBtn.setLayoutData(UIUtil.formData(null, -1, 0, 2, applyBtn, -5, null, -1));
		manualBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					Display display = Display.getCurrent();
					if (display == null) {
						display = Display.getDefault();
					}

					calDialog = new DualCalendarDialog(display, new ILoadDualCounterDialog() {

						@Override
						public void onPressedOk(String sDate, String eDate) {
							ExUtil.syncExec(new Runnable() {

								@Override
								public void run() {
									applyBtn.setEnabled(true);
									pastSdate = sDate;
									pastEdate = eDate;
									pastStime = DateUtil.getTime(pastSdate, "yyyyMMdd");
									pastEtime = DateUtil.getTime(pastEdate, "yyyyMMdd") + DateUtil.MILLIS_PER_DAY;
									TIME_RANGE = pastEtime - pastStime;
									sDateText.setText(DateUtil.format(pastStime, "yyyy-MM-dd"));
									eDateText.setText(DateUtil.format(pastEtime, "yyyy-MM-dd"));
								}
							});

						}

						@Override
						public void onPressedOk(long startTime, long endTime) {
						}

						@Override
						public void onPressedCancel() {
						}
					});
					calDialog.show(UIUtil.getMousePosition());

					break;
				}
			}
		});

		periodCombo = new Combo(headerComp, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		periodCombo.setLayoutData(UIUtil.formData(null, -1, 0, 3, manualBtn, -5, null, -1));
		ArrayList<String> periodStrList = new ArrayList<String>();
		for (DatePeriodUnit minute : DatePeriodUnit.values()) {
			periodStrList.add(minute.getLabel());
		}
		periodCombo.setItems(periodStrList.toArray(new String[DatePeriodUnit.values().length]));
		periodCombo.select(2);
		periodCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				applyBtn.setEnabled(true);
				if (((Combo) e.widget).getSelectionIndex() == 0) {
					setStartEndDate(30);
				} else if (((Combo) e.widget).getSelectionIndex() == 1) {
					setStartEndDate(7);
				} else {
					setStartEndDate(1);
				}
			}

			private void setStartEndDate(int i) {
				long yesterday = TimeUtil.getCurrentTime(serverId) - DatePeriodUnit.A_DAY.getTime();
				long startDate = TimeUtil.getCurrentTime(serverId) - (DatePeriodUnit.A_DAY.getTime() * i);
				sDateText.setText(DateUtil.format(startDate, "yyyy-MM-dd"));
				eDateText.setText(DateUtil.format(yesterday, "yyyy-MM-dd"));

				pastSdate = DateUtil.format(startDate, "yyyyMMdd");
				pastEdate = DateUtil.format(yesterday, "yyyyMMdd");

				pastStime = DateUtil.getTime(pastSdate, "yyyyMMdd");
				pastEtime = DateUtil.getTime(pastEdate, "yyyyMMdd") + DateUtil.MILLIS_PER_DAY;
			}

		});

		eDateText = new Label(headerComp, SWT.NONE);
		eDateText.setLayoutData(UIUtil.formData(null, -1, 0, 7, periodCombo, -5, null, -1));
		eDateText.setText(DateUtil.format(pastEtime, "yyyy-MM-dd"));

		Label windbarLabel = new Label(headerComp, SWT.NONE);
		windbarLabel.setLayoutData(UIUtil.formData(null, -1, 0, 7, eDateText, -5, null, -1));
		windbarLabel.setText("~");

		sDateText = new Label(headerComp, SWT.NONE);
		sDateText.setLayoutData(UIUtil.formData(null, -1, 0, 7, windbarLabel, -5, null, -1));
		sDateText.setText(DateUtil.format(pastStime, "yyyy-MM-dd"));
	}

	public void setFocus() {

	}

	@Override
	public void dispose() {
		super.dispose();
	}

	private CircularBufferDataProvider getDataProvider(int serverId) {
		CircularBufferDataProvider data = datas.get(serverId);
		if (data == null) {
			data = new CircularBufferDataProvider(true);
			datas.put(serverId, data);
			int bufferSize;
			bufferSize = (int) (TIME_RANGE / (int) DateUtil.MILLIS_PER_FIVE_MINUTE) + 1;
			data.setBufferSize(bufferSize);
			data.setCurrentXDataArray(new double[] {});
			data.setCurrentYDataArray(new double[] {});
			final Trace trace = new Trace(viewType.getTitle(), xyGraph.primaryXAxis, xyGraph.primaryYAxis, data);
			trace.setPointStyle(PointStyle.NONE);
			trace.setAreaAlpha(255);
			trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace.setTraceType(viewType.getTraceType());
			trace.setTraceColor(ColorUtil.getInstance().getColor(viewType.getColor()));

			xyGraph.addTrace(trace);
			traces.add(trace);
		}
		return data;
	}

	public void checkDBList() {
		
		if (!isDefaultView) {
			return;
		}
		
		if (ActiveDbInfo.getInstance().getActiveDBInfo().hashCode() == prvActiveDBHash) {
			return;
		}

		prvActiveDBHash = ActiveDbInfo.getInstance().getActiveDBInfo().hashCode();

		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				dbListCombo.removeAll();
			}
		});

		if (!ActiveDbInfo.getInstance().isEmpty()) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					for (String dbName : ActiveDbInfo.getInstance().keySet()) {
						dbListCombo.add(dbName);
					}
					dbListCombo.setEnabled(true);
				}
			});

		} else {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					dbListCombo.setEnabled(false);
				}
			});
		}

		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				for (int i = 0; i < dbListCombo.getItemCount(); i++) {
					if (dbListCombo.getItem(i).equals(selectionDB)) {
						dbListCombo.select(i);
						return;
					}
				}

				if (dbListCombo.getItemCount() != 0) {
					dbListCombo.select(0);
					selectionDB = dbListCombo.getItem(dbListCombo.getSelectionIndex());
				}
			}
		});
	}

	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}

	private void longPastLoad() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		final ArrayList<MapPack> values = new ArrayList<MapPack>();

		if (ActiveDbInfo.getInstance() == null) {
			return;
		}

		if (viewType.getInfoType() == InfoType.BROKER_INFO) {
			if (!ActiveDbInfo.getInstance().getDbList().isEmpty()) {
				selectionDB = ActiveDbInfo.getInstance().getDbList().get(0);
			} else {
				return;
			}
		} else if (selectionDB.equals("default")) {
			checkDBList();
			return;
		} else {
			checkDBList();
		}
		
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				selectionDB = dbListCombo.getText();
				if (selectionDB == "") {
					return;
				}
			}
		});

		try {
			MapPack param = new MapPack();
			ListValue objHashLv = new ListValue();
			objHashLv.add(ActiveDbInfo.getInstance().getObjectHash(selectionDB));
			param.put("objHash", objHashLv);
			param.put("counter", viewType.getCounterName());
			param.put("sDate", pastSdate);
			param.put("eDate", pastEdate);

			tcp.process(RequestCmd.CUBRID_DB_LONG_PERIOD_MULTI_DATA, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					MapPack mpack = (MapPack) in.readPack();
					values.add(mpack);
				};
			});

		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}

		ExUtil.exec(this.canvas, new Runnable() {
			public void run() {
				xyGraph.primaryXAxis.setRange(pastStime, pastEtime);

				for (MapPack mpack : values) {
					//int objHash = mpack.getInt("objHash");
					ListValue time = mpack.getList("time");
					ListValue value = mpack.getList("value");
					if (time == null || time.size() < 1) {
						continue;
					}

					CircularBufferDataProvider provider = getDataProvider(serverId);
					provider.clearTrace();

					for (int i = 0; time != null && i < time.size(); i++) {
						setTitleImage(Images.active);
						long x = time.getLong(i);
						if (value.getString(i) != null) {
							long y = Long.parseLong(value.getString(i));
							long prevY = 0;
							if (i != 0) {
								if (value.getString(i - 1) != null) {
									prevY = Long.parseLong(value.getString(i - 1));
								}
							}

							if (y < 0 || prevY < 0) { // temp code : Read issue //WorkAround
								 //System.out.println("CUBRID_DB_LONG_PERIOD_MULTI_DATA skip data y : " + y + " prevY : " + prevY);
							} else {
								provider.addSample(new Sample(x, y));
							}
						}
					}
					
					if (viewType.isPercent()) {
						xyGraph.primaryYAxis.setRange(0, 100);
					} else {
						double max = ChartUtil.getMax(provider.iterator());
						xyGraph.primaryYAxis.setRange(0, max);
					}
				}
			}
		});

		return;
	}

	@Override
	public void refresh() {
		if (ActiveDbInfo.getInstance() == null) {
			return;
		}

		if (viewType.getInfoType() == InfoType.BROKER_INFO) {
			if (!ActiveDbInfo.getInstance().getDbList().isEmpty()) {
				selectionDB = ActiveDbInfo.getInstance().getDbList().get(0);
			} else {
				return;
			}
		} else if (selectionDB.equals("default")) {
			checkDBList();
			return;
		} else {
			checkDBList();
		}

		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				if (dbListCombo != null && dbListCombo.getItemCount() != 0 && dbListCombo.getSelection() != null) {
					selectionDB = dbListCombo.getText();
					if (selectionDB == "") {
						return;
					}
				}
			}
		});

		if (ActiveDbInfo.getInstance().isEmpty()) {
			return;
		}

		longPastLoad();

		this.thread.shutdown();
	}

	public enum DatePeriodUnit {
		A_MONTH ("1 Month", 30 * 24 * 60 * 60 * 1000),
		A_WEEK ("1 Week",  7 * 24 * 60 * 60 * 1000),
		A_DAY ("1 Day", 24 * 60 * 60 * 1000);

		private String label;
		private long time;

		private DatePeriodUnit(String label, long time) {
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

	private void modifyData() {
		redraw();
		getDataProvider(serverId).clearTrace();
		Trace trace = traces.get(0);
		trace.setName(viewType.getTitle());
		trace.setTraceColor(ColorUtil.getInstance().getColor(viewType.getColor()));
		int bufferSize;
		bufferSize = (int) (TIME_RANGE / (int) DateUtil.MILLIS_PER_FIVE_MINUTE) + 1;
		getDataProvider(serverId).setBufferSize(bufferSize);
	}
}

