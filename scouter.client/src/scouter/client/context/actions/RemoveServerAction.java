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


public class RemoveServerAction extends Action {
	public final static String ID = RemoveServerAction.class.getName();

	private final IWorkbenchWindow window;
	private final int serverId;

	public RemoveServerAction(IWorkbenchWindow window, int serverId) {
		super("&Remove");
		this.window = window;
		this.serverId = serverId;
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.server_delete));
	}

	public void run() {
		if (window != null) {
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
			
			boolean ok = MessageDialog.openQuestion(window.getShell(), "Remove Server",
					"The selected server will disappear on your window. "+(newDefault != null?"\'"+newDefault.getName()+"\' will be Default.":"")+" \n\nContinue?");
			if (ok) {
				Server server = ServerManager.getInstance().getServer(serverId);
				ServerPrefUtil.removeServerAddr(server.getIp() + ":" + server.getPort());
				ServerManager.getInstance().removeServer(serverId);
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
	}
}
