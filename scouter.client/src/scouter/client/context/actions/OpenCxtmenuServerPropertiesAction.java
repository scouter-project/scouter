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
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.popup.ServerPropertiesDialog;

public class OpenCxtmenuServerPropertiesAction extends Action {
	public final static String ID = OpenCxtmenuServerPropertiesAction.class.getName();

	private final IWorkbenchWindow win;
	private int serverId;

	public OpenCxtmenuServerPropertiesAction(IWorkbenchWindow win, String label, int serverId) {
		this.win = win;
		this.serverId = serverId;
		setText(label);
	}
	
	public void run() {
		if (win != null) {
			Display display = Display.getCurrent();
			if (display == null) {
				display = Display.getDefault();
			}
			ServerPropertiesDialog dialog = new ServerPropertiesDialog(display, serverId);
			dialog.show();
		}
	}
}
