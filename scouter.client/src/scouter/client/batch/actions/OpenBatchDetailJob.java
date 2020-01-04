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
package scouter.client.batch.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.batch.views.BatchDetailView;
import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.lang.pack.BatchPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;

public class OpenBatchDetailJob extends Job {

	Display display;
	BatchPack pack;
	int serverId;
	String secId = "batchdetailview";
	
	public OpenBatchDetailJob(Display display, BatchPack pack, int serverId) {
		super("Load Batch History Detail");
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
						BatchDetailView view = (BatchDetailView) win.getActivePage().showView(BatchDetailView.ID, secId, IWorkbenchPage.VIEW_ACTIVATE);
						view.setInput(recvPack, serverId);	
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
			param.put("startTime", input.startTime);
			param.put("elapsedTime", input.elapsedTime);
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
