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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import scouter.client.util.UIUtil;
import scouter.client.cubrid.ActiveDbInfo;
import scouter.client.cubrid.CubridSingleItem;
import scouter.client.cubrid.CubridTypeShotPeriod;
import scouter.client.cubrid.CubridSingleItem.InfoType;

public class AddRealTimeDialog {
	private final Display display;
	private final IAddSingleRealTimeDialog callback;
	private int serverId;
	
	Combo dbListCombo;
	Combo counterCombo;
	Combo timeRangeCombo;

	public AddRealTimeDialog(Display display, int serverId, IAddSingleRealTimeDialog callback) {
		this.display = display;
		this.callback = callback;
		this.serverId = serverId;
	}

	public void show(Point p) {
		if (p != null)
			show((int) p.getX(), (int) p.getY() + 10);
	}

	public void show() {
		show(UIUtil.getMousePosition());
	}

	public void show(int x, int y) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout(new GridLayout(2, true));
		dialog.setText("Add RealTime View");
		UIUtil.setDialogDefaultFunctions(dialog);

		Label Label1 = new Label(dialog, SWT.NONE);
		Label1.setLayoutData(new GridData(SWT.LEFT));
		Label1.setText("Counter Name:");
		counterCombo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		ArrayList<String> multiViewList = new ArrayList<String>();
		for (CubridSingleItem view : CubridSingleItem.values()) {
			multiViewList.add(view.getTitle());
		}
		counterCombo.setItems(multiViewList.toArray(new String[CubridSingleItem.values().length]));
		counterCombo.select(0);
		counterCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

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

		Label Label2 = new Label(dialog, SWT.NONE);
		Label2.setLayoutData(new GridData(SWT.LEFT));
		Label2.setText("DB Name:");
		dbListCombo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		dbListCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		dbLoad();

		Label Label3 = new Label(dialog, SWT.NONE);
		Label3.setLayoutData(new GridData(SWT.LEFT));
		Label3.setText("Time Range:");
		timeRangeCombo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		ArrayList<String> minuteStrList = new ArrayList<String>();
		for (CubridTypeShotPeriod minute : CubridTypeShotPeriod.values()) {
			minuteStrList.add(minute.getLabel());
		}
		timeRangeCombo.setItems(minuteStrList.toArray(new String[CubridTypeShotPeriod.values().length]));
		timeRangeCombo.select(0);
		timeRangeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						String counterTitle = counterCombo.getText();
						CubridSingleItem type = CubridSingleItem.fromCounterName(counterTitle);
						String afterMinute = timeRangeCombo.getText();
						CubridTypeShotPeriod m = CubridTypeShotPeriod.fromString(afterMinute);
						
						if (type.getInfoType() == InfoType.BROKER_INFO) { 
							callback.onPressedOk(type.getInfoType().getTitle(), type, m.getTime());
						} else {
							callback.onPressedOk(dbListCombo.getItem(dbListCombo.getSelectionIndex()), type, m.getTime());
						}
						dialog.close();

					} catch (Exception e) {
						MessageDialog.openError(dialog, "Error55", "format error:" + e.getMessage());
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

		if (x > 0 && y > 0) {
			dialog.setLocation(x, y);
		}

		dialog.open();
	}

	public interface IAddSingleRealTimeDialog {
		void onPressedOk(String dbName, CubridSingleItem viewType, long timeRange);

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
