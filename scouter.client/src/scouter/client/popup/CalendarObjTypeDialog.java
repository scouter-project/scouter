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

import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import scouter.client.Images;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ImageCombo;
import scouter.client.util.UIUtil;
import scouter.lang.counters.CounterEngine;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class CalendarObjTypeDialog {
	
	private final Display display;
	private final ICalendarCallback callback;
	private final int serverId;
	private final CounterEngine counterEngine;
	private ImageCombo objTypeCombo;
	private String defObjType;
	
	public CalendarObjTypeDialog(Display display, ICalendarCallback callback, int serverId, String objType) {
		this.display = display;
		this.callback = callback;
		this.serverId = serverId;
		if(StringUtil.isEmpty(objType))
			this.defObjType=PManager.getInstance().getString(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_WAS);
		else
			this.defObjType = objType;
		
		counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
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
		calendar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		
		if (time > 0) {
			String yyyymmdd = DateUtil.format(time, "yyyy-MM-dd");
			String[] date = yyyymmdd.split("-");
			calendar.setDate(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));
		}
		
		objTypeCombo = new ImageCombo(dialog, SWT.BORDER | SWT.READ_ONLY);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1);
		gd.heightHint = 15;
		objTypeCombo.setLayoutData(gd);
		objTypeCombo.setBackground(ColorUtil.getInstance().getColor("white"));
		ArrayList<String> objTypeList = counterEngine.getAllObjectType();
		//String defObjType = this.objType!=null?this.objType:PManager.getInstance().getString(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_WAS);
		for (int i = 0; i < objTypeList.size() ; i++) {
			String objType = objTypeList.get(i);
			String displayName = counterEngine.getDisplayNameObjectType(objType);
			if (StringUtil.isEmpty(displayName)) continue;
			objTypeCombo.add(displayName, Images.getObjectIcon(objType, true, serverId));
			objTypeCombo.setData(displayName, objType);
			if (defObjType.equals(objType)) {
				objTypeCombo.select(i);
			}
		}
		
		
		Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						 String objType = CastUtil.cString(objTypeCombo.getData(objTypeCombo.getText()));
						 if (StringUtil.isEmpty(objType)) {
							 MessageDialog.openError(dialog, "Requirement", "Select Object Type");
							 return;
						 }
						 String date = (calendar.getMonth () + 1) + "/" + calendar.getDay() + "/" + calendar.getYear();
						 date = DateUtil.format(DateUtil.getTime(date, "MM/dd/yyyy"), "yyyyMMdd");
						 callback.onPressedOk(date, objType);
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
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
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
		
		if(x > 0 && y > 0){
			dialog.setLocation(x, y);
		}
		
		dialog.open();
	}
	
	public interface ICalendarCallback {
		void onPressedOk(String date, String objType);
	}
	
}
