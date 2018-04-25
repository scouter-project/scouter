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
package scouter.client.group.view;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import scouter.client.Images;
import scouter.client.group.GroupManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.XLogData;
import scouter.client.model.XLogDataComparator;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.TimeRangeDialog;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.xlog.XLogUtil;
import scouter.client.xlog.views.XLogViewCommon;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class XLogLoadTimeGroupView extends XLogViewCommon implements TimeRangeDialog.ITimeRange, CalendarDialog.ILoadCalendarDialog {

	public static final String ID = XLogLoadTimeGroupView.class.getName();
	
	private String grpName;
	private Server defaultServer = ServerManager.getInstance().getDefaultServer();
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	private Map<Integer, ListValue> serverObjMap = new HashMap<Integer, ListValue>();
	
	private long stime;
	private long etime;

	private int firstServerId;
	
	LoadXLogJob loadJob;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] datas = secId.split("&");
		grpName = datas[0];
		objType = datas[1];
	}

	@Override
	protected void openInExternalLink() {
		Program.launch(makeExternalUrl(firstServerId));
	}

	@Override
	protected void clipboardOfExternalLink() {
		Clipboard clipboard = new Clipboard(getViewSite().getShell().getDisplay());
		String linkUrl = makeExternalUrl(firstServerId);
		clipboard.setContents(new String[]{linkUrl}, new Transfer[]{TextTransfer.getInstance()});
		clipboard.dispose();
	}

	public void createPartControl(final Composite parent) {
		display = Display.getCurrent();
		shell = new Shell(display);
		this.setPartName("XLog - " + grpName);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		
		create(parent, man);
		
		man.add(new Action("zoom in", ImageUtil.getImageDescriptor(Images.zoomin)) {
			public void run() {
				TimeRangeDialog dialog = new TimeRangeDialog(display, XLogLoadTimeGroupView.this, DateUtil.yyyymmdd(stime));
				dialog.show(stime, etime);
			}
		});
		man.add(new Action("zoom out", ImageUtil.getImageDescriptor(Images.zoomout)) {
			public void run() {
				if (viewPainter.isZoomMode()) {
					viewPainter.endZoom();
				} else {
					viewPainter.keyPressed(16777261);
					viewPainter.build();
				}
				canvas.redraw();
			}
		});
		man.add(new Separator());		
		createContextMenu(parent, new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager){
            	if(viewPainter.isZoomMode() == false){
            		manager.add(new Action("Load") {
						public void run() {
							CalendarDialog dialog = new CalendarDialog(display, XLogLoadTimeGroupView.this);
							dialog.showWithEndTime();
						}
            		});
            		manager.add(new Action("Zoom In", ImageDescriptor.createFromImage(Images.zoomin)) {
						public void run() {
							TimeRangeDialog dialog = new TimeRangeDialog(display, XLogLoadTimeGroupView.this, DateUtil.yyyymmdd(stime));
							dialog.show(stime, etime);
						}
            		});
            	}
            }
		});
		
		canvas.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent e) {
				viewPainter.set(canvas.getClientArea());
				viewPainter.build();
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		setObjType(objType);
	}
	
	private void createContextMenu(Composite parent, IMenuListener listener){
        MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(listener);
        Menu menu = contextMenu.createContextMenu(parent);
        canvas.setMenu(menu);
    }
	
	public void setFocus() {
		super.setFocus();
	}
	
	public void setInput(long stime, long etime) {
		this.stime = stime;
		this.etime = etime;
		viewPainter.setEndTime(etime);
		viewPainter.setTimeRange(etime - stime);
		setDate(DateUtil.yyyymmdd(stime));
		String objTypeDisplay = defaultServer.getCounterEngine().getDisplayNameObjectType(objType);
		setContentDescription(grpName +" | "+objTypeDisplay+"\'s "+"XLog Pasttime"
				+ " | " + DateUtil.format(stime, "yyyy-MM-dd") + "(" + DateUtil.format(stime, "HH:mm")
				+ "~" + DateUtil.format(etime, "HH:mm") + ")");
		try {
			loadJob = new LoadXLogJob();
			loadJob.schedule();
		} catch (Exception e) {
			MessageDialog.openError(shell, "Error", e.getMessage());
		}
	}
	
	private GroupManager manager = GroupManager.getInstance();
	
	private void collectObj() {
		serverObjMap.clear();
		Set<Integer> objHashs = manager.getObjectsByGroup(grpName);
		for (int objHash : objHashs) {
			AgentObject agentObj = AgentModelThread.getInstance().getAgentObject(objHash);
			if (agentObj == null || agentObj.isAlive() == false) {
				continue;
			}
			int serverId = agentObj.getServerId();
			if (firstServerId == 0) {
				firstServerId = serverId;
			}
			ListValue lv = serverObjMap.get(serverId);
			if (lv == null) {
				lv = new ListValue();
				serverObjMap.put(serverId, lv);
			}
			lv.add(objHash);
		}
	}

	public void loadAdditinalData(long stime, long etime, final boolean reverse) {
		collectObj();
		Iterator<Integer> serverIds = serverObjMap.keySet().iterator();
		final TreeSet<XLogData> tempSet = new TreeSet<XLogData>(new XLogDataComparator());
		int limit = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_IGNORE_TIME);
		final int max = getMaxCount();
		while (serverIds.hasNext()) {
			final int serverId = serverIds.next();
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				param.put("date", DateUtil.yyyymmdd(stime));
				param.put("stime", stime);
				param.put("etime", etime);
				param.put("objHash", serverObjMap.get(serverId));
				param.put("reverse", new BooleanValue(reverse));
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
						if (tempSet.size() < max) {
							tempSet.add(new XLogData(x, serverId));
						}
					}
				});
			} catch (Throwable t) {
				ConsoleProxy.errorSafe(t.toString());
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
		}
		if (reverse) {
			Iterator<XLogData> itr = tempSet.descendingIterator();
			while (itr.hasNext()) {
				XLogData d = itr.next();
				twdata.putFirst(d.p.txid, d);
			}
		} else {
			Iterator<XLogData> itr = tempSet.iterator();
			while (itr.hasNext()) {
				XLogData d = itr.next();
				twdata.putLast(d.p.txid, d);
			}
		}
	}

	public void setTimeRange(long stime, long etime) {
		if (viewPainter.zoomIn(stime, etime)) {
			canvas.redraw();
		}
	}

	public void onPressedOk(long startTime, long endTime) {
		setInput(startTime, endTime);
	}

	public void onPressedOk(String date) {}

	public void onPressedCancel() {}
	
	class LoadXLogJob extends Job{
		
		int count = 0;
		
		public LoadXLogJob() {
			super("XLog Loading...(" 
					+ DateUtil.format(stime, "yyyy-MM-dd")
					+ " "
					+ DateUtil.format(stime, "HH:mm") 
					+ "~" 
					+ DateUtil.format(etime, "HH:mm") + ")");
		}

		protected IStatus run(final IProgressMonitor monitor) {
			monitor.beginTask("Loading...", IProgressMonitor.UNKNOWN);
			twdata.clear();
			collectObj();
			Iterator<Integer> serverIds = serverObjMap.keySet().iterator();
			final TreeSet<XLogData> tempSet = new TreeSet<XLogData>(new XLogDataComparator());
			int limit = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_IGNORE_TIME);
			final int max = getMaxCount();
			twdata.setMax(max);
			while (serverIds.hasNext()) {
				final int serverId = serverIds.next();
				final Server server = ServerManager.getInstance().getServer(serverId);
				monitor.subTask(server.getName() + " data loading...");
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("date", DateUtil.yyyymmdd(stime));
					param.put("stime", stime);
					param.put("etime", etime);
					param.put("objHash", serverObjMap.get(serverId));
					if (limit > 0) {
						param.put("limit", limit);
					}		
					if (max > 0) {
						param.put("max", max);
					}

					tcp.process(RequestCmd.TRANX_LOAD_TIME_GROUP, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							Pack p = in.readPack();
							if (monitor.isCanceled()) {
								throw new IOException("User cancelled");
							}
							XLogPack x = XLogUtil.toXLogPack(p);
							if (tempSet.size() < max) {
								tempSet.add(new XLogData(x, serverId));
								count++;
							}
							if (count % 10000 == 0) {
								monitor.subTask(count + " XLog data received.");
							}
						}
					});
				} catch(Throwable e){
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
			}
			for (XLogData d : tempSet) {
				twdata.putLast(d.p.txid, d);
			}
			monitor.done();
			refresh();
			return Status.OK_STATUS;
		}

		private void refresh() {
			viewPainter.build();
			ExUtil.exec(canvas, new Runnable() {
				public void run() {
					canvas.redraw();
				}
			});
		}
	}
	
	public void dispose() {
		super.dispose();
		if (loadJob != null && (loadJob.getState() == Job.WAITING || loadJob.getState() == Job.RUNNING)) {
			loadJob.cancel();
		}
	}
}
