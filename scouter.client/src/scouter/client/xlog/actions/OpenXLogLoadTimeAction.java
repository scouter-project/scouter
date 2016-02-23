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
package scouter.client.xlog.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.popup.CalendarDialog;
import scouter.client.util.ImageUtil;
import scouter.client.xlog.views.XLogLoadTimeView;

public class OpenXLogLoadTimeAction extends Action implements CalendarDialog.ILoadCalendarDialog {
	public final static String ID = OpenXLogLoadTimeAction.class.getName();

	private final IWorkbenchWindow window;
	private String objType;
	private int serverId;
	
	private long stime, etime;
	
	public OpenXLogLoadTimeAction(IWorkbenchWindow window, String objType, Image image, int serverId, long stime, long etime) {
		this.window = window;
		this.objType = objType;
		this.serverId = serverId;
		this.stime = stime;
		this.etime = etime;
		setText("XLog");
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));
	}
	
	public OpenXLogLoadTimeAction(IWorkbenchWindow window, String label, String objType, Image image, int serverId) {
		this(window, objType, image, serverId, 0, 0);
	}

	public void run() {
		if (window != null) {
			try {
				Display display = Display.getCurrent();
				if (display == null) {
					display = Display.getDefault();
				}
				CalendarDialog dialog = new CalendarDialog(display, this);
				if (stime > 0 && etime >0) {
					dialog.showWithEndTime(stime, etime);
				} else {
					dialog.showWithEndTime();
				}
			} catch (Exception e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}

	public void onPressedOk(long startTime, long endTime) {
		if (window != null) {
			try {
				XLogLoadTimeView view = (XLogLoadTimeView) window.getActivePage().showView(XLogLoadTimeView.ID, objType + startTime + endTime,
						IWorkbenchPage.VIEW_ACTIVATE);
				if (view != null) {
					view.setInput(startTime, endTime, objType, serverId);
				}

			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}

	public void onPressedOk(String date) {}
	public void onPressedCancel() {}
}
