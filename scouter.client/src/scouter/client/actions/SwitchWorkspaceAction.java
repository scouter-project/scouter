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
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.workspace.SwitchWorkspaceDialog;

public class SwitchWorkspaceAction extends Action {
	public final static String ID = SwitchWorkspaceAction.class.getName();

	private final IWorkbenchWindow window;

	public SwitchWorkspaceAction(IWorkbenchWindow window, String label) {
		this.window = window;
		setText(label);
		setId(ID);
		setActionDefinitionId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh));
	}

	public void run() {
		if (window == null) return;

		SwitchWorkspaceDialog dialog = new SwitchWorkspaceDialog(window.getShell());
		if (dialog.open() != Window.OK) return;

		String selectedPath = dialog.getSelectedPath();
		if (selectedPath == null || selectedPath.isEmpty()) return;

		String commandLine = buildCommandLine(selectedPath);
		System.setProperty("eclipse.exitdata", commandLine);
		System.setProperty("scouter.workspace.switch", "true");
		ExUtil.exec(new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().restart();
			}
		});
	}

	private String buildCommandLine(String newWorkspacePath) {
		String property = System.getProperty("eclipse.commands");
		if (property == null) {
			return "-data\n" + newWorkspacePath + "\n";
		}

		StringBuilder result = new StringBuilder();
		String[] lines = property.split("\n");
		boolean skipNext = false;
		boolean dataFound = false;

		for (String line : lines) {
			if (skipNext) {
				skipNext = false;
				continue;
			}
			if ("-data".equals(line.trim())) {
				result.append("-data\n");
				result.append(newWorkspacePath).append("\n");
				skipNext = true;
				dataFound = true;
			} else {
				result.append(line).append("\n");
			}
		}

		if (!dataFound) {
			result.append("-data\n");
			result.append(newWorkspacePath).append("\n");
		}

		return result.toString();
	}
}
