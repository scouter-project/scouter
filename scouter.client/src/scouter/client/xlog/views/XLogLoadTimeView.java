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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import scouter.client.Images;
import scouter.client.model.AgentDailyListProxy;
import scouter.client.model.XLogData;
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
import scouter.client.xlog.actions.OpenXLogLoadTimeAction;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.DecimalValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.ThreadUtil;

import java.io.IOException;
import java.util.Date;


public class XLogLoadTimeView extends XLogViewCommon implements TimeRangeDialog.ITimeRange, CalendarDialog.ILoadCalendarDialog {

	public static final String ID = XLogLoadTimeView.class.getName();
	
	long stime, etime;

	private int serverId;
	
	LoadXLogJob loadJob;

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

	public void createPartControl(Composite parent) {
		display = Display.getCurrent();
		shell = new Shell(display);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		
		create(parent, man);
		
		man.add(new Action("zoom in", ImageUtil.getImageDescriptor(Images.zoomin)) {
			public void run() {
				TimeRangeDialog dialog = new TimeRangeDialog(display, XLogLoadTimeView.this, DateUtil.yyyymmdd(stime));
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
		
	    // Add context menu
 		new MenuItem(contextMenu, SWT.SEPARATOR);
 	    MenuItem loadXLogItem = new MenuItem(contextMenu, SWT.PUSH);
 	    loadXLogItem.setText("Load History");
 	    loadXLogItem.addListener(SWT.Selection, new Listener() {
 			public void handleEvent(Event event) {
 				new OpenXLogLoadTimeAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), objType, Images.server, serverId, stime, etime).run();
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
	}
	
	private void createContextMenu(Composite parent, IMenuListener listener){
        MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(listener);
        Menu menu = contextMenu.createContextMenu(parent);
        canvas.setMenu(menu);
    }
	
	public void refresh(){
		viewPainter.build();
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				canvas.redraw();
			}
		});
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
	}

	public void setInput(long stime, long etime, String objType, int serverId) {
		this.stime = stime;
		this.etime = etime;
		this.objType = objType;
		this.serverId = serverId;
		
		setObjType(objType);
		viewPainter.setEndTime(etime);
		viewPainter.setTimeRange(etime - stime);

		String svrName = "";
		String objTypeDisplay = "";
		Server server = ServerManager.getInstance().getServer(serverId);
		if(server != null){
			svrName = server.getName();
			objTypeDisplay = server.getCounterEngine().getDisplayNameObjectType(objType);
		}
		
		this.setPartName("XLog - " + objTypeDisplay);
		setContentDescription("ⓢ"+svrName+" | "+objTypeDisplay+"\'s "+"XLog Pasttime"
						+ " | " + DateUtil.format(stime, "yyyy-MM-dd") + "(" + DateUtil.format(stime, "HH:mm")
						+ "~" + DateUtil.format(etime, "HH:mm") + ")");
		
		setDate(DateUtil.yyyymmdd(stime));
		
		try {
			loadJob = new LoadXLogJob();
			loadJob.schedule();
		} catch (Exception e) {
			MessageDialog.openError(shell, "Error", e.getMessage());
		}
	}
	
	public void setTimeRange(long stime, long etime) {
		if (viewPainter.zoomIn(stime, etime)) {
			canvas.redraw();
		}
	}

	public void setFocus() {
		super.setFocus();
		String statusMessage = "setInput(objType:"+objType+", serverId:"+serverId+ ", twdata size(): " + twdata.size() +")";
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		slManager.setMessage(statusMessage);
	}
	
	public void loadAdditinalData(long stime, long etime, final boolean reverse) {
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
			TcpProxy.putTcpProxy(tcp);
		}
	}

	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento = memento.createChild(ID);
		memento.putString("stime", String.valueOf(this.stime));
		memento.putString("etime", String.valueOf(this.etime));
		memento.putString("objType", objType);
	}
	
	AgentDailyListProxy agnetProxy = new AgentDailyListProxy();
	
	class LoadXLogJob extends Job{
		public LoadXLogJob() {
			super("XLog Loading...(" 
					+ DateUtil.format(stime, "yyyy-MM-dd")
					+ " "
					+ DateUtil.format(stime, "HH:mm") 
					+ "~" 
					+ DateUtil.format(etime, "HH:mm") + ")");
		}

		protected IStatus run(final IProgressMonitor monitor) {
			monitor.beginTask(ServerManager.getInstance().getServer(serverId).getName() + "....", IProgressMonitor.UNKNOWN);
			int limit = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_IGNORE_TIME);
			int max = getMaxCount();
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				twdata.clear();
				MapPack param = new MapPack();
				String date = DateUtil.yyyymmdd(stime);
				param.put("date", date);
				param.put("stime", stime);
				param.put("etime", etime);
				param.put("objHash", agnetProxy.getObjHashLv(date, serverId, objType));
			
				if (limit > 0) {
					param.put("limit", limit);
				}
				if (max > 0) {
					param.put("max", max);
				}
		
				ConsoleProxy.infoSafe("Load old XLog data");
				ConsoleProxy.infoSafe("stime :" + FormatUtil.print(new Date(stime), "yyyyMMdd HH:mm:ss.SSS"));
				ConsoleProxy.infoSafe("etime :" + FormatUtil.print(new Date(etime), "yyyyMMdd HH:mm:ss.SSS"));
				ConsoleProxy.infoSafe("objType :" + objType);
				ConsoleProxy.infoSafe("limit :" + limit + "   max:"+max);
				
				twdata.setMax(max);
				
				
				final	BooleanValue refreshFlag = new BooleanValue(true);
				new Thread(new Runnable(){
					public void run() {
						while(refreshFlag.value){
							refresh();
							ThreadUtil.sleep(2000);
						}
					}
				}).start();
				final DecimalValue count = new DecimalValue();
				tcp.process(RequestCmd.TRANX_LOAD_TIME_GROUP, param, new INetReader() {

					public void process(DataInputX in) throws IOException {
						Pack p = in.readPack();
						if (monitor.isCanceled()) {
							throw new IOException("User cancelled");
						}
						XLogPack x = XLogUtil.toXLogPack(p);
					
						twdata.putLast(x.txid, new XLogData(x, serverId));
						count.value++;
						
						if (count.value % 10000 == 0) {
						//	refresh();
							monitor.subTask(count.value + " XLog data received.");
						}
						
					}
				});
				
				refreshFlag.value=false;
				
			} catch (Throwable t) {
				ConsoleProxy.errorSafe(t.toString());
			} finally {
				TcpProxy.putTcpProxy(tcp);
				monitor.done();
			}
			refresh();
			return Status.OK_STATUS;
		}
	}

	public void onPressedOk(long startTime, long endTime) {
		setInput(startTime, endTime, objType, serverId);
	}

	public void onPressedOk(String date) {}

	public void onPressedCancel() {}

	public void dispose() {
		super.dispose();
		if (loadJob != null && (loadJob.getState() == Job.WAITING || loadJob.getState() == Job.RUNNING)) {
			loadJob.cancel();
		}
	}
}
