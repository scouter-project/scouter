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
import org.eclipse.swt.widgets.Display;

import scouter.client.net.LoginMgr;
import scouter.client.net.LoginResult;
import scouter.client.popup.LoginDialog2;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.util.StringUtil;


public class OpenServerAction extends Action {
	public final static String ID = OpenServerAction.class.getName();

	private final int serverId;

	public OpenServerAction(int serverId) {
		super("&Open Server");
		this.serverId = serverId;
	}

	public void run() {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (StringUtil.isNotEmpty(server.getUserId()) && StringUtil.isNotEmpty(server.getPassword())) {
			LoginResult result = LoginMgr.silentLogin(server, server.getUserId(), server.getPassword());
			if (result.success == false) {
				ConsoleProxy.errorSafe(result.getErrorMessage());
			}
		} else {

			LoginDialog2 dialog = new LoginDialog2(Display.getDefault().getActiveShell(), null, LoginDialog2.TYPE_OPEN_SERVER, server.getIp() + ":" + server.getPort(), server.getSocksAddr());
			dialog.open();
		}
	}
}
