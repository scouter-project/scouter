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

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import scouter.client.net.LoginMgr;
import scouter.client.net.LoginResult;
import scouter.client.popup.LoginDialog2;
import scouter.client.popup.LoginDialog2.ILoginDialog;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		Object exitStrategy = IApplication.EXIT_OK;
		try {
			boolean loginSuccessed = loginAutomaticallyWhenAutoLoginEnabled();
			if (loginSuccessed == false) {
				loginSuccessed = openLoginDialog(display);
			}
			if (loginSuccessed) {
				exitStrategy = createAndRunWorkbench(display);
			}
			return exitStrategy;
		} finally {
			display.dispose();
		}
	}

	private boolean openLoginDialog(Display display) {
		LoginDialog2 dialog = new LoginDialog2(display.getActiveShell(), new ILoginDialog() {
			@Override
			public void loginSuccess(String serverAddr, int serverId) {
				Server server = ServerManager.getInstance().getServer(serverId);
				ServerPrefUtil.storeDefaultServer(server.getIp()+":"+server.getPort());
				ServerManager.getInstance().setDefaultServer(server);						
			}
			
		}, LoginDialog2.TYPE_STARTUP, null);
		return (dialog.open() == Window.OK);
	}

	private boolean loginAutomaticallyWhenAutoLoginEnabled() {
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
		return autoLogined;
	}

	private Object createAndRunWorkbench(Display display) {
		int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
		if (returnCode == PlatformUI.RETURN_RESTART)
			return IApplication.EXIT_RESTART;
		else
			return IApplication.EXIT_OK;
	}

	public void stop() {
		if (!PlatformUI.isWorkbenchRunning())
			return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
	}
}
