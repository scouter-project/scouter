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
package scouter.client.popup;

import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.group.GroupManager;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ImageCombo;
import scouter.client.util.UIUtil;

public class AddGroupDialog {
	Display display;
	IAddGroup callback;
	String objType;
	String displayObjType;
	int serverId;
	
	public AddGroupDialog(Display display, IAddGroup callback) {
		this.display = display;
		this.callback = callback;
	}
	
	public AddGroupDialog(Display display, IAddGroup callback, String objType, String displayObjType, int serverId) {
		this(display, callback);
		this.objType = objType;
		this.displayObjType = displayObjType;
		this.serverId = serverId;
	}
	
	public void show() {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setLayout(new GridLayout(2, false));
		dialog.setText("Add Group");
		Label title = new Label(dialog, SWT.NONE);
		title.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		title.setText("Add group with object type");
		GridData gr = new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1);
		gr.widthHint = 350;
		title.setLayoutData(gr);
		Label label1 = new Label(dialog, SWT.NONE);
		label1.setText("Object Type");
		label1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
		final ImageCombo typeCombo = new ImageCombo(dialog, SWT.READ_ONLY | SWT.BORDER);
		typeCombo.setBackground(ColorUtil.getInstance().getColor("white"));
		if (objType == null) {
			Set<Integer> serverSet = ServerManager.getInstance().getOpenServerList();
			for (int serverId : serverSet) {
				Server server = ServerManager.getInstance().getServer(serverId);
				List<String> objTypeList = server.getCounterEngine().getAllObjectType();
				for (String objType : objTypeList) {
					String displayObjType = server.getCounterEngine().getDisplayNameObjectType(objType);
					if (typeCombo.getData(displayObjType) == null) {
						typeCombo.add(displayObjType, Images.getObjectIcon(objType, true, serverId));
						typeCombo.setData(displayObjType, objType);
					}
				}
			}
		} else {
			typeCombo.add(displayObjType, Images.getObjectIcon(objType, true, this.serverId));
			typeCombo.setData(displayObjType, objType);
			typeCombo.setEnabled(false);
		}
		typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		if (typeCombo.getItemCount() > 0) {
			typeCombo.select(0);
		}
		Label label2 = new Label(dialog, SWT.NONE);
		label2.setText("Group Name");
		label2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
		final Text name = new Text(dialog, SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Button okBtn = new Button(dialog, SWT.PUSH);
		okBtn.setText("&Ok");
		gr = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
		gr.widthHint = 100;
		okBtn.setLayoutData(gr);
		okBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (callback == null) {
					return;
				}
				String selectedType = (String) typeCombo.getData(typeCombo.getText());
				String groupName = name.getText();
				List<String> groups = GroupManager.getInstance().listGroup();
				if (groups.contains(groupName)) {
					MessageDialog.openWarning(dialog, "Duplicated Name", groupName + " is already exist.");
				} else {
					if (callback.addedGroup(selectedType, groupName)) {
						dialog.close();
					} else {
						MessageDialog.openError(dialog, "Failed", "Inappropriate name.");
					}
				}
			}
		});
		dialog.setDefaultButton(okBtn);
		dialog.pack();
		dialog.open();
	}
	
	public interface IAddGroup {
		public boolean addedGroup(String objType, String name);
	}
}
