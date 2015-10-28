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
import scouter.client.popup.AddGroupDialog;
import scouter.client.popup.AddGroupDialog.IAddGroup;
import scouter.client.util.ImageUtil;

public class OpenAddGroupAction extends Action {
	public final static String ID = OpenAddGroupAction.class.getName();

	private final IWorkbenchWindow window;
	IAddGroup callback;

	public OpenAddGroupAction(IWorkbenchWindow window, IAddGroup callback) {
		this.window = window;
		this.callback = callback;
		setText("&Add Group");
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.group_add));
	}

	public void run() {
		if (window != null) {
			AddGroupDialog dialog = new AddGroupDialog(window.getShell().getDisplay(), callback);
			dialog.show();
		}
	}
}
