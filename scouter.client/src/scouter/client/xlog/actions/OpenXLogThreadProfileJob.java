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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.model.XLogData;
import scouter.client.model.XLogProxy;
import scouter.client.util.ExUtil;
import scouter.client.xlog.views.XLogThreadProfileView;
import scouter.lang.step.Step;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

public class OpenXLogThreadProfileJob extends Job {

	final XLogData data;
	final long threadTxid;
	
	public OpenXLogThreadProfileJob(XLogData xlogData, long threadId) {
		super("Load XLog Thread Profile");
		this.data = xlogData;
		this.threadTxid = threadId;
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Thread Profile....", IProgressMonitor.UNKNOWN);
		String date = DateUtil.yyyymmdd(data.p.endTime);
		final Step[] steps =XLogProxy.getProfile(date, threadTxid, data.serverId);
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
				try {
					IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					XLogThreadProfileView view = (XLogThreadProfileView) win.getActivePage().showView(XLogThreadProfileView.ID, Hexa32.toString32(threadTxid),
							IWorkbenchPage.VIEW_ACTIVATE);
					view.setInput(data, steps, threadTxid, data.serverId);
				} catch (Exception ex) {
				}
			}
		});
		return Status.OK_STATUS;
	}
}
