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
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.counter.views.CounterRealTimeView;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.util.Hexa32;

public class OpenRealTimeViewAction extends Action {
	public final static String ID = OpenRealTimeViewAction.class.getName();

	private final IWorkbenchWindow window;
	private String counter;
	private int objHash;
	private String objName;
	private String objType;
	private int serverId;

	public OpenRealTimeViewAction(IWorkbenchWindow window, String label, String counter, Image image, int objHash, String objName, String objType, int serverId) {
		this.window = window;
		this.counter = counter;
		this.objHash = objHash;
		this.objName = objName;
		this.objType = objType;
		this.serverId = serverId;

		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}

	public void run() {
		if (window != null) {
			try {
				CounterRealTimeView v = (CounterRealTimeView) window.getActivePage().showView(
						CounterRealTimeView.ID, Hexa32.toString32(objHash) + "," + objType + counter + serverId,
						IWorkbenchPage.VIEW_ACTIVATE);
				if (v != null) {
					try {
						v.setInput(objHash, objName, objType, counter, serverId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (PartInitException e) {
				ConsoleProxy.error(e.toString());
			}
		}
	}
}
