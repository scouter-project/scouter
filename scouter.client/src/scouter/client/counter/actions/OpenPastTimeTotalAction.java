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

import java.util.Date;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.counter.views.CounterPastTimeTotalView;
import scouter.client.popup.CalendarDialog.AfterMinuteUnit;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.util.DateUtil;

public class OpenPastTimeTotalAction extends Action {
	public final static String ID = OpenPastTimeTotalAction.class.getName();

	private final IWorkbenchWindow window;
	private String objType;
	private String counter;
	private long startTime;
	private long endTime;
	private int serverId;
	
	public OpenPastTimeTotalAction(IWorkbenchWindow window, String label, String objType, String counter, Image image, long startTime, long endTime, int serverId) {
		this.window = window;
		this.objType = objType;
		this.counter = counter;
		this.startTime = startTime;
		this.endTime = endTime;
		this.serverId = serverId;
		
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}

	public void run() {
		if(startTime <= 0 && endTime <= 0){
			startTime = TimeUtil.getCurrentTime(serverId) - AfterMinuteUnit.FIVE_MIN.getTime();
			endTime = 0;
			String afterMinute = "5 min";
			AfterMinuteUnit m = AfterMinuteUnit.fromString(afterMinute);
			if (m != null) {
			 endTime = startTime + m.getTime();
			}
			if (DateUtil.isSameDay(new Date(startTime), new Date(endTime)) == false) {
			    endTime = DateUtil.getTime(DateUtil.format(startTime, "MM/dd/yyyy") + " 23:59", "MM/dd/yyyy HH:mm");
			}
		}
		if (window != null) {
			try {
				CounterPastTimeTotalView v = (CounterPastTimeTotalView) window.getActivePage().showView(CounterPastTimeTotalView.ID, 
						objType+counter + startTime + endTime+TimeUtil.getCurrentTime(serverId), IWorkbenchPage.VIEW_ACTIVATE);
				if (v != null) {
					v.setInput(startTime, endTime, objType, counter, serverId);
				}
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "[OpenTotalLoadTimeCounterAction] Error opening view:" + e.getMessage());
			}
		}
	}

	public void setStartEndTime(long st, long et) {
		this.startTime = st;
		this.endTime = et;
	}
}
