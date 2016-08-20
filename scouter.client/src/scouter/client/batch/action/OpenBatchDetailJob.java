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
package scouter.client.batch.action;

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
import scouter.lang.pack.BatchPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.Step;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

public class OpenBatchDetailJob extends Job {

	Display display;
	BatchPack pack;
	int serverId;
	String secId = "batchdetailview";
	
	public OpenBatchDetailJob(Display display, BatchPack pack, int serverId) {
		super("Load XLog Profile");
		this.display = display;
		this.pack = pack;
		this.serverId = serverId;
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Batch Detail....", IProgressMonitor.UNKNOWN);

		final BatchPack recvPack = (BatchPack)getPackData(serverId, pack);
		if(recvPack != null){
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
		}
		return Status.OK_STATUS;
	}
	
	private Pack getPackData(int serverId, BatchPack input) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objHash", input.objHash);
			param.put("time", input.startTime);
			param.put("position", input.position);
			Pack p = tcp.getSingle(RequestCmd.BATCH_HISTORY_DETAIL, param);
			if (p != null) {
				return p;
			}
		} catch (Throwable th) {
			ConsoleProxy.errorSafe(th.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
	
}
