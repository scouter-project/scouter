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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.group.view.CounterPastDateGroupCountView;
import scouter.client.model.GroupObject;
import scouter.client.popup.CalendarDialog;
import scouter.client.util.ImageUtil;

public class OpenPastDateGroupCountViewAction extends Action implements CalendarDialog.ILoadCalendarDialog{
	public final static String ID = OpenPastDateGroupCountViewAction.class.getName();

	private final IWorkbenchWindow window;
	private String grpName;
	private String objType;
	private String counter;

	public OpenPastDateGroupCountViewAction(IWorkbenchWindow window, String label, String counter, GroupObject grpObj) {
		this.window = window;
		this.counter = counter;
		this.grpName = grpObj.getName();
		this.objType = grpObj.getObjType();
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.TYPE_SERVICE_COUNT));
	}

	public void run() {
		if (window != null) {
			try {
				Display display = Display.getCurrent();
				if (display == null) {
					display = Display.getDefault();
				}
				CalendarDialog dialog = new CalendarDialog(display, this);
				dialog.show();
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}

	public void onPressedOk(String date) {
		if (window != null) {
			try {
				window.getActivePage().showView(CounterPastDateGroupCountView.ID, 
						grpName + "&" + objType + "&" + counter + "&" + date, IWorkbenchPage.VIEW_ACTIVATE);
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
	
	public void onPressedOk(long startTime, long endTime) {}
	public void onPressedCancel() { }
}
