package scouter.client.counter.views;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.counter.actions.OpenPTPairAllAction2;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.io.DataInputX;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class CounterPTAllPairChart2 extends CounterAllPairPainter {
	
	public final static String ID = CounterPTAllPairChart2.class.getName();

	private int serverId;
	private String objType;
	private String counter1;
	private String counter2;
	private long stime;
	private long etime;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
		this.counter1 = ids[2];
		this.counter2 = ids[3];
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new OpenPTPairAllAction2(getViewSite().getWorkbenchWindow(), "Load", serverId, objType, counter1, counter2));
	}
	
	public void setInput(long stime, long etime) {
		this.stime = stime;
		this.etime = etime;
		try {
			setViewTab(objType, counter1, serverId);
			Server server = ServerManager.getInstance().getServer(serverId);
			CounterEngine ce = server.getCounterEngine();
			String counterName = ce.getCounterDisplayName(objType, counter1);
			desc = "ⓢ" + server.getName() + " | (Past All) " + counterName + "(" + ce.getCounterUnit(objType, counter1) + ") "
					+ DateUtil.format(stime, "yyyyMMdd HH:mm:ss") + " ~ " + DateUtil.format(etime, "HH:mm:ss");
			this.xyGraph.primaryXAxis.setRange(stime, etime);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Set<Integer> keySet = dataMap.keySet();
		for (Integer key : keySet) {
			TracePair tp = dataMap.get(key);
			xyGraph.removeTrace(tp.totalTrace);
			xyGraph.removeTrace(tp.activeTrace);
		}
		dataMap.clear();
		load();
	}
	
	private void load() {
		CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		new Job("Load " + counterEngine.getCounterDisplayName(objType, counter1)) {
			protected IStatus run(IProgressMonitor monitor) {
				final Map<Integer, MapPack> valueMap1 = new HashMap<Integer, MapPack>();
				final Map<Integer, MapPack> valueMap2 = new HashMap<Integer, MapPack>();
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("stime", stime);
					param.put("etime", etime);
					param.put("objType", objType);
					param.put("counter", counter1);
					
					tcp.process(RequestCmd.COUNTER_PAST_TIME_ALL, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							MapPack mpack = (MapPack) in.readPack();
							int objHash = mpack.getInt("objHash");
							valueMap1.put(objHash, mpack);
						};
					});
					
					param.put("counter", counter2);
					
					tcp.process(RequestCmd.COUNTER_PAST_TIME_ALL, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							MapPack mpack = (MapPack) in.readPack();
							int objHash = mpack.getInt("objHash");
							valueMap2.put(objHash, mpack);
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
						for (MapPack mpack : valueMap1.values()) {
							try {
								int objHash = mpack.getInt("objHash");
								ListValue time = mpack.getList("time");
								ListValue value1 = mpack.getList("value");
								ListValue value2 = valueMap2.get(objHash).getList("value");
								
								TracePair tp = getTracePair(objType, objHash, (int) ((etime - stime) / (DateUtil.MILLIS_PER_SECOND * 2)));
								CircularBufferDataProvider maxProvider = (CircularBufferDataProvider) tp.totalTrace.getDataProvider();
								CircularBufferDataProvider valueProvider = (CircularBufferDataProvider) tp.activeTrace.getDataProvider();
								maxProvider.clearTrace();
								valueProvider.clearTrace();
								for (int i = 0; time != null && i < time.size(); i++) {
									long x = time.getLong(i);
									maxProvider.addSample(new Sample(x, CastUtil.cdouble(value1.get(i))));
									valueProvider.addSample(new Sample(x, CastUtil.cdouble(value2.get(i))));
								}
								max = Math.max(ChartUtil.getMax(maxProvider.iterator()), max);
							} catch (Throwable th) {}
						}
						if (CounterUtil.isPercentValue(objType, counter1)) {
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
}
