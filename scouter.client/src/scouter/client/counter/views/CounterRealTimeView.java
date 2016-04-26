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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.AgentColorManager;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
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
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.client.views.ScouterViewPart;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class CounterRealTimeView extends ScouterViewPart implements Refreshable {
	public static final String ID = CounterRealTimeView.class.getName();

	protected String objName;
	protected int objHash;
	protected String objType;
	protected String counter;
	protected int serverId;

	protected RefreshThread thread;

	private IMemento memento;

	IWorkbenchWindow window;
	IToolBarManager man;
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	public void setInput(int objHash, String objName, String objType, String counter, int serverId) throws Exception{
		if (thread != null && thread.isAlive()) {
			return;
		}
		this.objHash = objHash;
		this.objName = objName;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String counterDisplay = "";
		String counterUnit = "";
		if(server != null){
			svrName = server.getName();
			counterDisplay = server.getCounterEngine().getCounterDisplayName(objType, counter);
			counterUnit = server.getCounterEngine().getCounterUnit(objType, counter);
		}
		if(server == null){
			return;
		}
		desc = "ⓢ"+svrName+" | (Realtime) [" + objName + "] " + counterDisplay + (!"".equals(counterUnit)?" ("+counterUnit+")":"");
		setViewTab(objType, counter, serverId);
		
		MenuUtil.createCounterContextMenu(ID, canvas, serverId, objHash, objType, counter);

		if (thread == null) {
			thread = new RefreshThread(this, 2000);
			thread.start();
		}
		
		thread.setName(this.toString() + " - " 
				+ "objHash:"+objHash
				+ ", objName:"+objName
				+ ", objType:"+objType
				+ ", counter:"+counter
				+ ", serverId:"+serverId);
	}
	
	public void getRealtimePrevPerf(int objHash, String objType, String counter) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			param.put("objType", objType);
			param.put("counter", counter);

			long etime = TimeUtil.getCurrentTime(serverId);
			long stime = etime - DateUtil.MILLIS_PER_MINUTE * 5;
			param.put("stime", stime);
			param.put("etime", etime);
			
			MapPack out = (MapPack) tcp.getSingle(RequestCmd.COUNTER_PAST_TIME, param);
			
			if (out == null){
				return;
			}
			final ListValue time = out.getList("time");
			final ListValue value = out.getList("value");

			Map<Long, Value> map  = new HashMap<Long, Value>();

			for (int i = 0; time != null && i < time.size(); i++) {
				map.put(CastUtil.clong(time.get(i)), value.get(i));
			}
			final TreeMap<Long, Value> treeMap = new TreeMap<Long, Value>( map );
			
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					
					Iterator<Long> xs = treeMap.keySet().iterator();
					
					clearAllTrace();
					
					while(xs.hasNext()){
						long tm = xs.next();
						Value v = treeMap.get(tm);
						if (v instanceof ListValue) {
							ListValue list = (ListValue) v;
							for (int j = 0, size = list.size(); j < size; j++) {
								getDataProvider("data-" + j).addSample(new Sample(tm, list.getDouble(j)));
							}
						} else if (v != null && v.getValueType() != ValueEnum.NULL) {
							getDataProvider("data").addSample(
									new Sample(tm, CastUtil.cdouble(v)));
						}
					}
				}
			});
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}

	private boolean init = false;

	boolean isActive = false;
	public void refresh() {
		if (init == false) {
			init = true;
			clearAllTrace();
			getRealtimePrevPerf(objHash, objType, counter);
		}
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		final Stack<Value> value = new Stack<Value>();
		try {
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			param.put("counter", counter);
			param.put("timetype", TimeTypeEnum.REALTIME);
			isActive = false;
			tcp.process(RequestCmd.COUNTER_REAL_TIME, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Value v = in.readValue();
					if (v != null && v.getValueType() != ValueEnum.NULL) {
						value.push(v);
						isActive = true;
					}
				}
			});
			
		} catch(Throwable t){
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				
				if(isActive){
					setActive();
				}else{
					setInactive();
				}
				long now = TimeUtil.getCurrentTime(serverId);
				xyGraph.primaryXAxis.setRange(now - DateUtil.MILLIS_PER_MINUTE * 5, now + 1);
				double maxv = 0;
				if (value.size() > 0) {
					Value v = value.pop();
					if (v instanceof ListValue) {
						ListValue list = (ListValue) v;
						for (int j = 0, max = list.size(); j < max; j++) {
							getDataProvider("data-" + j).addSample(new Sample(now, list.getDouble(j)));
							maxv = Math.max(ChartUtil.getMax(getDataProvider("data-" + j).iterator()), maxv);
						}
					} else if (v.getValueType() != ValueEnum.NULL) {
						getDataProvider("data").addSample(new Sample(now, CastUtil.cdouble(v)));
						maxv = ChartUtil.getMax(getDataProvider("data").iterator());
					}
				}

				if (CounterUtil.isPercentValue(objType, counter)) {
					xyGraph.primaryYAxis.setRange(0, 100);
				} else {
					xyGraph.primaryYAxis.setRange(0, maxv);
				}
				canvas.redraw();
				xyGraph.repaint();

			}

		});
	}

	protected XYGraph xyGraph;
	protected FigureCanvas canvas;

	public void createPartControl(Composite parent) {
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		man = getViewSite().getActionBars().getToolBarManager();
		
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				init = false;
				refresh();
			}
		});
		
		parent.setLayout(UIUtil.formLayout(0, 0));
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		
		canvas = new FigureCanvas(parent);
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));

		canvas.setLayoutData(UIUtil.formData(0, 0, 0, 0, 100, 0, 100, 0));
		
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterRealTimeView.this.setContentDescription(desc);
					} else{
						CounterRealTimeView.this.setContentDescription("");
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
		xyGraph.setShowLegend(false);
		xyGraph.setShowTitle(false);

		canvas.setContents(xyGraph);

		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);

		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);

		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));

		restoreState();
		
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.F5){
					init = false;
					refresh();
				}
			}
		});
	}

	protected Map<String, CircularBufferDataProvider> datas = new HashMap<String, CircularBufferDataProvider>();

	private void clearAllTrace(){
		Iterator<String> itr = datas.keySet().iterator();
		while(itr.hasNext()){
			String providerName = itr.next();
			CircularBufferDataProvider provider = datas.get(providerName);
			provider.clearTrace();
		}
	}
	
	private CircularBufferDataProvider getDataProvider(String linename) {
		CircularBufferDataProvider data = datas.get(linename);
		if (data == null) {
			data = new CircularBufferDataProvider(true);
			datas.put(linename, data);

			data.setBufferSize(155);
			data.setCurrentXDataArray(new double[] {});
			data.setCurrentYDataArray(new double[] {});
			Trace trace = new Trace(linename, xyGraph.primaryXAxis, xyGraph.primaryYAxis, data);
			trace.setPointStyle(PointStyle.NONE);
			trace.getXAxis().setFormatPattern("HH:mm:ss");
			trace.getYAxis().setFormatPattern("#,##0");

			trace.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace.setTraceType(TraceType.SOLID_LINE);
			trace.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));

			xyGraph.addTrace(trace);

		}
		return data;

	}

	public void setFocus() {
		statusMessage = desc + " - setInput(int objHash:"+objHash+", String objName:"+objName+", String objType:"+objType+", String counter:"+counter+", int serverId:"+serverId+")";
		super.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}

	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento = memento.createChild(ID);
		memento.putInteger("objHash", objHash);
		memento.putString("objName", objName);
		memento.putString("objType", objType);
		memento.putString("counter", counter);
		memento.putInteger("serverId", serverId);
	}

	private void restoreState() {
		if (memento == null)
			return;
		IMemento m = memento.getChild(ID);

		int objHash = CastUtil.cint(m.getInteger("objHash"));
		String objName = m.getString("objName");
		String objType = m.getString("objType");
		String counter = m.getString("counter");
		int serverId = CastUtil.cint(m.getInteger("serverId"));
		try {
			setInput(objHash, objName, objType, counter, serverId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}