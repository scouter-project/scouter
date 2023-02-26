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
package scouter.client.xlog.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import scouter.client.Images;
import scouter.client.model.AgentDailyListProxy;
import scouter.client.model.AgentModelThread;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.XLogData;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.client.xlog.XLogUtil;
import scouter.client.xlog.actions.OpenSearchXLogDialogAction;
import scouter.client.xlog.actions.OpenXLogLoadTimeAction;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

import java.io.IOException;


public class XLogRealTimeView extends XLogViewCommon implements Refreshable {

	public static final String ID = XLogRealTimeView.class.getName();
	
	private RefreshThread thread;

	Action act;
	
	private MapPack param = new MapPack();
	
	int serverId;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
	}

	@Override
	protected void openInExternalLink() {
		Program.launch(makeExternalUrl(serverId));
	}

	@Override
	protected void clipboardOfExternalLink() {
		Clipboard clipboard = new Clipboard(getViewSite().getShell().getDisplay());
		String linkUrl = makeExternalUrl(serverId);
		clipboard.setContents(new String[]{linkUrl}, new Transfer[]{TextTransfer.getInstance()});
		clipboard.dispose();
	}

	@Override
	public void createPartControl(final Composite parent) {
		display = Display.getCurrent();
		shell = new Shell(display);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();

		create(parent, man);

		Action searchOpenAction = new Action("search", ImageUtil.getImageDescriptor(Images.search)) {
			public void run() {
				new OpenSearchXLogDialogAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), serverId, objType).run();
			}
		};
		man.add(searchOpenAction);
		man.add(new Separator());
		;
		man.add(new Action("zoom in", ImageUtil.getImageDescriptor(Images.zoomin)) {
			public void run() {
				viewPainter.keyPressed(16777259);
				viewPainter.build();
				canvas.redraw();
			}
		});
		man.add(new Action("zoom out", ImageUtil.getImageDescriptor(Images.zoomout)) {
			public void run() {
				viewPainter.keyPressed(16777261);
				viewPainter.build();
				canvas.redraw();
			}
		});
		canvas.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent e) {
				viewPainter.set(canvas.getClientArea());
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		
		// Add context menu
		new MenuItem(contextMenu, SWT.SEPARATOR);
	    MenuItem loadXLogItem = new MenuItem(contextMenu, SWT.PUSH);
	    loadXLogItem.setText("Load History");
	    loadXLogItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				new OpenXLogLoadTimeAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), "Load XLog", objType, Images.server, serverId).run();
			}
		});
	    
	    MenuItem searchXLogItem = new MenuItem(contextMenu, SWT.PUSH);
	    searchXLogItem.setText("Search");
	    searchXLogItem.setImage(Images.find);
	    searchXLogItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				new OpenSearchXLogDialogAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), serverId, objType).run();
			}
		});
		
		setObjType(objType);
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = "";
		String objTypeDisplay = "";
		if(server != null){
			svrName = server.getName();
			objTypeDisplay = server.getCounterEngine().getDisplayNameObjectType(objType);
			viewPainter.setServerId(serverId);
		}
		this.setPartName("XLog - " + objTypeDisplay);
		setContentDescription("ⓢ"+svrName+" | "+objTypeDisplay+"\'s "+"XLog Realtime");
		thread = new RefreshThread(this, 2000);
		thread.setName(this.toString() + " - " + "objType:"+objType + ", serverId:"+serverId);
		thread.start();
	}
	
	@Override
	public void setFocus() {
		super.setFocus();
		String statusMessage = "setInput(objType:"+objType+", serverId:"+serverId+ ", twdata size(): " + twdata.size() +")";
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		slManager.setMessage(statusMessage);
	}
	
	AgentModelThread agentThread = AgentModelThread.getInstance();

	@Override
	public void refresh() {
		setDate(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)));
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		ListValue objHashLv = agentThread.getLiveObjHashLV(serverId, objType);
		if (objHashLv.size() > 0) {
			try {
				param.put("objHash", objHashLv);
				int limit = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_IGNORE_TIME);
				param.put("limit", limit);
			
				twdata.setMax(getMaxCount());
				tcp.process(RequestCmd.TRANX_REAL_TIME_GROUP, param, new INetReader() {
					public void process(DataInputX in) throws IOException {
						Pack p = in.readPack();
						if (p.getPackType() == PackEnum.MAP) {
							 param = (MapPack) p;
						} else {
							XLogPack x = XLogUtil.toXLogPack(p);
							twdata.putLast(x.txid, new XLogData(x, serverId));
						}
					}
				});
			} catch(Exception e){
				ConsoleProxy.errorSafe(e.toString());
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
		}
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				viewPainter.build();
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						canvas.redraw();
					}
				});
			}
		});

	}
	
	AgentDailyListProxy agnetProxy = new AgentDailyListProxy();
	
	public void loadAdditinalData(long stime, long etime, final boolean reverse) {
		viewPainter.setViewIsInAdditionalDataLoading(true);
		int max = getMaxCount();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			String date = DateUtil.yyyymmdd(stime);
			param.put("date", date);
			param.put("stime", stime);
			param.put("etime", etime);
			param.put("objHash", agnetProxy.getObjHashLv(date, serverId, objType));
			param.put("reverse", new BooleanValue(reverse));
			int limit = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_IGNORE_TIME);
			if (limit > 0) {
				param.put("limit", limit);
			}
			if (max > 0) {
				param.put("max", max);
			}
			twdata.setMax(max);
			tcp.process(RequestCmd.TRANX_LOAD_TIME_GROUP, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Pack p = in.readPack();
					XLogPack x = XLogUtil.toXLogPack(p);
					if (reverse) {
						twdata.putFirst(x.txid, new XLogData(x, serverId));
					} else {
						twdata.putLast(x.txid, new XLogData(x, serverId));
					}
				}
			});
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			viewPainter.setViewIsInAdditionalDataLoading(false);
			TcpProxy.putTcpProxy(tcp);
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (thread != null) {
			thread.shutdown();
		}
	}
}
