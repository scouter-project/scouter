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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.group.GroupManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.UIUtil;

public class ManageGroupDialog {
	Display display;
	String groupName;
	String objType;
	IManageGroup callback;

	Set<Integer> noSelectedSet = new HashSet<Integer>();
	Set<Integer> selectedSet = new HashSet<Integer>();
	Set<Integer> currentAllSet = new HashSet<Integer>();

	public ManageGroupDialog(Display display, String groupName, String objType,
			IManageGroup callback) {
		this.display = display;
		this.groupName = groupName;
		this.objType = objType;
		this.callback = callback;
	}

	public void show() {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setLayout(new GridLayout(3, false));
		dialog.setText("Manage Group");
		
		CLabel title = new CLabel(dialog, SWT.NONE);
		GridData gr = new GridData(SWT.LEFT, SWT.CENTER, true, true, 3, 1);
		title.setLayoutData(gr);
		title.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		title.setImage(Images.getObjectIcon(objType, true, 0));
		title.setText(groupName + "(" +  getDisplayObjtype(objType) + ")");

		Composite searchComp = new Composite(dialog, SWT.NONE);
		searchComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		searchComp.setLayout(new GridLayout(2, false));

		CLabel searchLabel = new CLabel(searchComp, SWT.NONE);
		searchLabel.setText("Search:");
		final Text searchText = new Text(searchComp, SWT.BORDER);
		searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		searchText.setMessage("Enter keyword to filter...");

		new CLabel(dialog, SWT.NONE);
		new CLabel(dialog, SWT.NONE);

		final Table allObjects = new Table(dialog, SWT.BORDER | SWT.MULTI);
		gr = new GridData(SWT.FILL, SWT.FILL, true, true);
		gr.widthHint = 375;
		gr.heightHint = 450;
		allObjects.setLayoutData(gr);

		Composite centerComp = new Composite(dialog, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, true, true);
		gr.widthHint = 150;
		centerComp.setLayoutData(gr);
		centerComp.setLayout(new GridLayout(1, true));

		Button addBtn = new Button(centerComp, SWT.PUSH);
		addBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		addBtn.setImage(Images.arrow_right);
		addBtn.setText("Add");

		Button removeBtn = new Button(centerComp, SWT.PUSH);
		removeBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		removeBtn.setImage(Images.arrow_left);
		removeBtn.setText("Remove");

		final Table selectedObjects = new Table(dialog, SWT.BORDER | SWT.MULTI);
		gr = new GridData(SWT.FILL, SWT.FILL, true, true);
		gr.widthHint = 375;
		gr.heightHint = 450;
		selectedObjects.setLayoutData(gr);
		
		addBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				TableItem[] items = allObjects.getSelection();
				if (items == null || items.length < 1) {
					return;
				}
				for (int i = 0; i < items.length; i++) {
					TableItem item = items[i];
					int objHash = (Integer) item.getData();
					currentAllSet.remove(objHash);
					TableItem newItem = new TableItem(selectedObjects, SWT.NONE);
					newItem.setData(item.getData());
					newItem.setText(item.getText());
					newItem.setImage(item.getImage());
					item.dispose();
				}
				String filter = searchText.getText();
				refreshTable(allObjects, currentAllSet, filter);
				sortTable(selectedObjects);
			}
		});
		
		removeBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				TableItem[] items = selectedObjects.getSelection();
				if (items == null || items.length < 1) {
					return;
				}
				for (int i = 0; i < items.length; i++) {
					TableItem item = items[i];
					int objHash = (Integer) item.getData();
					currentAllSet.add(objHash);
					item.dispose();
				}
				String filter = searchText.getText();
				refreshTable(allObjects, currentAllSet, filter);
			}
		});
		
		Composite bottomComp = new Composite(dialog, SWT.NONE);
		bottomComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
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
				if (callback == null) {
					return;
				}
				TableItem[] selectedItems = selectedObjects.getItems();
				List<Integer> addedObjHashs = new ArrayList<Integer>();
				for (TableItem item : selectedItems) {
					int objHash = (Integer) item.getData();
					if (selectedSet.contains(objHash) == false) {
						addedObjHashs.add(objHash);
					}
				}
				List<Integer> removedObjHashs = new ArrayList<Integer>();
				for (Integer objHash : currentAllSet) {
					if (noSelectedSet.contains(objHash) == false) {
						removedObjHashs.add(objHash);
					}
				}
				callback.setResult(groupName, toIntArray(addedObjHashs), toIntArray(removedObjHashs));
				dialog.close();
			}
		});
		
		Map<Integer, AgentObject> agentMap = AgentModelThread.getInstance().getAgentObjectMap();
		Object[] keys = agentMap.keySet().toArray();
		for (Object key : keys) {
			if (key instanceof Integer) {
				int objHash = (Integer) key;
				AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
				if (agent.getObjType().equals(objType)) {
					Set<String> groupSet = GroupManager.getInstance().getObjGroups(objHash);
					if (groupSet.contains(groupName)) {
						selectedSet.add(objHash);
					} else {
						noSelectedSet.add(objHash);
						currentAllSet.add(objHash);
					}
				}
			}
		}

		refreshTable(allObjects, currentAllSet, null);

		for (Integer objHash : selectedSet) {
			AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
			if (agent == null) {
				continue;
			}
			TableItem item = new TableItem(selectedObjects, SWT.NONE);
			item.setData(objHash);
			item.setText(agent.getObjName() + "(" + ServerManager.getInstance().getServer(agent.getServerId()).getName() + ")");
			item.setImage(agent.isAlive() ? Images.active : Images.dead);
		}
		sortTable(selectedObjects);

		searchText.addListener(SWT.Modify, new Listener(){
			public void handleEvent(Event event) {
				String filter = searchText.getText();
				refreshTable(allObjects, currentAllSet, filter);
			}
		});
		
		dialog.pack();
		dialog.open();
	}

	private String getDisplayObjtype(String objType) {
		Iterator<Integer> itr = ServerManager.getInstance().getOpenServerList()
				.iterator();
		while (itr.hasNext()) {
			Server server = ServerManager.getInstance().getServer(itr.next());
			String displayObjType = server.getCounterEngine()
					.getDisplayNameObjectType(objType);
			if (displayObjType != null) {
				return displayObjType;
			}
		}
		return objType;
	}

	public int[] toIntArray(List<Integer> list) {
		int[] ret = new int[list.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = list.get(i);
		}
		return ret;
	}

	private void refreshTable(Table table, Set<Integer> objHashSet, String filter) {
		table.removeAll();
		for (Integer objHash : objHashSet) {
			AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
			if (agent == null) {
				continue;
			}
			String displayText = agent.getObjName() + "(" + ServerManager.getInstance().getServer(agent.getServerId()).getName() + ")";
			if (filter == null || filter.isEmpty() || displayText.toLowerCase().contains(filter.toLowerCase())) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setData(objHash);
				item.setText(displayText);
				item.setImage(agent.isAlive() ? Images.active : Images.dead);
			}
		}
		sortTable(table);
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
					Object data = items[i].getData();
					Image image = items[i].getImage();
					items[i].dispose();
					TableItem item = new TableItem(table, SWT.NONE, j);
					item.setText(text);
					item.setData(data);
					item.setImage(image);
					items = table.getItems();
					break;
				}
			}
		}
	}

	public interface IManageGroup {
		public void setResult(String groupName, int[] addObjHashs,
				int[] removeObjHashs);
	}
}
