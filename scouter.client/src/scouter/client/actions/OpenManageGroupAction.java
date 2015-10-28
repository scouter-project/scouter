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
package scouter.client.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.popup.ManageGroupDialog;
import scouter.client.popup.ManageGroupDialog.IManageGroup;
import scouter.client.util.ImageUtil;

public class OpenManageGroupAction extends Action {
	public final static String ID = OpenManageGroupAction.class.getName();

	private final IWorkbenchWindow window;
	String groupName;
	String objType;
	IManageGroup callback;

	public OpenManageGroupAction(IWorkbenchWindow window, String groupName, String objType, IManageGroup callback) {
		this.window = window;
		this.groupName = groupName;
		this.objType = objType;
		this.callback = callback;
		setText("&Manage Group");
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.group_edit));
	}

	public void run() {
		if (window != null) {
			ManageGroupDialog dialog = new ManageGroupDialog(window.getShell().getDisplay(), groupName, objType, callback);
			dialog.show();
		}
	}
}
