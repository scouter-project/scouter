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
package scouter.client.popup;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.util.DateUtil;

public class TimeRangeDialog {
	
	Display display;
	ITimeRange callback;
	String yyyymmdd;
	
	public TimeRangeDialog(Display display, ITimeRange callback) {
		this(display, callback, DateUtil.yyyymmdd(TimeUtil.getCurrentTime()));
	}
	
	public TimeRangeDialog(Display display, ITimeRange callback, String yyyymmdd) {
		this.display = display;
		this.callback = callback;
		this.yyyymmdd = yyyymmdd;
	}
	
	public void show() {
		show(TimeUtil.getCurrentTime() - DateUtil.MILLIS_PER_FIVE_MINUTE, TimeUtil.getCurrentTime());
	}
	
	public void show(long stime, long etime) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout (new GridLayout (4, false));
		dialog.setText("Time Range");
		UIUtil.setDialogDefaultFunctions(dialog);
		
		Label label = new Label(dialog, SWT.NONE);
        label.setText("From");
		final DateTime startTime = new DateTime(dialog, SWT.TIME | SWT.SHORT);
		startTime.setHours(DateUtil.getHour(stime));
		startTime.setMinutes(DateUtil.getMin(stime));
		
		label = new Label(dialog, SWT.NONE);
        label.setText("To");
        final DateTime endTime = new DateTime(dialog, SWT.TIME | SWT.SHORT);
        endTime.setHours(DateUtil.getHour(etime));
        endTime.setMinutes(DateUtil.getMin(etime));
        
        Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						 String fromTime = yyyymmdd + (startTime.getHours () < 10 ? "0" : "") + startTime.getHours() + (startTime.getMinutes () < 10 ? "0" : "") + startTime.getMinutes();
						 String toTime = yyyymmdd + (endTime.getHours () < 10 ? "0" : "") + endTime.getHours() + (endTime.getMinutes () < 10 ? "0" : "") + endTime.getMinutes ();
						 long stime = DateUtil.getTime(fromTime, "yyyyMMddHHmm");
						 long etime = DateUtil.getTime(toTime, "yyyyMMddHHmm");
						 if (etime <= stime) {
							 MessageDialog.openWarning(dialog, "Warning", "Time range is incorrect. ");
						 } else {
							 callback.setTimeRange(stime, etime);
							 dialog.close();
						 }
					} catch (Exception e) {
						e.printStackTrace();
						MessageDialog.openError(dialog, "Error", "Date format error:" + e.getMessage());
					}
					break;
				}
			}
		});
		
		Button cancelButton = new Button(dialog, SWT.PUSH);
		cancelButton.setText("&Cancel");
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		cancelButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					dialog.close();
					break;
				}
			}
		});

		dialog.setDefaultButton(okButton);
		dialog.pack();
		dialog.open();
	}
	
	
	public interface ITimeRange {
		public void setTimeRange(long stime, long etime);
	}

}
