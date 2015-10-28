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
package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.popup.SummaryDialog;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.MapPack;

public class OpenSummaryAction extends Action {
	public final static String ID = OpenSummaryAction.class.getName();

	private final IWorkbenchWindow win;
	private int objHash;
	private int serverId;

	public OpenSummaryAction(IWorkbenchWindow win, int serverId, int objHash) {
		this.win = win;
		this.objHash = objHash;
		this.serverId = serverId;
		setText("Summary");
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.table));
	}

	public void run() {
		if (win != null) {
			AgentObject ao = AgentModelThread.getInstance().getAgentObject(objHash);
			String title = ao.getDisplayName();
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			new SummaryDialog(serverId, param).show(title);
		}
	}
}
