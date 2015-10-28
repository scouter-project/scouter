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
package scouter.client.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.util.ImageUtil;
import scouter.client.views.WorkspaceExplorer;


public class OpenWorkspaceExplorerAction extends Action {
	public final static String ID = OpenWorkspaceExplorerAction.class.getName();

	private final IWorkbenchWindow window;
	private boolean autoRefresh;
	private int serverId;
	
	public OpenWorkspaceExplorerAction(IWorkbenchWindow window, String label, Image image, int serverId) {
		this.window = window;
		this.serverId = serverId;
		setText(label);
		setId(ID);
		setActionDefinitionId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}
	public OpenWorkspaceExplorerAction(IWorkbenchWindow window, String label, Image image, boolean autoRefresh, int serverId) {
		this.window = window;
		this.autoRefresh = autoRefresh;
		this.serverId = serverId;
		setText(label);
		setId(ID);
		setActionDefinitionId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}
	public void run() {
		if (window != null) {
			try {
				WorkspaceExplorer view = (WorkspaceExplorer) window.getActivePage().showView(WorkspaceExplorer.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
				if(autoRefresh && view != null){
					view.setInput(serverId);
				}
			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
	
}
