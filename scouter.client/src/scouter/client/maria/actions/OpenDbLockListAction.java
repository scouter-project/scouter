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
package scouter.client.maria.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.maria.views.DbLockListView;
import scouter.client.util.ConsoleProxy;

public class OpenDbLockListAction extends Action {
	
	final private int serverId;
	final private int objHash;
	final private long time;
	
	public OpenDbLockListAction(int serverId, int objHash, long time) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.time = time;
		setText("Lock List");
	}

	public void run() {
		try {
			DbLockListView view = (DbLockListView) PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow().getActivePage()
			.showView(DbLockListView.ID, serverId + "&" + objHash, IWorkbenchPage.VIEW_ACTIVATE);
			if (view != null) {
				view.setInput(time);
			}
		} catch (PartInitException e) {
			ConsoleProxy.errorSafe(e.toString());
		}
	}
}
