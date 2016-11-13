/*
 *  Copyright 2016 the original author or authors. 
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
 */
package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

import scouter.client.model.TextProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.stack.dialog.StackListDialog;

public class OpenStackDialogAction extends Action {
	public final static String ID = OpenStackDialogAction.class.getName();
	
	int serverId;
	int objHash;

	public OpenStackDialogAction(int serverId, int objHash) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.setText("Analyze");
	}
	
	public void run(){
		new CalendarDialog(Display.getDefault(), new ILoadCalendarDialog() {
			public void onPressedOk(String date) {
				String objName = TextProxy.object.getText(objHash);
				new StackListDialog(serverId, objName, date).open();
			}
			public void onPressedOk(long startTime, long endTime) {
				
			}
			public void onPressedCancel() {
			}
		}).show();
	}
}