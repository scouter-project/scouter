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
import org.mihalis.opal.utils.StringUtil;

import scouter.client.util.ClientFileUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ZipUtil;
import scouter.util.FileUtil;

import java.io.File;
import java.io.IOException;

public class ExportWorkspaceAction extends Action {
	public final static String ID = ExportWorkspaceAction.class.getName();

	private final IWorkbenchWindow window;

	public ExportWorkspaceAction(IWorkbenchWindow window, String label, Image image) {
		this.window = window;
		setText(label);
		setId(ID);
		setActionDefinitionId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}
	
	public void run() {
		if (window != null) {
			String message = "It may be lost that you make change in this session.\n"
					+ "If you want to export all settings made in this session,\n"
					+ "please restart and try again.\n"
					+ "Or if you didn't make any change in this session, just try it";
			if(!MessageDialog.openConfirm(window.getShell(), "Confirm", message)) {
				return;
			}

			FileDialog dialog = new FileDialog(window.getShell(), SWT.SAVE);

			dialog.setFilterNames(new String[] { "scouter export zip files", "zip Files (*.zip)" });
			dialog.setFilterExtensions(new String[] { "*.zip", "*.*" });
			//dialog.setFilterPath("c:\\");
			dialog.setFileName("scouter-client-export-workspace.zip");
			dialog.setOverwrite(true);
			
			String exportFileName = dialog.open();
			
			if(StringUtil.isEmpty(exportFileName)) {
				return;
			}
			
			String workspaceRootName = Platform.getInstanceLocation().getURL().getFile();
			String exportWorkingDirName = workspaceRootName + "/export-working";

			new Thread(() -> {
				ClientFileUtil.deleteDirectory(new File(exportWorkingDirName));
				FileUtil.mkdirs(exportWorkingDirName);

				try {
					ClientFileUtil.copy(new File(workspaceRootName + "/" + ClientFileUtil.XLOG_COLUMN_FILE),
							new File(exportWorkingDirName + "/" + ClientFileUtil.XLOG_COLUMN_FILE));
					ClientFileUtil.copy(new File(workspaceRootName + "/" + ClientFileUtil.GROUP_FILE),
							new File(exportWorkingDirName + "/" + ClientFileUtil.GROUP_FILE));
					ClientFileUtil.copy(new File(workspaceRootName + "/" + ClientFileUtil.WORKSPACE_METADATA_DIR),
							new File(exportWorkingDirName + "/" + ClientFileUtil.WORKSPACE_METADATA_DIR));
					ClientFileUtil.deleteFile(new File(exportWorkingDirName + "/" + ClientFileUtil.WORKSPACE_LOG_FILE));
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					ZipUtil.compress(exportWorkingDirName, exportFileName);
					ClientFileUtil.deleteDirectory(new File(exportWorkingDirName));
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}
			}).start();
		}
	}
}
