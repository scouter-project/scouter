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
package scouter.client.cubrid.actions;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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

import scouter.client.cubrid.ActiveDbInfo;
import scouter.client.cubrid.CubridSingleItem;
import scouter.client.cubrid.CubridSingleItem.InfoType;
import scouter.client.popup.CalendarDialog.AfterMinuteUnit;
import scouter.client.util.UIUtil;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class AddShortPeriodCalendarDialog {
	private final Display display;
	private final IAddSingleShortPeriodDialog callback;
	private final int serverId; 

	Combo dbListCombo;
	Combo counterCombo;

	public AddShortPeriodCalendarDialog(Display display, int serverId, IAddSingleShortPeriodDialog callback) {
		this.display = display;
		this.callback = callback;
		this.serverId = serverId;
	}

	public void show(Point p, long stime, long etime) {
		if (p != null)
			show((int) p.getX(), (int) p.getY() + 10, stime, etime);
	}

	public void show(Point p, long stime) {
		if (p != null)
			showOnlyStartTime((int) p.getX(), (int) p.getY() + 10, stime);
	}
	
	public void show(long stime, long etime) {
		show(UIUtil.getMousePosition(), stime, etime);
	}

	public void show(long stime) {
		show(UIUtil.getMousePosition(), stime);
	}
	
	public void show(int x, int y, long stime, long etime) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout(new GridLayout(4, false));
		dialog.setText("Add PeriodView");
		UIUtil.setDialogDefaultFunctions(dialog);

		Label Label = new Label(dialog, SWT.NONE);
		Label.setText("Counter");
		counterCombo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		ArrayList<String> multiViewList = new ArrayList<String>();
		for (CubridSingleItem view : CubridSingleItem.values()) {
			multiViewList.add(view.getTitle());
		}
		counterCombo.setItems(multiViewList.toArray(new String[CubridSingleItem.values().length]));
		counterCombo.select(0);
		counterCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		counterCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				if (CubridSingleItem.infotypeFromOrdinal(counterCombo.getSelectionIndex()).equals(InfoType.BROKER_INFO)) {
					dbListCombo.setEnabled(false);
					dbListCombo.removeAll();
					dbListCombo.add("BROKER_INFO");
					dbListCombo.select(0);
				} else {
					dbListCombo.setEnabled(true);
					dbListCombo.removeAll();
					dbLoad();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}
		});

		Label = new Label(dialog, SWT.NONE);
		Label.setText("DB");
		dbListCombo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		dbListCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		dbLoad();

		final DateTime calendar = new DateTime(dialog, SWT.CALENDAR);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		calendar.setLayoutData(data);

		int year = CastUtil.cint(DateUtil.format(stime, "yyyy"));
		int month = CastUtil.cint(DateUtil.format(stime, "MM")) - 1;
		int day = CastUtil.cint(DateUtil.format(stime, "dd"));
		calendar.setDate(year, month, day);
		calendar.setDay(day);

		Label = new Label(dialog, SWT.NONE);
		Label.setText("From");
		final DateTime startTime = new DateTime(dialog, SWT.TIME | SWT.SHORT);
		startTime.setHours(DateUtil.getHour(stime));
		startTime.setMinutes(DateUtil.getMin(stime));

		Label = new Label(dialog, SWT.NONE);
		Label.setText("To");
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
						String counterTitle = counterCombo.getText();
						CubridSingleItem type = CubridSingleItem.fromCounterName(counterTitle);

						String fromTime = (calendar.getMonth() + 1) + "/" + calendar.getDay() + "/" + calendar.getYear()
								+ " " + (startTime.getHours() < 10 ? "0" : "") + startTime.getHours() + ":"
								+ (startTime.getMinutes() < 10 ? "0" : "") + startTime.getMinutes();

						boolean nextDay0 = false;
						if (endTime.getHours() == 0 && endTime.getMinutes() == 0) {
							nextDay0 = true;
						}

						String toTime = (calendar.getMonth() + 1) + "/" + calendar.getDay() + "/" + calendar.getYear()
								+ " " + (endTime.getHours() < 10 ? "0" : "") + endTime.getHours() + ":"
								+ (endTime.getMinutes() < 10 ? "0" : "") + endTime.getMinutes();

						long startTime = DateUtil.getTime(fromTime, "MM/dd/yyyy HH:mm");
						long endTime = DateUtil.getTime(toTime, "MM/dd/yyyy HH:mm");

						if (nextDay0) {
							endTime += DateUtil.MILLIS_PER_DAY - 1000;
						}
						if (endTime <= startTime) {
							MessageDialog.openWarning(dialog, "Warning", "Time range is incorrect");
						} else {
							callback.onPressedOk(dbListCombo.getItem(dbListCombo.getSelectionIndex()), type, startTime,
									endTime);
							dialog.close();
						}

					} catch (Exception e) {
						MessageDialog.openError(dialog, "Error55", "format error:" + e.getMessage());
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

		dialog.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				callback.onPressedCancel();
			}
		});

		dialog.setDefaultButton(okButton);
		dialog.pack();

		if (x > 0 && y > 0) {
			dialog.setLocation(x, y);
		}

		dialog.open();
	}
	
	public void showOnlyStartTime(int x, int y, long stime) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout(new GridLayout(4, false));
		dialog.setText("Add PeriodView");
		UIUtil.setDialogDefaultFunctions(dialog);

		Label Label = new Label(dialog, SWT.NONE);
		Label.setText("Counter");
		counterCombo = new Combo(dialog, SWT.DROP_DOWN);
		ArrayList<String> multiViewList = new ArrayList<String>();
		for (CubridSingleItem view : CubridSingleItem.values()) {
			multiViewList.add(view.getTitle());
		}
		counterCombo.setItems(multiViewList.toArray(new String[CubridSingleItem.values().length]));
		counterCombo.select(0);
		counterCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		counterCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				if (CubridSingleItem.infotypeFromOrdinal(counterCombo.getSelectionIndex()).equals(InfoType.BROKER_INFO)) {
					dbListCombo.setEnabled(false);
					dbListCombo.removeAll();
					dbListCombo.add("BROKER_INFO");
					dbListCombo.select(0);
				} else {
					dbListCombo.setEnabled(true);
					dbListCombo.removeAll();
					dbLoad();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}
		});

		Label = new Label(dialog, SWT.NONE);
		Label.setText("DB");
		dbListCombo = new Combo(dialog, SWT.DROP_DOWN);
		dbListCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		dbLoad();

		final DateTime calendar = new DateTime(dialog, SWT.CALENDAR);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		calendar.setLayoutData(data);

		int year = CastUtil.cint(DateUtil.format(stime, "yyyy"));
		int month = CastUtil.cint(DateUtil.format(stime, "MM")) - 1;
		int day = CastUtil.cint(DateUtil.format(stime, "dd"));
		calendar.setDate(year, month, day);
		calendar.setDay(day);

		Label = new Label(dialog, SWT.NONE);
		Label.setText("From");
		final DateTime startTime = new DateTime(dialog, SWT.TIME | SWT.SHORT);
		startTime.setHours(DateUtil.getHour(stime));
		startTime.setMinutes(DateUtil.getMin(stime));

		Label = new Label(dialog, SWT.NONE);
		Label.setText("To");
		final Combo afterMinutes = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
        ArrayList<String> minuteStrList = new ArrayList<String>();
        for (AfterMinuteUnit minute : AfterMinuteUnit.values()) {
            minuteStrList.add(minute.getLabel());
        }
        afterMinutes.setItems(minuteStrList.toArray(new String[AfterMinuteUnit.values().length]));
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
                        	String counterTitle = counterCombo.getText();
    						CubridSingleItem type = CubridSingleItem.fromCounterName(counterTitle);
                        	
                            String fromTime = (calendar.getMonth() + 1) + "/" + calendar.getDay() + "/" + calendar.getYear() + " " + startTime.getHours() + ":" + (startTime.getMinutes() < 10 ? "0" : "") + startTime.getMinutes();
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
                                    endTime = DateUtil.getTime((calendar.getMonth() + 1) + "/" + calendar.getDay() + "/" + calendar.getYear() + " 23:59", "MM/dd/yyyy HH:mm");
                                }
                                callback.onPressedOk(dbListCombo.getItem(dbListCombo.getSelectionIndex()), type, startTime,
    									endTime);
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

		dialog.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				callback.onPressedCancel();
			}
		});

		dialog.setDefaultButton(okButton);
		dialog.pack();

		if (x > 0 && y > 0) {
			dialog.setLocation(x, y);
		}

		dialog.open();
	}
	

	public interface IAddSingleShortPeriodDialog {
		void onPressedOk(String dbName, CubridSingleItem viewType, long stime, long etime);

		void onPressedCancel();
	}

	private void dbLoad() {
		ActiveDbInfo activeDBList = ActiveDbInfo.getInstance();
		if (!activeDBList.isEmpty(serverId)) {
			for (String dbName : activeDBList.keySet(serverId)) {
				dbListCombo.add(dbName);
			}
		} else {
			return;
		}
		dbListCombo.select(0);
	}

}
