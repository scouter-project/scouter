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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.util.ImageUtil;
import scouter.client.visitor.views.VisitorLoadView;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;

public class OpenVisitorLoadAction extends Action {
	public final static String ID = OpenVisitorLoadAction.class.getName();

	private final IWorkbenchWindow win;
	private int objHash;
	private int serverId;
	private String date;

	public OpenVisitorLoadAction(IWorkbenchWindow win, int serverId, String date, int objHash) {
		this.win = win;
		this.objHash = objHash;
		this.serverId = serverId;
		this.date = date;
		setText("Load");
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.calendar));
	}

	public void run() {
		if (win != null) {
			try {
				VisitorLoadView view = (VisitorLoadView) win.getActivePage().showView(
						VisitorLoadView.ID, serverId + "&" + date + "&" + objHash, IWorkbenchPage.VIEW_ACTIVATE);
				if (view != null) {
					MapPack param = new MapPack();
					param.put("date", date);
					param.put("objHash", this.objHash);
					String objName = TextProxy.object.getText(objHash);
					view.setInput(date + " - " + objName, RequestCmd.VISITOR_LOADDATE, param);
				}
			} catch (PartInitException e) {
				MessageDialog.openError(win.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
}
