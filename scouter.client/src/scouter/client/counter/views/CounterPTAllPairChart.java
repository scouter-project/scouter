package scouter.client.counter.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
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
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.counter.actions.OpenPTPairAllAction;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.AgentColorManager;
import scouter.client.model.TextProxy;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.views.ScouterViewPart;
import scouter.io.DataInputX;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

public class CounterPTAllPairChart extends ScouterViewPart implements IObjectCheckListener {
	
	public final static String ID = CounterPTAllPairChart.class.getName();

	private int serverId;
	private String objType;
	private String counter;
	private long stime;
	private long etime;
	
	protected XYGraph xyGraph;
	protected Map<Integer, TracePair> dataMap = new HashMap<Integer, TracePair>();
	TracePair nearestTracePair;
	protected FigureCanvas canvas;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
		this.counter = ids[2];
	}
	
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
						CounterPTAllPairChart.this.setContentDescription(desc);
					} else {
						CounterPTAllPairChart.this.setContentDescription("");
					}
					r = canvas.getClientArea();
					xyGraph.setSize(r.width, r.height);
					lock = false;
				}
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
		
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
		xyGraph.primaryYAxis.setFormatPattern("#,##0");
		
		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));
		
		final DefaultToolTip toolTip = new DefaultToolTip(canvas, DefaultToolTip.RECREATE, true);
		toolTip.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		toolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		
		canvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				if (nearestTracePair != null) {
					int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
					nearestTracePair.setLineWidth(width);
					nearestTracePair = null;
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
				double max = 0;
				double value = 0;
				Iterator<Integer> keys = dataMap.keySet().iterator();
				while (keys.hasNext()) {
					int objHash = keys.next();
					TracePair tp = dataMap.get(objHash);
					Trace t1 = tp.t1;
					ISample s1 = ScouterUtil.getNearestPoint(t1.getDataProvider(), x);
					Trace t2 = tp.t2;
					ISample s2 = ScouterUtil.getNearestPoint(t2.getDataProvider(), x);
					if (s1 != null && s2 != null) {
						int x1 = xyGraph.primaryXAxis.getValuePosition(s1.getXValue(), false);
						int y1 = xyGraph.primaryYAxis.getValuePosition(s1.getYValue(), false);
						int x2 = xyGraph.primaryXAxis.getValuePosition(s2.getXValue(), false);
						int y2 = xyGraph.primaryYAxis.getValuePosition(s2.getYValue(), false);
						double distance1 = ScouterUtil.getPointDistance(e.x, e.y, x1, y1);
						double distance2 = ScouterUtil.getPointDistance(e.x, e.y, x2, y2);
						double distance = distance1 > distance2 ? distance2 : distance1;
						if (minDistance > distance) {
							minDistance = distance;
							nearestTracePair = tp;
							time = (long) s1.getXValue();
							max = s1.getYValue();
							value = s2.getYValue();
						}
					}
					
				}
				if (nearestTracePair != null) {
					int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
					nearestTracePair.setLineWidth(width + 2);
					toolTip.setText(TextProxy.object.getText(nearestTracePair.objHash)
							+ "\nTime : " + DateUtil.format(time, "HH:mm:ss")
							+ "\nMax : " + FormatUtil.print(max, "#,###.##")
							+ "\nValue : " +  FormatUtil.print(value, "#,###.##"));
					toolTip.show(new Point(e.x, e.y));
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
		});
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new OpenPTPairAllAction(getViewSite().getWorkbenchWindow(), "Load", serverId, objType, counter));
		
		ObjectSelectManager.getInstance().addObjectCheckStateListener(this);
	}
	
	public void setInput(long stime, long etime) {
		this.stime = stime;
		this.etime = etime;
		try {
			setViewTab(objType, counter, serverId);
			Server server = ServerManager.getInstance().getServer(serverId);
			CounterEngine ce = server.getCounterEngine();
			String counterName = ce.getCounterDisplayName(objType, counter);
			desc = "ⓢ" + server.getName() + " | (Past All) " + counterName + "(" + ce.getCounterUnit(objType, counter) + ") "
					+ DateUtil.format(stime, "yyyyMMdd HH:mm:ss") + " ~ " + DateUtil.format(etime, "HH:mm:ss");
			this.xyGraph.primaryXAxis.setRange(stime, etime);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Set<Integer> keySet = dataMap.keySet();
		for (Integer key : keySet) {
			TracePair tp = dataMap.get(key);
			xyGraph.removeTrace(tp.t1);
			xyGraph.removeTrace(tp.t2);
		}
		dataMap.clear();
		load();
	}
	
	private void load() {
		CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		new Job("Load " + counterEngine.getCounterDisplayName(objType, counter)) {
			protected IStatus run(IProgressMonitor monitor) {
				final List<MapPack> values = new ArrayList<MapPack>();
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("stime", stime);
					param.put("etime", etime);
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
				
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						double max = 0;
						for (MapPack mpack : values) {
							int objHash = mpack.getInt("objHash");
							ListValue time = mpack.getList("time");
							ListValue value = mpack.getList("value");
							TracePair tp = getTracePair(objHash);
							CircularBufferDataProvider maxProvider = (CircularBufferDataProvider) tp.t1.getDataProvider();
							CircularBufferDataProvider valueProvider = (CircularBufferDataProvider) tp.t2.getDataProvider();
							maxProvider.clearTrace();
							valueProvider.clearTrace();
							for (int i = 0; time != null && i < time.size(); i++) {
								long x = time.getLong(i);
								Value v = value.get(i);
								if (v != null && v.getValueType() == ValueEnum.LIST) {
									ListValue lv = (ListValue) v;
									maxProvider.addSample(new Sample(x, lv.getDouble(0)));
									valueProvider.addSample(new Sample(x, lv.getDouble(1)));
								}
							}
							max = Math.max(ChartUtil.getMax(maxProvider.iterator()), max);
						}
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
			
		}.schedule();
	}
	
	public void notifyChangeState() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				for (TracePair tp : dataMap.values()) {
					if (ObjectSelectManager.getInstance().isUnselectedObject(tp.objHash)) {
						tp.setVisible(false);
					} else {
						tp.setVisible(true);
					}
				}
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						redraw();
					}
				});
			}
		});
	}
	
	private TracePair getTracePair(int objHash) {
		TracePair tp = dataMap.get(objHash);
		if (tp == null) {
			tp = new TracePair();
			tp.objHash = objHash;
			
			CircularBufferDataProvider data1 = new CircularBufferDataProvider(true);
			data1.setBufferSize((int) ((etime - stime) / (DateUtil.MILLIS_PER_SECOND * 2)));
			data1.setCurrentXDataArray(new double[] {});
			data1.setCurrentYDataArray(new double[] {});
			String name = StringUtil.trimToEmpty(TextProxy.object.getLoadText(
					DateUtil.yyyymmdd(stime), objHash,
					serverId));
			Trace trace1 = new Trace(name+"(Max)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, data1);
			trace1.setPointStyle(PointStyle.NONE);
			trace1.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace1.setTraceType(TraceType.SOLID_LINE);
			trace1.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			xyGraph.addTrace(trace1);
			tp.t1 = trace1;
			
			CircularBufferDataProvider data2 = new CircularBufferDataProvider(true);
			data2.setBufferSize((int) ((etime - stime) / (DateUtil.MILLIS_PER_SECOND * 2)));
			data2.setCurrentXDataArray(new double[] {});
			data2.setCurrentYDataArray(new double[] {});
			Trace trace2 = new Trace(name+"(Value)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, data2);
			trace2.setPointStyle(PointStyle.NONE);
			trace2.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace2.setTraceType(TraceType.SOLID_LINE);
			trace2.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			xyGraph.addTrace(trace2);
			tp.t2 = trace2;
			
			dataMap.put(objHash, tp);
		}
		return tp;
	}

	@Override
	public void dispose() {
		super.dispose();
		ObjectSelectManager.getInstance().removeObjectCheckStateListener(this);
	}
	
	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}
	
	private static class TracePair {
		int objHash;
		Trace t1;
		Trace t2;
		
		public void setLineWidth(int width) {
			if (t1 != null)  t1.setLineWidth(width);
			if (t2 != null)  t2.setLineWidth(width);
		}
		
		public void setVisible(boolean visible) {
			if (t1 != null)  t1.setVisible(visible);
			if (t2 != null)  t2.setVisible(visible);
		}
	}
}
