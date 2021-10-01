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
import scouter.client.cubrid.views.CubridSingleRealTimeMultiView;
import scouter.client.cubrid.CubridSingleItem;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.util.DateUtil;

public class OpenMultiViewAction extends Action {

	final private int serverId;
	final private int multiViewOrdinal;

	public OpenMultiViewAction(int serverId, int objHash, int multiViewOrdinal) {
		this.serverId = serverId;
		this.multiViewOrdinal = multiViewOrdinal;
		setText(CubridSingleItem.values()[multiViewOrdinal].getTitle());
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.add));
	}

	public void run() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
					CubridSingleRealTimeMultiView.ID, serverId + "&" + "default" 
					+ "&" + multiViewOrdinal + "&" + DateUtil.MILLIS_PER_TEN_MINUTE, IWorkbenchPage.VIEW_ACTIVATE);
		} catch (PartInitException e) {
			ConsoleProxy.errorSafe(e.toString());
		}
	}
	
}
