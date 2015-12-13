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
package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.counter.views.CounterLoadDateView;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.util.ImageUtil;

public class OpenPastDateViewAction extends Action implements ILoadCalendarDialog {
	public final static String ID = OpenPastDateViewAction.class.getName();

	private final IWorkbenchWindow win;
	private int objHash;
	private String objType;
	private String date;
	private String objName;
	private String counter;
	private int serverId;

	public OpenPastDateViewAction(IWorkbenchWindow win, String label, Image image, int objHash, String objType, String date, String objName, String counter, int serverId) {
		this.win = win;
		this.objHash = objHash;
		this.objType = objType;
		this.date = date;
		this.objName = objName;
		this.counter = counter;
		this.serverId = serverId;
		
		setText(label);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}

	public void run() {
		if (win != null) {
			if(date == null){
				CalendarDialog dialog = new CalendarDialog(win.getShell().getDisplay(), OpenPastDateViewAction.this);
				dialog.show();
			}else{
				goLoadCounteView();
			}
		}
	}

	public void onPressedOk(long startTime, long endTime) {
	}

	public void onPressedOk(String date) {
		this.date = date;
		goLoadCounteView();
	}

	public void onPressedCancel() {
	}
	
	private void goLoadCounteView(){
		try {
			String subid = date + "-" + objHash + "-" + objType + counter;
			CounterLoadDateView view = (CounterLoadDateView) win.getActivePage().showView(
					CounterLoadDateView.ID, subid, IWorkbenchPage.VIEW_ACTIVATE);
			if (view != null) {
				view.setInput(date, objHash, objName, objType, counter, serverId);
			}
		} catch (Exception e) {
			MessageDialog.openError(win.getShell(), "Error", "Error opening view:" + e.getMessage());
		}
	}
}
