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
package scouter.client.configuration.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.popup.AccountDialog;
import scouter.client.util.ImageUtil;
import scouter.lang.Account;


public class EditAccountAction extends Action {
	public final static String ID = EditAccountAction.class.getName();

	private final IWorkbenchWindow window;
	int serverId;
	Account account;
	
	public EditAccountAction(IWorkbenchWindow window, int serverId) {
		this(window, serverId, null);
	}
	
	public EditAccountAction(IWorkbenchWindow window, int serverId, Account account) {
		this.account = account;
		this.window = window;
		this.serverId = serverId;
		setText("Edit Account");
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.user_edit));
	}

	public void run() {
		if (window != null) {
			AccountDialog dialog = new AccountDialog(serverId, account);
			dialog.show(AccountDialog.EDIT_MODE);
		}
	}
}
