/*
 *  Copyright 2016 the original author or authors. 
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

import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.client.views.ObjectThreadDumpView;
import scouter.lang.pack.MapPack;
import scouter.lang.value.TextValue;
import scouter.net.RequestCmd;

public class OpenBatchActiveStackJob extends Job {

	private String key;
	private int serverId;
	private int objHash;
	
	
	public OpenBatchActiveStackJob(String key, int objHash, int serverId) {
		super("Load Batch Active Stack");
		this.key = key;
		this.serverId = serverId;
		this.objHash = objHash;
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Batch Stack....", IProgressMonitor.UNKNOWN);

		final String stackText = getStackData(serverId, objHash, key);
		if(stackText == null){
			return Status.CANCEL_STATUS;
		}
			
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
				try {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					ObjectThreadDumpView view = (ObjectThreadDumpView) window.getActivePage().showView(ObjectThreadDumpView.ID, objHash + "&" + TimeUtil.getCurrentTime(serverId), IWorkbenchPage.VIEW_ACTIVATE);
					if (view != null) {
						view.setInput(stackText);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});			
		
		return Status.OK_STATUS;
	}
	
	private String getStackData(int serverId, int objHash, String key) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		String stack = null;
		try {
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			param.put("key", key);

			MapPack map = (MapPack)tcp.getSingle(RequestCmd.BATCH_ACTIVE_STACK, param);
			if(map == null){
				return null;
			}
			stack = map.getText("stack");
		} catch (Throwable th) {
			ConsoleProxy.errorSafe(th.toString());
			return null;
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return stack;
	}
}
