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
import scouter.client.util.UIUtil;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class AddLongPeriodCalendarDialog {

	private final Display display;
	private final IAddSingleLongPeriodDialog callback;

	Combo dbListCombo;
	Combo counterCombo;

	public AddLongPeriodCalendarDialog(Display display, IAddSingleLongPeriodDialog callback) {
		this.display = display;
		this.callback = callback;
	}

	public void show(long stime, long etime) {
		show((int) UIUtil.getMousePosition().getX(), (int) UIUtil.getMousePosition().getY(), DateUtil.yyyymmdd(stime),
				DateUtil.yyyymmdd(etime));
	}

	public void show() {
		show(-1, -1);
	}

	public void show(Point p) {
		if (p != null)
			show((int) p.getX(), (int) p.getY() + 10);
	}

	public void show(int x, int y) {
		show("Start Date:", "End Date:", x, y, null, null);
	}

	public void show(int x, int y, String yyyymmdd1, String yyyymmdd2) {
		show("Start Date:", "End Date:", x, y, yyyymmdd1, yyyymmdd2);
	}

	public void show(String label1, String label2, int x, int y) {
		show(label1, label2, x, y, null, null);
	}

	public void show(String label1, String label2, int x, int y, String yyyymmdd1, String yyyymmdd2) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout(new GridLayout(4, false));
		dialog.setText("Date");

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

		Label sDate = new Label(dialog, SWT.NONE);
		sDate.setText(label1);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		sDate.setLayoutData(data);

		final DateTime startCal = new DateTime(dialog, SWT.CALENDAR);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		startCal.setLayoutData(data);

		Label eDate = new Label(dialog, SWT.NONE);
		eDate.setText(label2);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		eDate.setLayoutData(data);

		final DateTime endCal = new DateTime(dialog, SWT.CALENDAR);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
		endCal.setLayoutData(data);

		Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						String sDate = (startCal.getMonth() + 1) + "/" + startCal.getDay() + "/" + startCal.getYear();
						sDate = DateUtil.format(DateUtil.getTime(sDate, "MM/dd/yyyy"), "yyyyMMdd");
						String eDate = (endCal.getMonth() + 1) + "/" + endCal.getDay() + "/" + endCal.getYear();
						eDate = DateUtil.format(DateUtil.getTime(eDate, "MM/dd/yyyy"), "yyyyMMdd");

						if (CastUtil.cint(sDate) > CastUtil.cint(eDate)) {
							MessageDialog.openError(dialog, "Error", "End Date is later than Start Date");
							return;
						}

						String counterTitle = counterCombo.getText();
						CubridSingleItem type = CubridSingleItem.fromCounterName(counterTitle);

						callback.onPressedOk(dbListCombo.getItem(dbListCombo.getSelectionIndex()), type, sDate, eDate);
						dialog.close();
					} catch (Exception e) {
						MessageDialog.openError(dialog, "Error55", "Date format error:" + e.getMessage());
					}
					break;
				}
			}
		});

		Button cancelButton = new Button(dialog, SWT.PUSH);
		cancelButton.setText("&Cancel");
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
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

		if (x > 0 && y > 0) {
			dialog.setLocation(x, y);
		}

		if (yyyymmdd1 != null) {
			int year = Integer.valueOf(yyyymmdd1.substring(0, 4));
			int month = Integer.valueOf(yyyymmdd1.substring(4, 6)) - 1;
			int day = Integer.valueOf(yyyymmdd1.substring(6, 8));
			startCal.setDate(year, month, day);
		}
		if (yyyymmdd2 != null) {
			int year = Integer.valueOf(yyyymmdd2.substring(0, 4));
			int month = Integer.valueOf(yyyymmdd2.substring(4, 6)) - 1;
			int day = Integer.valueOf(yyyymmdd2.substring(6, 8));
			endCal.setDate(year, month, day);
		}

		dialog.open();
	}

	public interface IAddSingleLongPeriodDialog {
		void onPressedOk(String dbName, CubridSingleItem viewType, String sDate, String eDate);

		void onPressedCancel();
	}

	private void dbLoad() {
		ActiveDbInfo activeDBList = ActiveDbInfo.getInstance();
		if (!activeDBList.isEmpty()) {
			for (String dbName : activeDBList.keySet()) {
				dbListCombo.add(dbName);
			}
		} else {
			return;
		}
		dbListCombo.select(0);
	}

}
