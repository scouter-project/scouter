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
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.group.view.XLogLoadTimeGroupView;
import scouter.client.model.GroupObject;
import scouter.client.popup.CalendarDialog;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;

public class OpenPastTimeTranXGroupViewAction extends Action implements CalendarDialog.ILoadCalendarDialog {
	public final static String ID = OpenPastTimeTranXGroupViewAction.class.getName();

	private final IWorkbenchWindow window;
	private String grpName;
	private String objType;

	public OpenPastTimeTranXGroupViewAction(IWorkbenchWindow window, String label, GroupObject grpObj) {
		this.window = window;
		this.grpName = grpObj.getName();
		this.objType = grpObj.getObjType();
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.transrealtime));
	}

	public void run() {
		if (window != null) {
			try {
				Display display = Display.getCurrent();
				if (display == null) {
					display = Display.getDefault();
				}
				CalendarDialog dialog = new CalendarDialog(display, this);
				dialog.showWithEndTime();
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
		if (window != null) {
			
		}
	}

	public void onPressedOk(long startTime, long endTime) {
		try {
			XLogLoadTimeGroupView view = (XLogLoadTimeGroupView) window.getActivePage().showView(XLogLoadTimeGroupView.ID, grpName + "&" + objType,
					IWorkbenchPage.VIEW_ACTIVATE);
			if (view != null) {
				view.setInput(startTime, endTime);
			}
		} catch (PartInitException e) {
			ConsoleProxy.error(e.toString());
		}
		
	}
	public void onPressedOk(String date) {}
	public void onPressedCancel() {}
}
