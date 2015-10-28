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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.model.AgentColorManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.util.ImageUtil;


public class SetColorAction extends Action {
	public final static String ID = SetColorAction.class.getName();

	private final IWorkbenchWindow window;
	private int objHash;
	
	public SetColorAction(IWorkbenchWindow window, int objHash) {
		this.window = window;
		this.objHash = objHash;
		setText("Set Color");
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.color_swatch));
	}

	public void run() {
		if (window != null) {
			ColorDialog dlg = new ColorDialog(window.getShell());
			AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
			dlg.setRGB(agent.getColor().getRGB());
			dlg.setText("Choose a Color");
			RGB rgb = dlg.open();
			if (rgb != null) {
				Color color = AgentColorManager.getInstance().changeColor(objHash, rgb);
				//agent.setColor(color);
			}
		}
	}
}
