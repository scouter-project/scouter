package scouter.client.maria.views;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.maria.actions.OpenDbDailyConnView;
import scouter.client.maria.actions.OpenDigestTableAction;
import scouter.client.model.AgentDailyListProxy;
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
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class DbDailyTotalConnView extends ViewPart {
	
	public static final String ID = DbDailyTotalConnView.class.getName();
	
	int serverId;
	
	static long TIME_RANGE = DateUtil.MILLIS_PER_DAY;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 10);
	static int BUFFER_SIZE = (int)(TIME_RANGE / DateUtil.MILLIS_PER_FIVE_MINUTE);
	
	FigureCanvas canvas;
	XYGraph xyGraph;
	
	Trace totalTrace;
	Trace activeTrace;
	
	String date;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
		date = ids[1];
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		this.setPartName("Connections[" + server.getName() + "][" + date + "]");
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
		xyGraph.primaryYAxis.setFormatPattern("#,##0");
		
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		final CircularBufferDataProvider totalProvider = new CircularBufferDataProvider(true);
		totalProvider.setBufferSize(BUFFER_SIZE);
		totalProvider.setCurrentXDataArray(new double[] {});
		totalProvider.setCurrentYDataArray(new double[] {});
		totalTrace = new Trace("Total (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, totalProvider);
		totalTrace.setPointStyle(PointStyle.NONE);
		totalTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		totalTrace.setTraceType(TraceType.SOLID_LINE);
		totalTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_RED));
		xyGraph.addTrace(totalTrace);
		
		CircularBufferDataProvider activeProvider = new CircularBufferDataProvider(true);
		activeProvider.setBufferSize(BUFFER_SIZE);
		activeProvider.setCurrentXDataArray(new double[] {});
		activeProvider.setCurrentYDataArray(new double[] {});
		activeTrace = new Trace("Running (SUM)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, activeProvider);
		activeTrace.setPointStyle(PointStyle.NONE);
		activeTrace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
		activeTrace.setTraceType(TraceType.SOLID_LINE);
		activeTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GREEN));
		xyGraph.addTrace(activeTrace);
		
		ScouterUtil.addHorizontalRangeListener(xyGraph.getPlotArea(), new OpenDigestTableAction(serverId), true);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new OpenDbDailyConnView(serverId, date));
		man.add(new Action("Zoom out", ImageUtil.getImageDescriptor(Images.zoomout)) {
			public void run() {
				long stime = DateUtil.yyyymmdd(date);
				long etime = stime + DateUtil.MILLIS_PER_DAY - 1;
				xyGraph.primaryXAxis.setRange(stime, etime);
				double max = ChartUtil.getMax(totalProvider.iterator());
				xyGraph.primaryYAxis.setRange(0, max);
			}
		});
		load();
	}

	public void setFocus() {
		
	}
	
	AgentDailyListProxy agentDailyProxy = new AgentDailyListProxy();

	public void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				Pack p = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", agentDailyProxy.getObjHashLv(date, serverId, CounterConstants.MARIA_PLUGIN));
					param.put("sdate", date);
					param.put("edate", date);
					p = tcp.getSingle(RequestCmd.DB_DAILY_CONNECTIONS, param);
				} catch (Exception e) {
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (p == null) {
					ExUtil.exec(canvas, new Runnable() {
						public void run() {
							setTitleImage(Images.inactive);
							DbDailyTotalConnView.this.setContentDescription(date.substring(0, 4) + "-" + date.substring(5, 6) + "-" + date.substring(7, 8));
							long stime = DateUtil.yyyymmdd(date);
							long etime = stime + DateUtil.MILLIS_PER_DAY - 1;
							xyGraph.primaryXAxis.setRange(stime, etime);
						}
					});
				} else {
					MapPack m = (MapPack) p;
					final ListValue timeLv = m.getList("time");
					final ListValue totalLv = m.getList("total");
					final ListValue activeLv = m.getList("active");
					ExUtil.exec(canvas, new Runnable() {
						public void run() {
							setTitleImage(Images.active);
							CircularBufferDataProvider totalProvider = (CircularBufferDataProvider)totalTrace.getDataProvider();
							CircularBufferDataProvider activeProvider = (CircularBufferDataProvider)activeTrace.getDataProvider();
							totalProvider.clearTrace();
							activeProvider.clearTrace();
							for (int i = 0; i < timeLv.size(); i++) {
								long time = timeLv.getLong(i);
								double totalV = totalLv.getDouble(i);
								double activeV = activeLv.getDouble(i);
								totalProvider.addSample(new Sample(time, totalV));
								activeProvider.addSample(new Sample(time, activeV));
							}
							DbDailyTotalConnView.this.setContentDescription(date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8));
							long stime = DateUtil.yyyymmdd(date);
							long etime = stime + DateUtil.MILLIS_PER_DAY - 1;
							xyGraph.primaryXAxis.setRange(stime, etime);
							double max = ChartUtil.getMax(totalProvider.iterator());
							xyGraph.primaryYAxis.setRange(0, max);
						}
					});
				}
				
			}
		});
		
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}
}
