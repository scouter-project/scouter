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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.cubrid.ActiveDbInfo;
import scouter.client.cubrid.CubridMenuUtil;
import scouter.client.cubrid.CubridSingleItem;
import scouter.client.cubrid.CubridTypeShotPeriod;
import scouter.client.cubrid.CubridSingleItem.InfoType;
import scouter.client.cubrid.actions.AlertSettingDialog;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.TcpProxy;
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
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

public class CubridSingleRealTimeMultiView extends ViewPart implements Refreshable {

	public static final String ID = CubridSingleRealTimeMultiView.class.getName();

	int serverId;
	int objhash;
	CubridSingleItem viewType;

	RefreshThread thread;

	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 5);

	FigureCanvas canvas;
	XYGraph xyGraph;

	Combo dbListCombo;
	Combo dbCounterCombo;
	Combo timeRangeCombo;

	Menu contextMenu;
	String selectionDB;
	boolean isDefaultView = false;

	Trace nearestTrace;
	ArrayList<Trace> traces = new ArrayList<>();
	protected Map<Integer, CircularBufferDataProvider> datas = new HashMap<Integer, CircularBufferDataProvider>();

	String date;
	long stime, etime;
	long timeRange;
	long pastStime, pastEtime;

	int prvActiveDBHash;

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		serverId = CastUtil.cint(ids[0]);
		selectionDB = CastUtil.cString(ids[1]);
		int ordinal = CastUtil.cint(ids[2]);
		timeRange = CastUtil.clong(ids[3]);
		viewType = CubridSingleItem.values()[ordinal];
		
		if (selectionDB.equals("default")) {
			isDefaultView = true;
		}
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		this.setPartName("SingleRealTimeMultiView " + viewType.getTitle() + " [" + server.getName() + "]");
		IWorkbenchWindow window = getSite().getWorkbenchWindow();
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Warning Alert Setting", ImageUtil.getImageDescriptor(Images.preference)) {
			public void run() {
				AlertSettingDialog dialog = new AlertSettingDialog(
						window.getShell().getDisplay(), serverId,
						dbCounterCombo.getSelectionIndex());
				dialog.show();
			}
		});
		
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		dbCounterCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		dbCounterCombo.setLayoutData(new GridData(SWT.FILL));
		
		if (!isDefaultView) {
			dbCounterCombo.setEnabled(false);
		}
		
		for (CubridSingleItem counterName : CubridSingleItem.values()) {
			dbCounterCombo.add(counterName.getTitle());
			if (counterName.ordinal() == viewType.ordinal()) {
				dbCounterCombo.select(viewType.ordinal());
			}
		}
		
		dbCounterCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ExUtil.exec(canvas, new Runnable() {
					@Override
					public void run() {
						viewType = CubridSingleItem.values()[dbCounterCombo.getSelectionIndex()];
						modifyData(true);
						if (viewType.getInfoType() == InfoType.BROKER_INFO) {
							dbListCombo.setEnabled(false);
							dbListCombo.removeAll();
							dbListCombo.add(InfoType.BROKER_INFO.getTitle());
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
		dbListCombo.setLayoutData(new GridData(SWT.RIGHT));

		if (!isDefaultView) {
			dbListCombo.add(selectionDB);
			dbListCombo.select(0);
			dbListCombo.setEnabled(false);
		}
		
		if (viewType.getInfoType() == InfoType.BROKER_INFO) {
			dbListCombo.setEnabled(false);
			dbListCombo.removeAll();
			dbListCombo.add("BROKER_INFO");
			dbListCombo.select(0);
		} 

		dbListCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ExUtil.exec(canvas, new Runnable() {
					@Override
					public void run() {
						selectionDB = dbListCombo.getText();
						modifyData(true);
						thread.interrupt();
					}
				});

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		timeRangeCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		ArrayList<String> minuteStrList = new ArrayList<String>();
		for (CubridTypeShotPeriod minute : CubridTypeShotPeriod.values()) {
			minuteStrList.add(minute.getLabel());
		}
		timeRangeCombo.setItems(minuteStrList.toArray(new String[CubridTypeShotPeriod.values().length]));
		CubridTypeShotPeriod m = CubridTypeShotPeriod.fromTime(timeRange);
		timeRangeCombo.select(m.ordinal());
		timeRangeCombo.setLayoutData(new GridData(SWT.LEFT));
		timeRangeCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				timeRange = CubridTypeShotPeriod.values()[timeRangeCombo.getSelectionIndex()].getTime();
				modifyData(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		parent.setLayout(layout);
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		canvas = new FigureCanvas(parent);
		GridData gdataXyGraph = new GridData(GridData.FILL_BOTH);
		gdataXyGraph.horizontalSpan = 3;
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

		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
		if (viewType.isPercent()) {
			xyGraph.primaryYAxis.setRange(0, 100);
		}
		
		xyGraph.primaryYAxis.setFormatPattern("#,##0");

		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");

		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));

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

		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
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

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Value v = null;

		try {
			MapPack param = new MapPack();
			ListValue objHashLv = new ListValue();
			objHashLv.add(ActiveDbInfo.getInstance().getObjectHash(selectionDB));
			param.put("objHash", objHashLv);
			param.put("counter", viewType.getCounterName());
			v = tcp.getSingleValue(RequestCmd.CUBRID_DB_REALTIME_MULTI_DATA, param);
		} catch (Exception e) {
			e.printStackTrace();
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (v == null) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.inactive);
					long now = TimeUtil.getCurrentTime(serverId);
					long stime = now - timeRange;
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
					long stime = now - timeRange;
					xyGraph.primaryXAxis.setRange(stime, now + 1);

					getDataProvider(serverId).addSample(new Sample(now, data));
					if (viewType.isPercent()) {
						xyGraph.primaryYAxis.setRange(0, 100);
					} else {
						double max = ChartUtil.getMax(getDataProvider(serverId).iterator());
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
			bufferSize = (int) (timeRange / REFRESH_INTERVAL) + 1;
			data.setBufferSize(bufferSize);
			data.setCurrentXDataArray(new double[] {});
			data.setCurrentYDataArray(new double[] {});

			final Trace trace = new Trace(viewType.getTitle(), xyGraph.primaryXAxis, xyGraph.primaryYAxis, data);
			trace.setPointStyle(PointStyle.NONE);
			trace.setAreaAlpha(90);
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

	private void modifyData(boolean clean) {
		redraw();
		if (clean) {
			getDataProvider(serverId).clearTrace();
		}
		Trace trace = traces.get(0);
		trace.setName(viewType.getTitle());
		trace.setTraceType(viewType.getTraceType());
		trace.setTraceColor(ColorUtil.getInstance().getColor(viewType.getColor()));
		int bufferSize;
		bufferSize = (int) (timeRange / REFRESH_INTERVAL) + 1;
		getDataProvider(serverId).setBufferSize(bufferSize);
	}

}
