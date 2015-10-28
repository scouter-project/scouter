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


import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.client.util.RCPUtil;


public class CloseServerAction extends Action {
	public final static String ID = CloseServerAction.class.getName();

	private final IWorkbenchWindow window;
	private final int serverId;

	public CloseServerAction(IWorkbenchWindow window, int serverId) {
		super("&Close Server");
		this.window = window;
		this.serverId = serverId;
	}

	public void run() {
		Server newDefault = null;
		if(ServerManager.getInstance().getDefaultServer().getId() == serverId){
			Set<Integer> serverIdSet = ServerManager.getInstance().getOpenServerList();
			Integer[] serverIds = serverIdSet.toArray(new Integer[serverIdSet.size()]);
			for(int i = 0 ; i < serverIds.length ; i++) {
				if(serverId != serverIds[i]){
					newDefault = ServerManager.getInstance().getServer(serverIds[i]);
					break;
				}
			}
		}
		
		Server server = ServerManager.getInstance().getServer(serverId);
		server.close();
		server.setOpen(false);
		if(newDefault != null){
			ConsoleProxy.infoSafe("Default Server Changed to \'"+newDefault.getName()+"\'");
			ServerManager.getInstance().setDefaultServer(newDefault);
			ServerPrefUtil.storeDefaultServer(newDefault.getIp()+":"+newDefault.getPort());
			if (MessageDialog.openConfirm(window.getShell(), "Reset Perspectives", "Default server is changed. Would you reset all perspective?")) {
				RCPUtil.resetPerspective();
			}
		}
	}
}
