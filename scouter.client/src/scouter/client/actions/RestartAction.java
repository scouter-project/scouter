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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;


public class RestartAction extends Action {
	public final static String ID = RestartAction.class.getName();

	private final IWorkbenchWindow window;

	public RestartAction(IWorkbenchWindow window, String label) {
		this.window = window;
		setText(label);
		setId(ID);
		setActionDefinitionId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh));
	}
	
	public void run() {
		if (window != null) {
			boolean ok = MessageDialog.openQuestion(window.getShell(), "Restart", "Restart now?");
			if(ok){
				ExUtil.exec(new Runnable() {
					public void run() {
						PlatformUI.getWorkbench().restart();
					}
				});
			}
		}
	}
}
