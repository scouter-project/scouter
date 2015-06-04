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
package scouter.client.context.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import scouter.client.model.AgentDataProxy;

public class OpenCxtmenuResetCacheAction extends Action {
	public final static String ID = OpenCxtmenuResetCacheAction.class.getName();

	private int objHash;
	private int serverId;

	public OpenCxtmenuResetCacheAction(String label, int objHash, int serverId) {
		this.objHash = objHash;
		this.serverId = serverId;
		setText(label);
	}

	public void run() {
		AgentDataProxy.resetCache(objHash, serverId);
		MessageBox m = new MessageBox(Display.getDefault().getActiveShell());
		m.setMessage("Reset text caches in the agent");
		m.open();
	}
}
