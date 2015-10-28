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
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.counter.views.CounterLoadDateView;
import scouter.client.util.ImageUtil;

public class OpenLoadDateCounterAction extends Action {
	public final static String ID = OpenLoadDateCounterAction.class.getName();

	private final IWorkbenchWindow window;
	private String objType;
	private String counter;
	private String curDate;
	private int objHash;
	private String objName;
	private int serverId;

	public OpenLoadDateCounterAction(IWorkbenchWindow window, String label, String objType, String counter, Image image, String curDate, int objHash, String objName, int serverId) {
		this.window = window;
		this.objType = objType;
		this.counter = counter;
		this.curDate = curDate;
		this.objHash = objHash;
		this.objName = objName;
		this.serverId = serverId;
		
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}

	public void run() {
		if (window != null) {
			try {
				String subid = curDate + "-" + objHash + "-" + objType + counter;
				CounterLoadDateView v = (CounterLoadDateView) window.getActivePage().showView(
						CounterLoadDateView.ID, subid, IWorkbenchPage.VIEW_ACTIVATE);
				if (v != null) {
					try {
						v.setInput(curDate, objHash, objName, objType, counter, serverId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
}
