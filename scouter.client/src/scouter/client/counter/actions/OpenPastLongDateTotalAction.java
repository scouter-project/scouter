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

import scouter.client.counter.views.CounterPastLongDateTotalView;
import scouter.client.counter.views.CounterPastLongDateAllView.DatePeriodUnit;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.util.DateUtil;

public class OpenPastLongDateTotalAction extends Action /*implements LoadDualCalendarDialog.ILoadDualCounterDialog*/ {
	public final static String ID = OpenPastLongDateTotalAction.class.getName();

	private final IWorkbenchWindow window;
	private String objType;
	private String counter;
	private String sDate, eDate;
	private int serverId;
	
	public OpenPastLongDateTotalAction(IWorkbenchWindow window, String label, String objType, String counter, Image image, String sDate, String eDate, int serverId) {
		this.window = window;
		this.objType = objType;
		this.counter = counter;
		this.sDate = sDate;
		this.eDate = eDate;
		this.serverId = serverId;
		
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}

	public void run() {
		if(sDate == null && eDate == null){
			long yesterday = TimeUtil.getCurrentTime(serverId) - DatePeriodUnit.A_DAY.getTime();
			sDate = DateUtil.format(yesterday, "yyyyMMdd");
			eDate = sDate;
		}
		if (window != null) {
			try {
				CounterPastLongDateTotalView v = (CounterPastLongDateTotalView) window.getActivePage().showView(CounterPastLongDateTotalView.ID, 
						objType + counter + sDate+eDate + TimeUtil.getCurrentTime(serverId), IWorkbenchPage.VIEW_ACTIVATE);
				if (v != null) {
					v.setInput(sDate, eDate, objType, counter, serverId);
				}
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}

	public void setStartEndTime(String sDate, String eDate) {
		this.sDate = sDate;
		this.eDate = eDate;
	}

}
