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

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.util.DateUtil;

public class PopupMessageDialog {
	
	int keepSec;
	
	public void show (String from, String messsage, int keepSec) {
		this.keepSec = keepSec;
		show(from, messsage);
	}
	
	public void show(String from, String message) {
		final Shell dialog = new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setText(from + " - " + DateUtil.format(TimeUtil.getCurrentTime(), "HH:mm:ss"));
		dialog.setLayout(new GridLayout(1, true));
		if (keepSec > 0) {
			final Label label = new Label(dialog, SWT.NONE);
			label.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			label.setText("After " + keepSec + " sec, this message will be closed.");
			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					keepSec--;
					ExUtil.exec(Display.getDefault(), new Runnable() {
						public void run() {
							if (keepSec > 0) {
								label.setText("After " + keepSec + " sec, this message will be closed.");
							} else {
								dialog.close();
							}
						}
					});
				}
			}, 1000L, 1000L);
		}
		final Text text = new Text(dialog, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
		text.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		GridData gd = new GridData(400, 300);
		text.setLayoutData(gd);
		text.setText(message);
		final Button btn = new Button(dialog, SWT.PUSH);
		gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		gd.widthHint = 100;
		btn.setLayoutData(gd);
		btn.setText("&Close");
		btn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dialog.close();
			}
		});
		dialog.pack();
		
		Monitor primaryMonitor = Display.getDefault().getPrimaryMonitor ();
	    Rectangle bounds = primaryMonitor.getBounds ();
	    Rectangle rect = dialog.getBounds ();
	    int x = bounds.x + (bounds.width - rect.width) / 2 ;
	    int y = bounds.y + (bounds.height - rect.height) / 2 ;
	    dialog.setLocation (x, y);
		dialog.open();
	}
}
