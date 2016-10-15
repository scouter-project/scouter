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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import scouter.Version;
import scouter.client.net.LoginMgr;
import scouter.client.net.LoginResult;
import scouter.client.popup.LoginDialog2.ILoginDialog;
import scouter.client.popup.LoginDialog2;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.remote.CheckMyJob;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.AlertProxyThread;
import scouter.client.threads.SessionObserver;

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
		//PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR,	true); 
		configurer.setShowProgressIndicator(true);
		//configurer.setShowFastViewBars(false);
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
							LoginResult result = LoginMgr.silentLogin(server, id, pwd);
							if (result.success) {
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
			LoginDialog2 dialog = new LoginDialog2(display.getActiveShell(), this, LoginDialog2.TYPE_STARTUP, null);
			if (dialog.open() == Window.OK) {
				
			} else {
				System.exit(0);
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
			configurer.getWindow().getShell().setMaximized(true);
			startBackgroundJob();
		}
	}
	
	private void startBackgroundJob() {
		CheckMyJob.getInstance();
		SessionObserver.load();
		AlertProxyThread.getInstance();
	}

	@Override
	public void loginSuccess(String serverAddr, int serverId) {
		Server server = ServerManager.getInstance().getServer(serverId);
		ServerPrefUtil.storeDefaultServer(server.getIp()+":"+server.getPort());
		ServerManager.getInstance().setDefaultServer(server);
	}
}