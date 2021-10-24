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

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import scouter.client.net.LoginMgr;
import scouter.client.net.LoginResult;
import scouter.client.popup.LoginDialog2;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ClientFileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.io.File.separator;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		Location instanceLocation = Platform.getInstanceLocation();
//		if(instanceLocation.isSet())
//			instanceLocation.release();
//		instanceLocation.set(new URL("file", null, System.getProperty("user.home") + "/scouter-workspace-test"), false);
		
		String workspaceRootName = instanceLocation.getURL().getFile();
		String importWorkingDirName = workspaceRootName + separator+ "import-working";

		try {
			ClientFileUtil.copy(new File(importWorkingDirName + separator + ClientFileUtil.XLOG_COLUMN_FILE),
					new File(workspaceRootName + separator + ClientFileUtil.XLOG_COLUMN_FILE));
			ClientFileUtil.copy(new File(importWorkingDirName + separator + ClientFileUtil.GROUP_FILE),
					new File(workspaceRootName + separator + ClientFileUtil.GROUP_FILE));
			ClientFileUtil.copy(new File(importWorkingDirName + separator + ClientFileUtil.WORKSPACE_METADATA_DIR),
					new File(workspaceRootName + separator + ClientFileUtil.WORKSPACE_METADATA_DIR));
		} catch (IOException e) {
			e.printStackTrace();
		}

		ClientFileUtil.deleteDirectory(new File(importWorkingDirName));

		Display display = PlatformUI.createDisplay();
		Object exitStrategy = IApplication.EXIT_OK;
		try {
			boolean loginSuccessed = loginAutomaticallyWhenAutoLoginEnabled();
			if (!loginSuccessed) {
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
		LoginDialog2 dialog = new LoginDialog2(display.getActiveShell(), (serverAddr, serverId) -> {
			Server server = ServerManager.getInstance().getServer(serverId);
			ServerPrefUtil.storeDefaultServer(server.getIp()+":"+server.getPort());
			ServerManager.getInstance().setDefaultServer(server);
		}, LoginDialog2.TYPE_STARTUP, null, null);
		return (dialog.open() == Window.OK);
	}

	private boolean loginAutomaticallyWhenAutoLoginEnabled() {
		boolean autoLogined = false;
		String[] serverList = ServerPrefUtil.getStoredServerList();
		if (serverList != null && serverList.length > 0) {
			String[] autoList = ServerPrefUtil.getStoredAutoLoginServerList();
			HashSet<String> autoSet = new HashSet<>();
			if (autoList != null && autoList.length > 0) {
				autoSet = new HashSet<>(Arrays.asList(autoList));
			}
			String defaultSrv = ServerPrefUtil.getStoredDefaultServer();
			ServerManager manager = ServerManager.getInstance();
			for (String addr : serverList) {
				String[] iport = addr.split(":");
				if (iport.length < 2) {
					continue;
				}
				
				String socksIp = null;
				String socksPort = null;
				if (ServerPrefUtil.isSocksLogin(addr)) {
					String socksAddr = ServerPrefUtil.getStoredSocksServer(addr);
					String[] socksAddrs = socksAddr.split(":");
					socksIp = socksAddrs[0];
					socksPort = socksAddrs[1];
				}
				
				Server server = new Server(iport[0], iport[1], null, socksIp, socksPort);
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
							String pwd = accountInfo.substring(index + 1);
							LoginResult result = LoginMgr.silentLogin(server, id, pwd);
							if (result.success) {
								autoLogined = true;
							}
						}
					}
				}
			}
			if (autoLogined && !autoSet.contains(defaultSrv)) {
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
		display.syncExec(() -> {
			if (!display.isDisposed())
				workbench.close();
		});
	}
}
