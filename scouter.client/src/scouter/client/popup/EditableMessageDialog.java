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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import scouter.client.util.UIUtil;

public class EditableMessageDialog {
	
	Shell dialog;
	StyledText text;
	
	public void show(String title, String message) {
		show(title, message, new ArrayList<StyleRange>());
	}
	
	public void show(String title, final String message, LineStyleListener listener) {
		initLayout();
		dialog.setText(title);
		text.setText(message);
		if (listener != null) {
			text.addLineStyleListener(listener);
		}
		dialog.pack();
		dialog.open();
	}
	
	public void show(String title, final String message, List<StyleRange> srList) {
		initLayout();
		dialog.setText(title);
		text.setText(message);
		if (srList != null && srList.size() > 0) {
			text.setStyleRanges(srList.toArray(new StyleRange[srList.size()]));
		}
		dialog.pack();
		dialog.open();
	}
	
	private void initLayout() {
		dialog = new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.RESIZE);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setLayout(new GridLayout(1, true));
		text = new StyledText(dialog, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 400;
		gd.heightHint = 300;
		text.setLayoutData(gd);
		text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL || e.stateMask == SWT.COMMAND) {
					if (e.keyCode == 'a' || e.keyCode == 'A') {
						text.selectAll();
					}
				}
			}
		});
		Button btn = new Button(dialog, SWT.PUSH);
		gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		gd.widthHint = 100;
		btn.setLayoutData(gd);
		btn.setText("&Close");
		btn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dialog.close();
			}
		});
	}
}
