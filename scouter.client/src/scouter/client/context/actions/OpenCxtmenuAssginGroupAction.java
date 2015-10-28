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
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.popup.GroupAssignmentDialog;
import scouter.client.popup.GroupAssignmentDialog.IGroupAssign;
import scouter.client.util.ImageUtil;

public class OpenCxtmenuAssginGroupAction extends Action {
	
	public final static String ID = OpenCxtmenuAssginGroupAction.class.getName();

	private final IWorkbenchWindow win;
	private final int serverId;
	private final int objHash;
	private final String objName;
	private final String objType;
	private final IGroupAssign callback;
	
	public OpenCxtmenuAssginGroupAction(IWorkbenchWindow win, int serverId, int objHash, String objName, String objType, IGroupAssign callback) {
		this.win = win;
		this.serverId = serverId;
		this.objHash = objHash;
		this.objName = objName;
		this.objType = objType;
		this.callback = callback;
		setText("&Assign Groups");
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.group_go));
	}

	public void run() {
		if (win != null) {
			GroupAssignmentDialog dialog = new GroupAssignmentDialog(win.getShell().getDisplay(), serverId, objHash, objName, objType, callback);
			dialog.show();
		}
	}
}
