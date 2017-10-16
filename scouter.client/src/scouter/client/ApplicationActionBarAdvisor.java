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
package scouter.client;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import scouter.client.actions.*;
import scouter.client.constants.MenuStr;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	protected void makeActions(IWorkbenchWindow window) {

		Server server = ServerManager.getInstance().getDefaultServer();
		int serverId = 0;
		if(server != null){
			serverId = server.getId();
		}

		// File
		register(new OpenClientThreadListAction(window, "Client Thread List", Images.thread));
		register(new OpenClientEnvViewAction(window));
		register(new OpenWorkspaceExplorerAction(window, "Workspace Explorer", Images.explorer, serverId));
		register(new ExportWorkspaceAction(window, "Export perspective settings", Images.explorer));
		register(new ImportWorkspaceAction(window, "Import perspective settings", Images.explorer));
		register(new RestartAction(window, "Restart"));
		
		// Management
		register(new AddServerAction(window, "Add Server", Images.add));
		register(new OpenServerManagerAction());
		register(new OpenObjectDashboardAction(window, "Object Dashboard"));
		register(new OpenConsoleAction(window, "Console"));
		register(new OpenAlertRealTimeAction(window, MenuStr.ALERT_REAL, Images.alert));
		register(new OpenGroupNavigationAction(window));
		
		// Window
		register(ActionFactory.RESET_PERSPECTIVE.create(window));
	}

	public IAction getAction(String id) {
		return super.getAction(id);
	}

	protected void fillMenuBar(IMenuManager menuBar) {
	}
}
