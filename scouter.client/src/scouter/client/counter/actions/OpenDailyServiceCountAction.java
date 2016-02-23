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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.counter.views.CounterPastCountView;
import scouter.client.popup.CalendarDialog;
import scouter.client.util.ImageUtil;
import scouter.util.DateUtil;

public class OpenDailyServiceCountAction extends Action implements CalendarDialog.ILoadCalendarDialog {
	public final static String ID = OpenDailyServiceCountAction.class.getName();

	private final IWorkbenchWindow window;
	private String objType;
	private String counter;
	private int serverId;
	private String date;

	public OpenDailyServiceCountAction(IWorkbenchWindow window, String objType, String counter, Image image, int serverId) {
		this(window, objType, counter, image, serverId, null);
	}
	
	public OpenDailyServiceCountAction(IWorkbenchWindow window, String objType, String counter, Image image, int serverId, String date) {
		this.window = window;
		this.objType = objType;
		this.counter = counter;
		this.serverId = serverId;
		this.date = date;
		setText("24H Service Count");
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}

	public void run() {
		if (window != null) {
			try {
				Display display = Display.getCurrent();
				if (display == null) {
					display = Display.getDefault();
				}
				CalendarDialog dialog = new CalendarDialog(display, this);
				if (date == null) {
					dialog.show();
				} else {
					dialog.show(-1, -1, DateUtil.getTime(date, "yyyyMMdd"));
				}
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}

	public void onPressedOk(String date) {
		if (window != null) {
			try {
				CounterPastCountView v = (CounterPastCountView) window.getActivePage().showView(CounterPastCountView.ID, 
						objType + counter + date, IWorkbenchPage.VIEW_ACTIVATE);
				if (v != null) {
					v.setInput(date, objType, counter, serverId);
				}
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
	
	public void onPressedOk(long startTime, long endTime) {}
	public void onPressedCancel() { }
	
}
