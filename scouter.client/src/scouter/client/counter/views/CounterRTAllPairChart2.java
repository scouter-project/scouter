package scouter.client.counter.views;

import java.util.HashMap;
import java.util.Iterator;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.counter.actions.OpenPTPairAllAction2;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CounterUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class CounterRTAllPairChart2 extends CounterAllPairPainter implements Refreshable {
	
	public final static String ID = CounterRTAllPairChart2.class.getName();

	protected RefreshThread thread = null;
	
	private int serverId;
	private String objType;
	private String counter1;
	private String counter2;
	
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
		try {
			setViewTab(objType, counter1, serverId);
			Server server = ServerManager.getInstance().getServer(serverId);
			CounterEngine ce = server.getCounterEngine();
			String counterName = ce.getCounterDisplayName(objType, counter1);
			desc = "ⓢ" + server.getName() + " | (Current All) " + counterName + "(" + ce.getCounterUnit(objType, counter1) + ")";
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new OpenPTPairAllAction2(getViewSite().getWorkbenchWindow(), "Load", serverId, objType, counter1, counter2));
		
		thread = new RefreshThread(this, 2000);
		thread.setName(this.toString() + " - " + "objType:" + objType + ", counter:" + counter1 + ", serverId:" + serverId);
		thread.start();
	}
	
	public void refresh() {
		final HashMap<Integer, MapValue> values = new HashMap<Integer, MapValue>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objType", objType);
			ListValue counterList = param.newList("counter");
			counterList.add(counter1);
			counterList.add(counter2);
			MapPack out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_REAL_TIME_ALL_MULTI, param);
			isActive = false;
			if (out != null) {
				ListValue objHashLv = out.getList("objHash");
				ListValue counterLv = out.getList("counter");
				ListValue valueLv = out.getList("value");
				for (int i = 0; i < objHashLv.size(); i++) {
					int objHash = CastUtil.cint(objHashLv.get(i));
					MapValue mv = values.get(objHash);
					if (mv == null) {
						mv = new MapValue();
						values.put(objHash, mv);
					}
					mv.put(counterLv.getString(i), valueLv.get(i));
					isActive = true;
				}
			}
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				if (isActive) {
					setActive();
				} else {
					setInactive();
				}

				long now = TimeUtil.getCurrentTime(serverId);
				xyGraph.primaryXAxis.setRange(now - DateUtil.MILLIS_PER_MINUTE * 5, now + 1);
				Iterator<Integer> itr = values.keySet().iterator();
				while (itr.hasNext()) {
					int objHash = itr.next();
					Value value = values.get(objHash);
					if (value != null && value.getValueType() == ValueEnum.MAP) {
						MapValue mv = (MapValue) value;
						TracePair tp = getTracePair(objType, objHash, 155);
						CircularBufferDataProvider provider1 = (CircularBufferDataProvider) tp.totalTrace.getDataProvider();
						CircularBufferDataProvider provider2 = (CircularBufferDataProvider) tp.activeTrace.getDataProvider();
						provider1.addSample(new Sample(now, CastUtil.cdouble(mv.get(counter1))));
						provider2.addSample(new Sample(now, CastUtil.cdouble(mv.get(counter2))));
					}

				}
				if (CounterUtil.isPercentValue(objType, counter1)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					double max = getMaxValue();
					xyGraph.primaryYAxis.setRange(0, max);
				}
				redraw();
			}
		});
	}
}
