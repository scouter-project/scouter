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
package scouter.client.context.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Activator;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.RCPUtil;


public class SetDefaultServerAction extends Action {
	public final static String ID = SetDefaultServerAction.class.getName();

	IWorkbenchWindow win;
	Server server;

	public SetDefaultServerAction(IWorkbenchWindow window, Server server) {
		super("Set as Default Server");
		this.win = window;
		this.server = server;
	}

	public void run() {
		if(server != null) {
			ServerPrefUtil.storeDefaultServer(server.getIp()+":"+server.getPort());
			ServerManager.getInstance().setDefaultServer(server);
			IWorkbenchPage page = win.getActivePage();
			IPerspectiveDescriptor perspective = page.getPerspective();
			if (Activator.getDefault().isPrePerspective(perspective.getId())) {
				if (MessageDialog.openConfirm(win.getShell(), "Reset Perspectives", "Default server is changed. Would you reset current perspective?")) {
					RCPUtil.resetPerspective();
				}
			} else {
				MessageDialog.openInformation(win.getShell(), "Custom perspective", "Default server is changed. But custom perspective is not applied");
			}
		}
	}
}
