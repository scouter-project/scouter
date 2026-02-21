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
package scouter.client.workspace;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SwitchWorkspaceDialog extends TitleAreaDialog {

	private Text pathText;
	private Table workspaceTable;
	private String selectedPath;
	private final String currentWorkspacePath;

	public SwitchWorkspaceDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		currentWorkspacePath = WorkspaceManager.getInstance().getCurrentWorkspacePath();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Switch Workspace");
		newShell.setSize(600, 450);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Switch Workspace");
		setMessage("Select a workspace to switch to. The application will restart.");

		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		container.setLayout(new GridLayout(3, false));

		Label pathLabel = new Label(container, SWT.NONE);
		pathLabel.setText("Workspace:");

		pathText = new Text(container, SWT.BORDER);
		pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("..");
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Select Workspace Directory");
				dialog.setMessage("Select the workspace directory");
				String dir = dialog.open();
				if (dir != null) {
					pathText.setText(dir);
				}
			}
		});

		Label tableLabel = new Label(container, SWT.NONE);
		tableLabel.setText("Recent Workspaces:");
		GridData tableLabelGd = new GridData();
		tableLabelGd.horizontalSpan = 3;
		tableLabel.setLayoutData(tableLabelGd);

		workspaceTable = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		workspaceTable.setHeaderVisible(true);
		workspaceTable.setLinesVisible(true);
		GridData tableGd = new GridData(GridData.FILL_BOTH);
		tableGd.horizontalSpan = 2;
		workspaceTable.setLayoutData(tableGd);

		TableColumn nameCol = new TableColumn(workspaceTable, SWT.NONE);
		nameCol.setText("Name");
		nameCol.setWidth(120);

		TableColumn pathCol = new TableColumn(workspaceTable, SWT.NONE);
		pathCol.setText("Path");
		pathCol.setWidth(250);

		TableColumn lastUsedCol = new TableColumn(workspaceTable, SWT.NONE);
		lastUsedCol.setText("Last Used");
		lastUsedCol.setWidth(150);

		workspaceTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = workspaceTable.getSelectionIndex();
				if (idx >= 0) {
					TableItem item = workspaceTable.getItem(idx);
					String path = item.getData("path").toString();
					if (!isCurrentWorkspace(path)) {
						pathText.setText(path);
					}
				}
			}
		});

		Composite buttonPanel = new Composite(container, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		Button newButton = new Button(buttonPanel, SWT.PUSH);
		newButton.setText("New...");
		newButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		newButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewWorkspace();
			}
		});

		Button removeButton = new Button(buttonPanel, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelectedWorkspace();
			}
		});

		refreshTable();

		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Switch", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		String path = pathText.getText().trim();
		if (path.isEmpty()) {
			setErrorMessage("Please enter or select a workspace path.");
			return;
		}
		if (isCurrentWorkspace(path)) {
			setErrorMessage("This is the current workspace. Please select a different one.");
			return;
		}
		File dir = new File(path);
		if (!dir.exists()) {
			boolean create = MessageDialog.openQuestion(getShell(), "Create Workspace",
					"The directory does not exist. Create it?");
			if (create) {
				if (!dir.mkdirs()) {
					setErrorMessage("Failed to create directory: " + path);
					return;
				}
			} else {
				return;
			}
		}
		WorkspaceManager.getInstance().addWorkspace(path, dir.getName());
		WorkspaceManager.getInstance().setLastUsed(path);
		selectedPath = path;
		super.okPressed();
	}

	public String getSelectedPath() {
		return selectedPath;
	}

	private void refreshTable() {
		workspaceTable.removeAll();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<WorkspaceInfo> list = WorkspaceManager.getInstance().getWorkspaceList();
		String normalizedCurrent = normalizePath(currentWorkspacePath);
		for (WorkspaceInfo info : list) {
			TableItem item = new TableItem(workspaceTable, SWT.NONE);
			String name = info.getDisplayName();
			boolean isCurrent = normalizePath(info.getPath()).equals(normalizedCurrent);
			if (isCurrent) {
				name = name + " (current)";
			}
			item.setText(0, name);
			item.setText(1, info.getPath());
			item.setText(2, info.getLastUsed() > 0 ? sdf.format(new Date(info.getLastUsed())) : "");
			item.setData("path", info.getPath());
			if (isCurrent) {
				item.setGrayed(true);
			}
		}
	}

	private void createNewWorkspace() {
		DirectoryDialog dirDialog = new DirectoryDialog(getShell());
		dirDialog.setText("Select New Workspace Directory");
		dirDialog.setMessage("Choose a directory for the new workspace");
		String dir = dirDialog.open();
		if (dir == null) return;

		InputDialog nameDialog = new InputDialog(getShell(), "Workspace Name",
				"Enter a display name for this workspace:", new File(dir).getName(), null);
		if (nameDialog.open() == Window.OK) {
			String name = nameDialog.getValue().trim();
			if (name.isEmpty()) {
				name = new File(dir).getName();
			}
			File dirFile = new File(dir);
			if (!dirFile.exists()) {
				dirFile.mkdirs();
			}
			WorkspaceManager.getInstance().addWorkspace(dir, name);
			refreshTable();
			pathText.setText(dir);
		}
	}

	private void removeSelectedWorkspace() {
		int idx = workspaceTable.getSelectionIndex();
		if (idx < 0) {
			setErrorMessage("Please select a workspace to remove.");
			return;
		}
		TableItem item = workspaceTable.getItem(idx);
		String path = item.getData("path").toString();
		if (isCurrentWorkspace(path)) {
			setErrorMessage("Cannot remove the current workspace.");
			return;
		}
		WorkspaceManager.getInstance().removeWorkspace(path);
		refreshTable();
		pathText.setText("");
		setErrorMessage(null);
	}

	private boolean isCurrentWorkspace(String path) {
		return normalizePath(path).equals(normalizePath(currentWorkspacePath));
	}

	private String normalizePath(String path) {
		if (path == null) return "";
		if (path.endsWith("/") || path.endsWith(File.separator)) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
}
