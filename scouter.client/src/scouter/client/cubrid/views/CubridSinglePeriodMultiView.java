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
import java.util.Locale;
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
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

public class CubridSinglePeriodMultiView extends ViewPart implements Refreshable {

	public static final String ID = CubridSinglePeriodMultiView.class.getName();

	int serverId;
	CubridSingleItem viewType;

	RefreshThread thread;

	long TIME_RANGE = DateUtil.MILLIS_PER_TEN_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 5);
	static long CHECK_ACTIVE_REALTIME = DateUtil.MILLIS_PER_TEN_MINUTE;

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

	String date;
	long stime, etime;

	long pastStime, pastEtime;

	boolean manulRefresh = true;

	Label serverText, sDateText, sTimeText, eTimeText;
	Composite headerComp;
	CalendarDialog calDialog;
	Button applyBtn;

	int prvActiveDBHash;

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		serverId = CastUtil.cint(ids[0]);
		selectionDB = CastUtil.cString(ids[1]);
		int ordinal = CastUtil.cint(ids[2]);
		viewType = CubridSingleItem.values()[ordinal];
		pastStime = CastUtil.cLong(ids[3]);
		pastEtime = CastUtil.cLong(ids[4]);

		TIME_RANGE = pastEtime - pastStime;
		
		if (selectionDB == "default") {
			isDefaultView = true;
		}
		
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		this.setPartName("ShortPeriod - " + viewType.getTitle() + "[" + server.getName() + "]");

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
						} else {
							if (isDefaultView) {
								dbListCombo.setEnabled(true);
							}
							dbListCombo.removeAll();
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
				selectionDB = dbListCombo.getItem(dbListCombo.getSelectionIndex());
				applyBtn.setEnabled(true);
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
		
		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
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
				manulRefresh = true;
				thread.interrupt();
			}
		});

		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
	}

	private void createUpperMenu(Composite composite) {
		headerComp = new Composite(composite, SWT.NONE);
		headerComp.setLayoutData(new GridData(SWT.RIGHT));
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
						manulRefresh = true;
						thread.interrupt();
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

					calDialog = new CalendarDialog(display, new ILoadCalendarDialog() {
						@Override
						public void onPressedOk(String date) {
						}

						@Override
						public void onPressedOk(long startTime, long endTime) {
							ExUtil.syncExec(new Runnable() {
								public void run() {
									pastStime = startTime;
									pastEtime = endTime;
									applyBtn.setEnabled(true);
									TIME_RANGE = pastEtime - pastStime;
									eTimeText.setText(DateUtil.format(pastEtime, "hh:mm a", Locale.ENGLISH));
									sTimeText.setText(DateUtil.format(pastStime, "hh:mm a", Locale.ENGLISH));
									sDateText.setText(DateUtil.format(pastStime, "yyyy-MM-dd"));
								}
							});

						}

						@Override
						public void onPressedCancel() {
						}
					});
					int hourRange = DateUtil.getHour(TimeUtil.getCurrentTime(serverId));
					int MiniteRange = DateUtil.getMin(TimeUtil.getCurrentTime(serverId));
					if (hourRange > 4) {
						calDialog.showWithEndTime(UIUtil.getMousePosition(),
								TimeUtil.getCurrentTime(serverId) - DateUtil.MILLIS_PER_HOUR * 4,
								TimeUtil.getCurrentTime(serverId));
					} else {
						calDialog.showWithEndTime(UIUtil.getMousePosition(),
								TimeUtil.getCurrentTime(serverId) - DateUtil.MILLIS_PER_HOUR * hourRange - DateUtil.MILLIS_PER_MINUTE * MiniteRange,
								TimeUtil.getCurrentTime(serverId));
					}
					break;
				}
			}
		});

		eTimeText = new Label(headerComp, SWT.NONE);
		eTimeText.setLayoutData(UIUtil.labelFormData(manualBtn));
		eTimeText.setText(DateUtil.format(pastEtime, "hh:mm a", Locale.ENGLISH));

		Label label = new Label(headerComp, SWT.NONE);
		label.setLayoutData(UIUtil.labelFormData(eTimeText));
		label.setText("~");

		sTimeText = new Label(headerComp, SWT.NONE);
		sTimeText.setLayoutData(UIUtil.labelFormData(label));
		sTimeText.setText(DateUtil.format(pastStime, "hh:mm a", Locale.ENGLISH));

		sDateText = new Label(headerComp, SWT.NONE);
		sDateText.setLayoutData(UIUtil.labelFormData(sTimeText));
		sDateText.setText(DateUtil.format(pastStime, "yyyy-MM-dd"));
	}

	public void setFocus() {

	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}

	public void refresh() {
		if (ActiveDbInfo.getInstance() == null) {
			return;
		}
		
		if (viewType.getInfoType() == InfoType.BROKER_INFO) {
			if (!ActiveDbInfo.getInstance().getDbList(serverId).isEmpty()) {
				selectionDB = ActiveDbInfo.getInstance().getDbList(serverId).get(0);
			} else {
				return;
			}
		} else if (selectionDB.equals("default")) {
			checkDBList();
			return;
		} else {
			checkDBList();
		}

		if (manulRefresh) {
			manulRefresh = false;
			if (TimeUtil.getCurrentTime(serverId) - pastEtime <= CHECK_ACTIVE_REALTIME) {
				pastEtime = TimeUtil.getCurrentTime(serverId);
				pastLoad();
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						eTimeText.setText("RealTime");
					}
				});
			} else {
				pastLoad();
				return;
			}
		} else {
			if (TimeUtil.getCurrentTime(serverId) - pastEtime > CHECK_ACTIVE_REALTIME) {
				return;
			}
		}

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Value v = null;

		try {
			MapPack param = new MapPack();
			ListValue objHashLv = new ListValue();
			objHashLv.add(ActiveDbInfo.getInstance().getObjectHash(serverId, selectionDB));
			param.put("objHash", objHashLv);
			param.put("counter", viewType.getCounterName());
			v = tcp.getSingleValue(RequestCmd.CUBRID_DB_REALTIME_MULTI_DATA, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (v == null) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.inactive);
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);
				}
			});
		} else {
			MapValue value = (MapValue) v;
			final long data = value.getLong(viewType.getCounterName());
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.active);
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - TIME_RANGE;
					xyGraph.primaryXAxis.setRange(stime, now + 1);

					getDataProvider(serverId).addSample(new Sample(now, data));
					double max = ChartUtil.getMax(getDataProvider(serverId).iterator());
					if (viewType.isPercent()) {
						xyGraph.primaryYAxis.setRange(0, 100);
					} else {
						xyGraph.primaryYAxis.setRange(0, max);
					}
				}
			});
		}
	}

	private CircularBufferDataProvider getDataProvider(int serverId) {
		CircularBufferDataProvider data = datas.get(serverId);
		if (data == null) {
			data = new CircularBufferDataProvider(true);
			datas.put(serverId, data);
			int bufferSize;
			bufferSize = (int) (TIME_RANGE / REFRESH_INTERVAL) + 1;
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
		
		if (ActiveDbInfo.getInstance().getActiveDBInfo(serverId).hashCode() == prvActiveDBHash) {
			return;
		}

		prvActiveDBHash = ActiveDbInfo.getInstance().getActiveDBInfo(serverId).hashCode();

		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				dbListCombo.removeAll();
			}
		});

		if (!ActiveDbInfo.getInstance().isEmpty(serverId)) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					for (String dbName : ActiveDbInfo.getInstance().keySet(serverId)) {
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

	private void pastLoad() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		final ArrayList<MapPack> values = new ArrayList<MapPack>();

		try {
			MapPack param = new MapPack();
			ListValue objHashLv = new ListValue();
			objHashLv.add(ActiveDbInfo.getInstance().getObjectHash(serverId, selectionDB));
			param.put("objHash", objHashLv);
			param.put("counter", viewType.getCounterName());
			param.put("stime", pastStime);
			param.put("etime", pastEtime);
			param.put("objName", ActiveDbInfo.getInstance().getObjectName(serverId, selectionDB));

			tcp.process(RequestCmd.CUBRID_DB_PERIOD_MULTI_DATA, param, new INetReader() {
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
						long y = Long.parseLong(value.getString(i));
						long prevY = 0;
						if (i != 0) {
							prevY = Long.parseLong(value.getString(i - 1));
						}

						if (y < 0 || prevY < 0) { //temp code : Read issue WorkAround
							//System.out.println("CUBRID_DB_LONG_PERIOD_MULTI_DATA skip data y : " + y + " prevY : " + prevY);
						} else {
							provider.addSample(new Sample(x, y));
						}
					}
					double max = ChartUtil.getMax(provider.iterator());
					if (viewType.isPercent()) {
						xyGraph.primaryYAxis.setRange(0, 100);
					} else {
						xyGraph.primaryYAxis.setRange(0, max);
					}
				}
			}
		});

		return;
	}

	private void modifyData() {
		redraw();
		getDataProvider(serverId).clearTrace();
		Trace trace = traces.get(0);
		trace.setName(viewType.getTitle());
		trace.setTraceColor(ColorUtil.getInstance().getColor(viewType.getColor()));
		int bufferSize;
		bufferSize = (int) (TIME_RANGE / REFRESH_INTERVAL) + 1;
		getDataProvider(serverId).setBufferSize(bufferSize);
	}
}
