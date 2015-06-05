/*
 *  Copyright 2015 LG CNS.
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

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import scouter.client.actions.AddServerAction;
import scouter.client.actions.OpenAlertRealTimeAction;
import scouter.client.actions.OpenClientEnvViewAction;
import scouter.client.actions.OpenClientThreadListAction;
import scouter.client.actions.OpenConsoleAction;
import scouter.client.actions.OpenGroupNavigationAction;
import scouter.client.actions.OpenObjectDashboardAction;
import scouter.client.actions.OpenWorkspaceExplorerAction;
import scouter.client.actions.RestartAction;
import scouter.client.constants.MenuStr;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ImageUtil;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	private IWorkbenchAction exitAction;

	private IWorkbenchAction aboutAction;

	private IWorkbenchAction preferencesAction;

	private IContributionItem perspective;

	private AddServerAction addServerAction;
	
	private OpenClientThreadListAction openClientThreadListAction;
	
	private OpenWorkspaceExplorerAction openWorkspaceExplorerAction;
	
	private OpenGroupNavigationAction openGroupNavigationAction;
	
	private OpenClientEnvViewAction openClientEnvAction;

	private RestartAction openRestart;

	private OpenAlertRealTimeAction openAlertRealtimeAction;

	private OpenConsoleAction openConsole;
	private OpenObjectDashboardAction openObjDashboard;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	protected void makeActions(IWorkbenchWindow window) {

		Server server = ServerManager.getInstance().getDefaultServer();
		int serverId = 0;
		if(server != null){
			serverId = server.getId();
		}
		
		exitAction = ActionFactory.QUIT.create(window);
		exitAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.MENU_EXIT));
		register(exitAction);
		
		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);
		
		preferencesAction = ActionFactory.PREFERENCES.create(window);
		preferencesAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.preference));
		register(preferencesAction);
		
		register(addServerAction = new AddServerAction(window, "Add Server", Images.add));

		register(openClientThreadListAction = new OpenClientThreadListAction(window, "Client Thread List", Images.thread));
		register(openWorkspaceExplorerAction = new OpenWorkspaceExplorerAction(window, "Workspace Explorer", Images.explorer, serverId));
		register(openGroupNavigationAction = new OpenGroupNavigationAction(window));
		register(openClientEnvAction = new OpenClientEnvViewAction(window));
		register(openRestart = new RestartAction(window, "Restart"));

		perspective = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window);
		register(openAlertRealtimeAction = new OpenAlertRealTimeAction(window, MenuStr.ALERT_REAL, Images.alert));
		register(openConsole = new OpenConsoleAction(window, "Console"));
		register(openObjDashboard = new OpenObjectDashboardAction(window, "Object Dashboard"));
	}

	public IAction getAction(String id) {
		return super.getAction(id);
	}

	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
		MenuManager viewMenu = new MenuManager("&Views", "scouter.client.menu.views");
		MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
		MenuManager reportMenu = new MenuManager("&Report", "scouter.client.menu.report");

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		menuBar.add(windowMenu);
		menuBar.add(reportMenu);

		viewMenu.add(openObjDashboard);
		viewMenu.add(openConsole);
		viewMenu.add(openAlertRealtimeAction);

		MenuManager perspectiveMenu = new MenuManager("&Perspective", "scouter.client.menu.perspective");
		perspectiveMenu.add(perspective);

		windowMenu.add(perspectiveMenu);
		windowMenu.add(preferencesAction);

//		fileMenu.add(newWindowAction);
//		fileMenu.add(new Separator());
		fileMenu.add(addServerAction);
		fileMenu.add(new Separator());
		fileMenu.add(openClientThreadListAction);
		fileMenu.add(openClientEnvAction);
		fileMenu.add(new Separator());
		//fileMenu.add(openTerminal);
		fileMenu.add(openWorkspaceExplorerAction);
		fileMenu.add(openGroupNavigationAction);
		
		fileMenu.add(new Separator());
		
		fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		fileMenu.add(new Separator());
		fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

		fileMenu.add(openRestart);
		fileMenu.add(exitAction);
	}

	protected void fillCoolBar(ICoolBarManager coolBar) {
		// IToolBarManager toolbar = new ToolBarManager(SWT.FLAT);
		// coolBar.add(new ToolBarContributionItem(toolbar, "main"));
		// // toolbar.add(new Separator());
		// toolbar.add(clusterTest);
	}

	public void fillTrayItemContextMenu(IMenuManager menuManager) {
		menuManager.add(aboutAction);
		menuManager.add(exitAction);
	}
}
