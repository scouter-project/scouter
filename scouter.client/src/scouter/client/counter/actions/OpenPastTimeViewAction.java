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

import scouter.client.counter.views.CounterLoadTimeView;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.util.DateUtil;

public class OpenPastTimeViewAction extends Action implements ILoadCalendarDialog {
	public final static String ID = OpenPastTimeViewAction.class.getName();

	private final IWorkbenchWindow win;
	private int objHash;
	private String objType;
	private String date;
	private String objName;
	private String counter;
	private int serverId;
	
	long startTime, endTime;

	public OpenPastTimeViewAction(IWorkbenchWindow win, String label, Image image, int objHash, String objType, String date, String objName, String counter, int serverId) {
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
				long current = TimeUtil.getCurrentTime(serverId);
				CalendarDialog calDialog = new CalendarDialog(win.getShell().getDisplay(), OpenPastTimeViewAction.this);
				calDialog.showWithTime(UIUtil.getMousePosition(), current);
			}else{
				long st = DateUtil.getTime(date + " 00:00", "yyyyMMdd HH:mm");
				CalendarDialog calDialog = new CalendarDialog(win.getShell().getDisplay(), OpenPastTimeViewAction.this);
				calDialog.showWithTime(UIUtil.getMousePosition(), st);
			}
		}
	}

	private void goLoadCounteView(){
		try {
			String subid = date + "-" + objHash + "-" + objType + counter+startTime+endTime;
			CounterLoadTimeView view = (CounterLoadTimeView) win.getActivePage().showView(
					CounterLoadTimeView.ID, subid, IWorkbenchPage.VIEW_ACTIVATE);
			if (view != null) {
				view.setInput(date, startTime, endTime, objHash, objName, objType, counter, serverId);
			}
		} catch (Exception e) {
			MessageDialog.openError(win.getShell(), "Error", "Error opening view:" + e.getMessage());
		}
	}

	public void onPressedOk(long startTime, long endTime) {
		this.date = DateUtil.yyyymmdd(startTime);
		this.startTime = startTime;
		this.endTime = endTime;
		goLoadCounteView();
	}

	public void onPressedOk(String date) {
	}

	public void onPressedCancel() {
	}
}
