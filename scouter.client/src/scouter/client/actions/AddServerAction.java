/*
 *  Copyright 2015 the original author or authors.
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.popup.LoginDialog;
import scouter.client.util.ImageUtil;


public class AddServerAction extends Action {
	public final static String ID = AddServerAction.class.getName();

	private final IWorkbenchWindow window;

	public AddServerAction(IWorkbenchWindow window, String label, Image image) {
		this.window = window;
		setText(label);
		setId(ID);
		setActionDefinitionId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}
	
	LoginDialog dialog;

	public void run() {
		if (window != null) {
			dialog = new LoginDialog(window.getShell().getDisplay(), null, LoginDialog.TYPE_ADD_SERVER);
			dialog.show();
		}
	}
}
