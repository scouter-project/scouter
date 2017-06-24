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


import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import scouter.client.util.ClientFileUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ZipUtil;
import scouter.util.FileUtil;
import scouter.util.StringUtil;

import java.io.File;

public class ImportWorkspaceAction extends Action {
	public final static String ID = ImportWorkspaceAction.class.getName();

	private final IWorkbenchWindow window;

	public ImportWorkspaceAction(IWorkbenchWindow window, String label, Image image) {
		this.window = window;
		setText(label);
		setId(ID);
		setActionDefinitionId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}
	
	public void run() {
		if (window != null) {
			FileDialog dialog = new FileDialog(window.getShell(), SWT.OPEN);

			dialog.setFilterNames(new String[] {"scouter export zip files", "zip Files (*.zip)"});
			dialog.setFilterExtensions(new String[] {"*.zip"});

			String importFileName = dialog.open();
			if (StringUtil.isEmpty(importFileName)) {
				return;
			}
			
			String workspaceRootName = Platform.getInstanceLocation().getURL().getFile();
			String importWorkingDirName = workspaceRootName + "/import-working";

			ClientFileUtil.deleteDirectory(new File(importWorkingDirName));
			FileUtil.mkdirs(importWorkingDirName);
			try {
				ZipUtil.decompress(importFileName, importWorkingDirName);
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}

			String message = "Import completed.\nRestarting...";
			MessageDialog.openInformation(window.getShell(), "Info", message);
			ExUtil.exec(new Runnable() {
				public void run() {
					PlatformUI.getWorkbench().restart();
				}
			});
		}
	}
}
