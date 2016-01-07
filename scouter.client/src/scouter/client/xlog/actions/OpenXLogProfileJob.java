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
package scouter.client.xlog.actions;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.model.XLogProxy;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.xlog.XLogUtil;
import scouter.client.xlog.views.XLogProfileView;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.Step;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

public class OpenXLogProfileJob extends Job {

	Display display;
	XLogData data;
	String date;
	long txid;
	int serverId;
	String secId = "profileview";
	
	public OpenXLogProfileJob(Display display, String date, long txid) {
		super("Load XLog Profile");
		this.display = display;
		this.date = date;
		this.txid = txid;
	}
	
	public OpenXLogProfileJob(Display display, String date, long txid, int serverId) {
		super("Load XLog Profile");
		this.display = display;
		this.date = date;
		this.txid = txid;
		this.serverId =serverId;
	}
	
	public OpenXLogProfileJob(Display display, XLogData xlogData, int serverId) {
		super("Load XLog Profile");
		this.display = display;
		this.data = xlogData;
		this.date = DateUtil.yyyymmdd(xlogData.p.endTime);
		this.txid = xlogData.p.txid;
		this.serverId = serverId;
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Profile....", IProgressMonitor.UNKNOWN);
		if (data == null) {
			if (serverId == 0) {
				Set<Integer> serverSet = ServerManager.getInstance().getOpenServerList();
				for (int serverId : serverSet) {
					data = getXLogData(serverId, date, txid);
					if (data != null) {
						this.serverId = serverId;
						secId = Hexa32.toString32(txid);
						break;
					}
				}
			} else {
				data = getXLogData(serverId, date, txid);
				secId = Hexa32.toString32(txid);
			}
		}
		if (data == null) {
			ConsoleProxy.errorSafe("Cannot find : " + Hexa32.toString32(txid));
			return Status.CANCEL_STATUS;
		}
		
		final Step[] steps =XLogProxy.getProfile(date, txid, serverId);
		ExUtil.exec(display, new Runnable() {
			public void run() {
				try {
					IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					XLogProfileView view = (XLogProfileView) win.getActivePage().showView(XLogProfileView.ID, secId,
							IWorkbenchPage.VIEW_ACTIVATE);
					view.setInput(steps, data, data.serverId);	
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		return Status.OK_STATUS;
	}
	
	
	private XLogData getXLogData(int serverId, String date, long txid) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			param.put("txid", txid);
			Pack p = tcp.getSingle(RequestCmd.XLOG_READ_BY_TXID, param);
			if (p != null) {
				XLogPack xp = XLogUtil.toXLogPack(p);
				XLogData d = new XLogData(xp, serverId);
				d.objName = TextProxy.object.getLoadText(date, xp.objHash, serverId);
				d.serviceName = TextProxy.service.getLoadText(date, xp.service, serverId);
				return d;
			}
		} catch (Throwable th) {
			ConsoleProxy.errorSafe(th.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
}
