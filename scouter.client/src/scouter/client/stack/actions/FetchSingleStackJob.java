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
 */
package scouter.client.stack.actions;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.util.ExUtil;
import scouter.client.views.ObjectThreadDumpView;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.StackPack;
import scouter.net.RequestCmd;

public class FetchSingleStackJob extends Job {

	int serverId;
	String objName;
	long time;
	List<Long> list;
	ObjectThreadDumpView view;
	
	public FetchSingleStackJob(int serverId, String objName, long time, List<Long> list, ObjectThreadDumpView view) {
		super(objName + " stack ...");
		this.serverId = serverId;
		this.objName = objName;
		this.time = time;
		this.list = list;
		this.view = view;
	}

	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask("Fetching...", 1);
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		final StringBuilder content = new StringBuilder();
		try {
			MapPack param = new MapPack();
			param.put("objName", objName);
			param.put("from", time);
			param.put("to", time + 1);
			tcp.process(RequestCmd.GET_STACK_ANALYZER, param, new INetReader() {
				int count = 0;
				public void process(DataInputX in) throws IOException {
					StackPack sp = (StackPack) in.readPack();
					content.append(sp.getStack());
					monitor.worked(count++);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		monitor.done();
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
		    	IWorkbench workbench = PlatformUI.getWorkbench();
		    	IWorkbenchWindow window = workbench.getActiveWorkbenchWindow(); 				
				if (window != null) {
					try {
						if(view == null){
							ObjectThreadDumpView newView = (ObjectThreadDumpView) window.getActivePage().showView(ObjectThreadDumpView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
							if (newView != null) {
								newView.setInput(content.toString(), objName, serverId, time, list);
							}
						}else{
							view.setInput(content.toString());
						}
					} catch (PartInitException e) {
						MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
					}
				}
			}
		});
		
		return Status.OK_STATUS;
	}
}
