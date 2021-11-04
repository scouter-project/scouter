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
package scouter.client.cubrid.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.cubrid.views.CubridLongTransactionList;
import scouter.client.cubrid.views.CubridRealtimeDmlView;
import scouter.client.cubrid.views.CubridServerInfoView;
import scouter.client.cubrid.views.CubridSpaceDbView;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;

public class OpenOtherViewAction extends Action {

	final private int serverId;
	final private OtherViewType viewType; 

	public OpenOtherViewAction(int serverId, int objHash, OtherViewType viewType) {
		this.serverId = serverId;
		this.viewType = viewType;
		setText(viewType.getTitle());
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.add));
	}

	public void run() {
		
		try {
			if (viewType.equals(OtherViewType.DB_SPACE_INFO)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CubridSpaceDbView.ID,
						serverId + "&" + "default", IWorkbenchPage.VIEW_ACTIVATE);
			} else if (viewType.equals(OtherViewType.LONG_TRANSACTION)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CubridLongTransactionList.ID,
						serverId + "&" + "default", IWorkbenchPage.VIEW_ACTIVATE);
			} else if (viewType.equals(OtherViewType.DML_REALTIME)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CubridRealtimeDmlView.ID,
						serverId + "&" + "default", IWorkbenchPage.VIEW_ACTIVATE);
			} else if (viewType.equals(OtherViewType.CUBRID_SERVERINFO)) {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CubridServerInfoView.ID,
						"" + serverId, IWorkbenchPage.VIEW_ACTIVATE);
			}
		} catch (PartInitException e) {
			ConsoleProxy.errorSafe(e.toString());
		}
	}
	
	public enum OtherViewType {
		DB_SPACE_INFO("DB Space Info"),
		LONG_TRANSACTION("Long Tranjaction List"), 
		DML_REALTIME("Realtime DML"),
		CUBRID_SERVERINFO("CUBRID ServerInfo");

		private String title;

		OtherViewType(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}
	
}
