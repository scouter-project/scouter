package scouter.client.counter.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.CounterColorManager;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class CounterRealTimeMultiView extends ViewPart implements Refreshable {
	
	public static String ID = CounterRealTimeMultiView.class.getName();
	
	int serverId;
	int objHash;
	String objType;
	String title;
	List<String> counters = new ArrayList<String>();
	
	CounterEngine counterEngine;
	RefreshThread thread;
	
	FigureCanvas canvas;
	XYGraph xyGraph;
	HashMap<String, Trace> traceMap = new HashMap<String, Trace>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
		objHash = CastUtil.cint(ids[1]);
		objType = ids[2];
		title = ids[3];
		for (int i = 4; i < ids.length; i++) {
			counters.add(ids[i]);
		}
	}

	public void createPartControl(Composite parent) {
		counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		String objName = TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId);
		setPartName(title + "[" + objName + "]");
		parent.setLayout(new GridLayout());
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		canvas = new FigureCanvas(parent);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				xyGraph.setSize(r.width, r.height);
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		
		xyGraph = new XYGraph();
		xyGraph.setShowTitle(false);
		xyGraph.setShowLegend(true);
		canvas.setContents(xyGraph);
		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);
		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		xyGraph.primaryYAxis.setFormatPattern("#,##0");
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		for (String counter : counters) {
			String name = counterEngine.getCounterDisplayName(objType, counter);
			CircularBufferDataProvider provider = new CircularBufferDataProvider(true);
			provider.setBufferSize(155);
			Trace trace = new Trace(name , xyGraph.primaryXAxis, xyGraph.primaryYAxis, provider);
			trace.setPointStyle(PointStyle.NONE);
			trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace.setTraceType(TraceType.SOLID_LINE);
			trace.setTraceColor(CounterColorManager.getInstance().assignColor(counter));
			traceMap.put(counter, trace);
			xyGraph.addTrace(trace);
		}
		
		thread = new RefreshThread(this, 2000);
		thread.start();
	}

	public void setFocus() {
		
	}
 
	public void refresh() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;
		try {
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			ListValue counterLv = param.newList("counter");
			for (String counter : counters) {
				counterLv.add(counter);
			}
			p = tcp.getSingle(RequestCmd.COUNTER_REAL_TIME_MULTI, param);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (p == null) {
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					setTitleImage(Images.inactive);
					long now = TimeUtil.getCurrentTime(serverId);
					xyGraph.primaryXAxis.setRange(now - DateUtil.MILLIS_PER_MINUTE * 5, now + 1);
				}
			});
		} else {
			MapPack m = (MapPack) p;
			final ListValue counterLv = m.getList("counter");
			final ListValue valueLv = m.getList("value");
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					long now = TimeUtil.getCurrentTime(serverId);
					xyGraph.primaryXAxis.setRange(now - DateUtil.MILLIS_PER_MINUTE * 5, now + 1);
					int size = counterLv.size();
					if (size > 0) {
						setTitleImage(Images.active);
					} else {
						setTitleImage(Images.inactive);
					}
					double max = 0.0d;
					for (int i = 0; i < counterLv.size(); i++) {
						String counter = counterLv.getString(i);
						double value = valueLv.getDouble(i);
						Trace t = traceMap.get(counter);
						CircularBufferDataProvider provider = (CircularBufferDataProvider) t.getDataProvider();
						provider.addSample(new Sample(now, value));
						double max2 = ChartUtil.getMax(provider.iterator());
						if (max2 > max) {
							max = max2;
						}
					}
					xyGraph.primaryYAxis.setRange(0, max);
				}
			});
		}
	}
	
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}
	
}
