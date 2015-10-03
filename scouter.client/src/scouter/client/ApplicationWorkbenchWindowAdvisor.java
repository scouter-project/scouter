/*
 *  Copyright 2015 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import scouter.Version;
import scouter.client.net.LoginMgr;
import scouter.client.popup.LoginDialog;
import scouter.client.popup.LoginDialog.ILoginDialog;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.remote.CheckMyJob;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.AlertProxyThread;
import scouter.client.threads.SessionObserver;
import scouter.client.util.RCPUtil;

/*
 * ApplicationWorkbenchWindowAdvisor.preWindowOpen()
 * ApplicationWorkbenchWindowAdvisor.createActionBarAdvisor()
 * ApplicationActionBarAdvisor.makeActions()
 * ApplicationActionBarAdvisor.fillMenuBar()
 * ApplicationActionBarAdvisor.fillCoolBar()
 * ApplicationActionBarAdvisor.fillStatusLine()
 * ApplicationWorkbenchWindowAdvisor.postWindowOpen()
 * ApplicationActionBarAdvisor.dispose()
 * ApplicationWorkbenchWindowAdvisor.dispose()
 */

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor implements ILoginDialog{
	
	private ApplicationActionBarAdvisor actionBarAdvisor;
	Display display;
	boolean finish = false;
	
	private static final String[] hideActions = {
			"com.kyrsoft.stmemmon.actionSet",
			"org.eclipse.debug.ui.breakpointActionSet",
			"org.eclipse.debug.ui.debugActionSet",
			"org.eclipse.debug.ui.launchActionSet",
			"org.eclipse.debug.ui.profileActionSet",
			"org.eclipse.jdt.ui.A_OpenActionSet",
			"org.eclipse.jdt.ui.CodingActionSet",
			"org.eclipse.jdt.ui.JavaActionSet",
			"org.eclipse.jdt.ui.JavaElementCreationActionSet",
			"org.eclipse.jdt.ui.SearchActionSet",
			"org.eclipse.jdt.ui.text.java.actionSet.presentation",
			"org.eclipse.search.searchActionSet",
			"org.eclipse.team.ui.actionSet",
			"org.eclipse.ui.NavigateActionSet",
			"org.eclipse.ui.WorkingSetActionSet",
			"org.eclipse.ui.WorkingSetActionSet.toolbar",
			"org.eclipse.ui.WorkingSetModificationActionSet",
			"org.eclipse.ui.edit.text.actionSet.annotationNavigation",
			"org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo",
			"org.eclipse.ui.edit.text.actionSet.navigation",
			"org.eclipse.ui.edit.text.actionSet.presentation",
			"org.eclipse.update.ui.softwareUpdates",
			"org.eclipse.ui.actionSet.openFiles",
			"org.eclipse.ui.actionSet.keyBindings",
			"org.eclipse.ui.edit.text.actionSet.openExternalFile",
			"org.eclipse.ui.externaltools.ExternalToolsSet" };

	private static final String[] removePreferences = {
		"org.eclipse.ant.ui.AntPreferencePage",
		"org.eclipse.datatools.connectivity.ui.preferences.dataNode",
		"org.eclipse.debug.ui.DebugPreferencePage",
		"org.eclipse.help.ui.browsersPreferencePage",
		"org.eclipse.jdt.ui.preferences.JavaBasePreferencePage",
		"org.eclipse.pde.ui.MainPreferencePage",
		"org.eclipse.team.ui.TeamPreferences",
		"org.eclipse.ui.preferencePages.Workbench",
		"org.eclipse.equinox.security.ui.category"
		};
	
	private static final String[] removePerspectives = {
		"org.eclipse.debug.ui.DebugPerspective",
		"org.eclipse.jdt.ui.JavaPerspective",
		"org.eclipse.jdt.ui.JavaHierarchyPerspective",
		"org.eclipse.jdt.ui.JavaBrowsingPerspective",
		"org.eclipse.team.ui.TeamSynchronizingPerspective"
	};
	
	private static final String[] preLoadingPerspectives = {
															PerspectiveService.ID
															};

	public ApplicationWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		actionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
		return actionBarAdvisor;
	}

	
	IWorkbenchWindowConfigurer configurer;
	
	public void preWindowOpen() {
		configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1440, 900));
		configurer.setShowMenuBar(true);
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR,	true); 
		configurer.setShowProgressIndicator(true);
		configurer.setShowFastViewBars(false);
		configurer.setShowPerspectiveBar(true);
		
		display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}

		boolean autoLogined = false;
		String[] serverList = ServerPrefUtil.getStoredServerList();
		if (serverList != null && serverList.length > 0) {
			String[] autoList = ServerPrefUtil.getStoredAutoLoginServerList();
			HashSet<String> autoSet = new HashSet<String>();
			if (autoList != null && autoList.length > 0) {
				autoSet = new HashSet<String>(Arrays.asList(autoList));
			}
			String defaultSrv = ServerPrefUtil.getStoredDefaultServer();
			ServerManager manager = ServerManager.getInstance();
			for (String addr : serverList) {
				String[] iport = addr.split(":");
				if (iport == null || iport.length < 2) {
					continue;
				}
				Server server = new Server(iport[0], iport[1]);
				if (addr.equals(defaultSrv)) {
					manager.setDefaultServer(server);
				} else {
					manager.addServer(server);
				}
				if (autoSet.contains(addr)) {
					String accountInfo = ServerPrefUtil.getStoredAccountInfo(addr);
					if (accountInfo != null) {
						int index = accountInfo.indexOf(PreferenceConstants.P_SVR_DIVIDER);
						if (index > -1) {
							String id = accountInfo.substring(0, index);
							String pwd = accountInfo.substring(index + 1, accountInfo.length());
							boolean result = LoginMgr.silentLogin(server, id, pwd);
							if (result) {
								autoLogined = true;
							}
						}
					}
				}
			}
			if (autoLogined && autoSet.contains(defaultSrv) == false) {
				Set<Integer> openSet = manager.getOpenServerList();
				Integer[] array = openSet.toArray(new Integer[openSet.size()]);
				Server server = manager.getServer(array[0]);
				ServerPrefUtil.storeDefaultServer(server.getIp()+":"+server.getPort());
				ServerManager.getInstance().setDefaultServer(server);
			}
		}
		if (autoLogined == false) {
			LoginDialog dialog = new LoginDialog(display, this, LoginDialog.TYPE_STARTUP);
			dialog.show();
			while (!finish) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
		configurer.setTitle("Version - "+Version.getClientFullVersion() + "(" + TimeZone.getDefault().getDisplayName() + ")");
	}
	
	public void postWindowOpen() {
		super.postWindowOpen();
	}

	public void dispose() {
		super.dispose();
	}

	public void postWindowRestore() throws WorkbenchException {
		super.postWindowRestore();
	}

	public void postWindowCreate() {
		super.postWindowCreate();
		boolean isrcp = false;
		if (Platform.getProduct() != null) {
			if (Activator.PRODUCT_ID.equals(Platform.getProduct().getId())
					&& Activator.APPLICATION_ID.equals(Platform.getProduct()
							.getApplication())) {
				isrcp = true;
			}
		} else {
			if (Activator.APPLICATION_ID.equals(System
					.getProperty("eclipse.application"))) {
				isrcp = true;
			}
		}

		if (isrcp) {
			IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

			configurer.getWindow().addPerspectiveListener(
					new IPerspectiveListener() {

						public void perspectiveActivated(IWorkbenchPage page,
								IPerspectiveDescriptor perspective) {
							RCPUtil.hideActions(hideActions);
						}

						public void perspectiveChanged(IWorkbenchPage page,
								IPerspectiveDescriptor perspective,
								String changeId) {
						}
					});

			RCPUtil.hideActions(hideActions);
			RCPUtil.hidePreference(removePreferences);
			RCPUtil.preLoadingPerspective(preLoadingPerspectives);
			RCPUtil.hidePerspectives(removePerspectives);
			configurer.getWindow().getShell().setMaximized(true);
			startBackgroundJob();
		}
	}
	
	private void startBackgroundJob() {
		CheckMyJob.getInstance();
		SessionObserver.load();
		AlertProxyThread.getInstance();
	}

	public void onPressedCancel() {
		if(!finish){
    		System.exit(0);
    	}
	}

	public void onPressedOk(String serverAddr, int serverId) {
		Server server = ServerManager.getInstance().getServer(serverId);
		ServerPrefUtil.storeDefaultServer(server.getIp()+":"+server.getPort());
		ServerManager.getInstance().setDefaultServer(server);
		finish = true;
	}
}