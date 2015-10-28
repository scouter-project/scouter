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
package scouter.client.heapdump.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.heapdump.views.HeapDumpListView;
import scouter.client.util.ImageUtil;

public class HeapDumpListAction extends Action {
	public final static String ID = HeapDumpListAction.class.getName();

	private final IWorkbenchWindow window;
	private String objName;
	private int objHash;
	private int serverId;
	
	public HeapDumpListAction(IWorkbenchWindow window, String label, String objName, int objHash, Image image, int serverId) {
		this.window = window;
		this.objName = objName;
		this.objHash = objHash;
		this.serverId = serverId;
		
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}

	public void run() {
		if (window != null) {
			try {
				HeapDumpListView v = (HeapDumpListView) window.getActivePage().showView(HeapDumpListView.ID, 
						""+objHash, IWorkbenchPage.VIEW_ACTIVATE);
				if (v != null) {
					v.setInput(objHash, objName, serverId);
				}
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
}
