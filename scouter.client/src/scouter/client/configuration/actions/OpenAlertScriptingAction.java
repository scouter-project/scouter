/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.configuration.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import scouter.client.configuration.views.AlertScriptingView;
import scouter.client.util.ImageUtil;

public class OpenAlertScriptingAction extends Action {
	public final static String ID = OpenAlertScriptingAction.class.getName();

	private final IWorkbenchWindow window;
	private int serverId;
	private String familyName;
	private String counterName;
	private String counterDisplayName;

	public OpenAlertScriptingAction(IWorkbenchWindow window, String label, Image image, int serverId, String familyName, String counterName, String counterDisplayName) {
		this.window = window;
		this.serverId = serverId;
		this.familyName = familyName;
		this.counterName = counterName;
		this.counterDisplayName = counterDisplayName;
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}

	public void run() {
		if (window != null) {
			try {
				AlertScriptingView v = (AlertScriptingView) window.getActivePage().showView(AlertScriptingView.ID, serverId + "-c-" + counterName, IWorkbenchPage.VIEW_ACTIVATE);
				if(v!= null) {
					v.setInput(serverId, familyName, counterName, counterDisplayName);
				}
			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
}
