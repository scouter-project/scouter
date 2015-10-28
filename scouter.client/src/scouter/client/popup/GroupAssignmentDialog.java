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

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import scouter.client.Images;
import scouter.client.group.GroupManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.popup.AddGroupDialog.IAddGroup;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.UIUtil;

public class GroupAssignmentDialog implements IAddGroup {
	Display display;
	int serverId;
	int objHash;
	String objName;
	String objType;
	Table groupTable;
	IGroupAssign callback;

	Set<Integer> noSelectedSet = new HashSet<Integer>();
	Set<Integer> selectedSet = new HashSet<Integer>();

	public GroupAssignmentDialog(Display display, int serverId, int objHash, String objName, String objType, IGroupAssign callback) {
		this.display = display;
		this.serverId = serverId;
		this.objHash = objHash;
		this.objName = objName;
		this.objType = objType;
		this.callback = callback;
	}

	public void show() {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setLayout(new GridLayout(2, false));
		dialog.setText("Group Assignment");
		CLabel title = new CLabel(dialog, SWT.NONE);
		GridData gr = new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1);
		title.setLayoutData(gr);
		title.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		title.setImage(Images.getObjectIcon(objType, true, serverId));
		title.setText(objName);
		
		groupTable = new Table(dialog,  SWT.CHECK | SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, true);
		gr.widthHint = 250;
		gr.heightHint = 350;
		groupTable.setLayoutData(gr);
		
		Composite buttonComp = new Composite(dialog, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, true);
		buttonComp.setLayoutData(gr);
		buttonComp.setLayout(UIUtil.formLayout(3, 3));
		
		Button selectAllBtn = new Button(buttonComp, SWT.PUSH);
		selectAllBtn.setLayoutData(UIUtil.formData(null, -1, 0, 5, null, -1, null, -1, 100));
		selectAllBtn.setText("&Select All");
		selectAllBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem[] items = groupTable.getItems();
				for (TableItem item : items) {
					item.setChecked(true);
				}
			}
		});
		
		Button deselectAllBtn = new Button(buttonComp, SWT.PUSH);
		deselectAllBtn.setLayoutData(UIUtil.formData(null, -1, selectAllBtn, 5, null, -1, null, -1, 100));
		deselectAllBtn.setText("&Deselect All");
		deselectAllBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem[] items = groupTable.getItems();
				for (TableItem item : items) {
					item.setChecked(false);
				}
			}
		});
		
		Button newBtn = new Button(buttonComp, SWT.PUSH);
		newBtn.setLayoutData(UIUtil.formData(null, -1, deselectAllBtn, 10, null, -1, null, -1, 100));
		newBtn.setText("&New");
		newBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
				if (agent == null) {
					return;
				}
				Server server = ServerManager.getInstance().getServer(agent.getServerId());
				if (server == null) {
					server = ServerManager.getInstance().getDefaultServer();
				}
				String displayObjType = server.getCounterEngine().getDisplayNameObjectType(objType);
				AddGroupDialog addDialog = new AddGroupDialog(display, GroupAssignmentDialog.this, objType, displayObjType, server.getId());
				addDialog.show();
			}
		});
		
		Composite bottomComp = new Composite(dialog, SWT.NONE);
		bottomComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		bottomComp.setLayout(UIUtil.formLayout(3, 3));
		final Button cancelBtn = new Button(bottomComp, SWT.PUSH);
		cancelBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, 100, -5, null, -1, 100));
		cancelBtn.setText("&Cancel");
		cancelBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				dialog.close();
			}
		});
		
		final Button okBtn = new Button(bottomComp, SWT.PUSH);
		okBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, cancelBtn, -5, null, -1, 100));
		okBtn.setText("&Ok");
		okBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				TableItem[] items = groupTable.getItems();
				List<String> checked = new ArrayList<String>();
				for (TableItem item : items) {
					if (item.getChecked()) {
						checked.add(item.getText());
					}
				}
				callback.endGroupAssignment(objHash, checked.toArray(new String[checked.size()]));
				dialog.close();
			}
		});
		
		Set<String> groupSet = GroupManager.getInstance().getGroupsByType(objType);
		Set<String> assginedGroups = GroupManager.getInstance().getObjGroups(objHash);
		for (String group : groupSet) {
			TableItem item = new TableItem(groupTable, SWT.NONE);
			item.setText(group);
			item.setChecked(assginedGroups.contains(group));
		}
		
		sortTable(groupTable);
		
		dialog.pack();
		dialog.open();
	}

	private void sortTable(Table table) {
		TableItem[] items = table.getItems();
		Collator collator = Collator.getInstance(Locale.getDefault());
		for (int i = 1; i < items.length; i++) {
			String value1 = items[i].getText(0);
			for (int j = 0; j < i; j++) {
				String value2 = items[j].getText(0);
				if (collator.compare(value1, value2) < 0) {
					String text = items[i].getText(0);
					boolean checked = items[i].getChecked();
					items[i].dispose();
					TableItem item = new TableItem(table, SWT.NONE, j);
					item.setText(text);
					item.setChecked(checked);
					items = table.getItems();
					break;
				}
			}
		}
	}

	public boolean addedGroup(String objType, String name) {
		if (GroupManager.getInstance().addGroup(objType, name)) {
			TableItem item = new TableItem(groupTable, SWT.NONE);
			item.setText(name);
			sortTable(groupTable);
			return true;
		}
		return false;
	}
	
	public interface IGroupAssign {
		public void endGroupAssignment(int objHash, String[] groups);
	}
}
