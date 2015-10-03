/*
 *  Copyright 2015 the original author or authors.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.jdbc.util.BasicFormatterImpl;

import scouter.client.util.SqlFormatUtil;
import scouter.client.util.UIUtil;

public class SQLFormatDialog {
	
	public void show(final String message, final String error) {
		final Shell dialog = new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.RESIZE);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setText("SQL");
		dialog.setLayout(new GridLayout(1, true));
		StyledText errorTxt = new StyledText(dialog, SWT.MULTI | SWT.WRAP | SWT.BORDER);
		GridData gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		errorTxt.setLayoutData(gr);
		gr.exclude = true;
		errorTxt.setVisible(false);
		if (error != null) {
			gr.exclude = false;
			errorTxt.setVisible(true);
			errorTxt.setText(error);
			StyleRange sr = new StyleRange();
			sr.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
			sr.start = 0;
			sr.length = error.length();
			errorTxt.setStyleRange(sr);
		}
		final StyledText text = new StyledText(dialog, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 700;
		gd.heightHint = 500;
		text.setLayoutData(gd);
		SqlFormatUtil.applyStyledFormat(text, message);
		text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL) {
					if (e.keyCode == 'a' || e.keyCode == 'A') {
						text.selectAll();
					}
				}
			}
		});
		Composite bottomComp = new Composite(dialog, SWT.NONE);
		bottomComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		bottomComp.setLayout(UIUtil.formLayout(3, 3));
		
		Button btn = new Button(bottomComp, SWT.PUSH);
		btn.setLayoutData(UIUtil.formData(null, -1, null, -1, 100, -5, null, -1, 100));
		btn.setText("&Close");
		btn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dialog.close();
			}
		});
		
		final Button formatBtn = new Button(bottomComp, SWT.PUSH);
		formatBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, btn, -5, null, -1, 100));
		formatBtn.setText("&Format");
		formatBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				formatBtn.setEnabled(false);
				String formateed = new BasicFormatterImpl().format(text.getText());
				text.setText(formateed);
				formatBtn.setEnabled(true);
			}
		});
		
		dialog.pack();
		dialog.open();
	}
}
