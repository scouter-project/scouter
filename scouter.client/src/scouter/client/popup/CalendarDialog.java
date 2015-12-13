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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import scouter.client.util.ConsoleProxy;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class CalendarDialog {
	
	private final Display display;
	private final ILoadCalendarDialog callback;
	
	public CalendarDialog(Display display, ILoadCalendarDialog callback) {
		this.display = display;
		this.callback = callback;
	}
	
	public void show() {
		show(-1, -1, -1);
	}
		
	
	public void show(int x, int y, long time){
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout (new GridLayout (2, true));
		dialog.setText("Date");
		
		UIUtil.setDialogDefaultFunctions(dialog);
		
		final DateTime calendar = new DateTime(dialog, SWT.CALENDAR);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		calendar.setLayoutData(data);
		
		if (time > 0) {
			String yyyymmdd = DateUtil.format(time, "yyyy-MM-dd");
			String[] date = yyyymmdd.split("-");
			calendar.setDate(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));
		}
		
		Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						 String date = (calendar.getMonth () + 1) + "/" + calendar.getDay() + "/" + calendar.getYear();
						 date = DateUtil.format(DateUtil.getTime(date, "MM/dd/yyyy"), "yyyyMMdd");
						 dialog.close();
						 callback.onPressedOk(date);
					} catch (Exception e) {
						MessageDialog.openError(dialog, "Error55", "Date format error:" + e.getMessage());
					}
					break;
				}
			}
		});
		
		Button cancelButton = new Button(dialog, SWT.PUSH);
		cancelButton.setText("&Cancel");
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		cancelButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					callback.onPressedCancel();
					dialog.close();
					break;
				}
			}
		});

		dialog.addListener(SWT.Close, new Listener() {
	        public void handleEvent(Event event) {
	        	callback.onPressedCancel();
	        }
	    });
		
		dialog.setDefaultButton(okButton);
		dialog.pack();
		
		if(x > 0 && y > 0){
			dialog.setLocation(x, y);
		}
		
		dialog.open();
	}
	
	public void showWithTime() {
		showWithTime(-1, -1, -1);
	}
	public void showWithTime(Point p, long time){
		if(p != null)
			showWithTime((int)p.getX(), (int)p.getY() + 10, time);
	}
	
	public void showWithTime(int x, int y, long time) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout (new GridLayout (4, false));
		dialog.setText("Date/Time");
		
		UIUtil.setDialogDefaultFunctions(dialog);
		
		final DateTime calendar = new DateTime(dialog, SWT.CALENDAR);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		calendar.setLayoutData(data);
		
		if(time > 0){
			int year = CastUtil.cint(DateUtil.format(time, "yyyy"));
			int month = CastUtil.cint(DateUtil.format(time, "MM")) - 1;
			int day = CastUtil.cint(DateUtil.format(time, "dd"));
			calendar.setDate(year, month, day);
			calendar.setDay(day);
		}
		
		Label label = new Label(dialog, SWT.NONE);
        label.setText("From");
		final DateTime startTime = new DateTime(dialog, SWT.TIME | SWT.SHORT);
		
		if(time > 0){
			int hours = CastUtil.cint(DateUtil.format(time, "HH"));
			int minutes = CastUtil.cint(DateUtil.format(time, "mm"));
			int seconds = CastUtil.cint(DateUtil.format(time, "ss"));
			startTime.setTime(hours, minutes, seconds);
		}else{
			startTime.setHours(7);
			startTime.setMinutes(0);
		}
		
		label = new Label(dialog, SWT.NONE);
        label.setText("To");
		final Combo afterMinutes = new Combo (dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		ArrayList<String> minuteStrList = new ArrayList<String>();
		for (AfterMinuteUnit minute : AfterMinuteUnit.values()) {
			minuteStrList.add(minute.getLabel());
		}
		afterMinutes.setItems (minuteStrList.toArray(new String[AfterMinuteUnit.values().length]));
		afterMinutes.select(0);
		afterMinutes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						 String fromTime = (calendar.getMonth () + 1) + "/" + calendar.getDay() + "/" + calendar.getYear() + " " + startTime.getHours() + ":" + (startTime.getMinutes () < 10 ? "0" : "") + startTime.getMinutes ();
						 long startTime = DateUtil.getTime(fromTime, "MM/dd/yyyy HH:mm");
						 long endTime = 0;
						 String afterMinute = afterMinutes.getText();
						 AfterMinuteUnit m = AfterMinuteUnit.fromString(afterMinute);
						 if (m != null) {
							 endTime = startTime + m.getTime();
						 }
						 if (endTime <= startTime) {
							 MessageDialog.openWarning(dialog, "Warning", "Time range is incorrect");
						 } else {
							 if (DateUtil.isSameDay(new Date(startTime), new Date(endTime)) == false) {
								 endTime = DateUtil.getTime((calendar.getMonth () + 1) + "/" + calendar.getDay() + "/" + calendar.getYear() + " 23:59", "MM/dd/yyyy HH:mm");
							 }
							 callback.onPressedOk(startTime, endTime);
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
					callback.onPressedCancel();
					dialog.close();
					break;
				}
			}
		});

		dialog.setDefaultButton(okButton);
		dialog.pack();
		
		if(x > 0 && y > 0){
			dialog.setLocation(x, y);
		}
		
		dialog.open();
	}
	
	public void showWithEndTime() {
		long etime=(TimeUtil.getCurrentTime()/60000)*60000 ;
		long stime=etime-DateUtil.MILLIS_PER_FIVE_MINUTE;
		showWithEndTime(stime, etime);
	}
	
	public void showWithEndTime(long stime, long etime) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout (new GridLayout (4, false));
		dialog.setText("Date/Time");
		
		final DateTime calendar = new DateTime(dialog, SWT.CALENDAR);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		calendar.setLayoutData(data);
		
		int year = CastUtil.cint(DateUtil.format(stime, "yyyy"));
		int month = CastUtil.cint(DateUtil.format(stime, "MM")) - 1;
		int day = CastUtil.cint(DateUtil.format(stime, "dd"));
		calendar.setDate(year, month, day);
		calendar.setDay(day);
		
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
						 String fromTime = (calendar.getMonth () + 1) + "/" + calendar.getDay() + "/" + calendar.getYear() + " " + (startTime.getHours () < 10 ? "0" : "") + startTime.getHours() + ":" + (startTime.getMinutes () < 10 ? "0" : "") + startTime.getMinutes ();
						 String toTime = (calendar.getMonth () + 1) + "/" + calendar.getDay() + "/" + calendar.getYear() + " " + (endTime.getHours () < 10 ? "0" : "") + endTime.getHours() + ":" + (endTime.getMinutes () < 10 ? "0" : "") + endTime.getMinutes ();
						 long startTime = DateUtil.getTime(fromTime, "MM/dd/yyyy HH:mm");
						 long endTime = DateUtil.getTime(toTime, "MM/dd/yyyy HH:mm");
						 if (endTime <= startTime) {
							 MessageDialog.openWarning(dialog, "Warning", "Time range is incorrect");
						 } else {
							 if (DateUtil.isSameDay(new Date(startTime), new Date(endTime)) == false) {
								 endTime = DateUtil.getTime((calendar.getMonth () + 1) + "/" + calendar.getDay() + "/" + calendar.getYear() + " 23:59", "MM/dd/yyyy HH:mm");
							 }
							 callback.onPressedOk(startTime, endTime);
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
					callback.onPressedCancel();
					dialog.close();
					break;
				}
			}
		});

		dialog.setDefaultButton(okButton);
		dialog.pack();
		dialog.open();
	}
	
	public interface ILoadCalendarDialog {
		void onPressedOk(long startTime, long endTime);
		void onPressedOk(String date);
		void onPressedCancel();
	}
	
	public enum AfterMinuteUnit {
		FIVE_MIN ("5 min", 5 * 60 * 1000),
		TEN_MIN ("10 min", 10 * 60 * 1000),
		TWT_MIN("20 min", 20 * 60 * 1000),
		THIRTY_MIN("30 min", 30 * 60 * 1000),
		ONE_HOUR("1 hour", 60 * 60 * 1000),
		FOUR_HOURS("4 hours", 4 * 60 * 60 * 1000);
		
		private String label;
		private long time;

		private AfterMinuteUnit(String label,  long time) {
			this.label = label;
			this.time = time;
		}
		
		public String getLabel() {
			return this.label;
		}
		
		public long getTime() {
			return this.time;
		}
		
		public static AfterMinuteUnit fromString(String text) {
		    if (text != null) {
		    	for (AfterMinuteUnit b : AfterMinuteUnit.values()) {
		    		if (text.equalsIgnoreCase(b.label)) {
		    			return b;
		    		}
		    	}
		    }
		    return null;
	  }
	}
}
