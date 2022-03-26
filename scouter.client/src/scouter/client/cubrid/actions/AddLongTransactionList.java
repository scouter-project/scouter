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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
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

public class AddLongTransactionList {
	private final Display display;
	private final IAddLongTransactionList callback;
	private final int serverId;
	
	Combo dbListCombo;

	public AddLongTransactionList(Display display, int serverId, IAddLongTransactionList callback) {
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
		dialog.setText("Add ListView");
		UIUtil.setDialogDefaultFunctions(dialog);

		Label Label2 = new Label(dialog, SWT.NONE);
		Label2.setLayoutData(new GridData(SWT.LEFT));
		Label2.setText("DB Name:");
		dbListCombo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		dbListCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		dbLoad();

		Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						callback.onPressedOk(dbListCombo.getItem(dbListCombo.getSelectionIndex()));
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

	public interface IAddLongTransactionList {
		void onPressedOk(String dbName);

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
