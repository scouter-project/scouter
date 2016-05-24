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
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import scouter.client.util.SqlMakerUtil;
import scouter.client.util.UIUtil;
import scouter.util.StringUtil;

public class SQLFormatDialog {
	public void show(final String message, final String error){
		show(message, error, null);
	}
	
	public void show(final String message, final String error, final String params) {
		final Shell dialog = new Shell(Display.getDefault(), SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
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
		if(params == null){
			SqlFormatUtil.applyStyledFormat(text, message);
		}else{
			SqlFormatUtil.applyStyledFormat(text, SqlMakerUtil.bindSQL(message, params));
		}
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
				
				String inputSQL = text.getText();
				String bindVariables = null;
				
				int search = -1;
				if(inputSQL != null){
					search = inputSQL.indexOf(SqlMakerUtil.SQLDIVIDE);
					if(search >= 0){
						bindVariables = inputSQL.substring(search);
						inputSQL = inputSQL.substring(0, search);
					}
				}
				String formateed = new BasicFormatterImpl().format(inputSQL);
				if(search >=0){
					formateed = formateed + bindVariables;
				}
				
				text.setText(formateed);
				formatBtn.setEnabled(true);
			}
		});

		final Button copyBtn = new Button(bottomComp, SWT.PUSH);
		copyBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, formatBtn, -5, null, -1, 100));
		copyBtn.setText("&Copy");
		copyBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
					Clipboard clipboard = new Clipboard(Display.getDefault());
					TextTransfer textTransfer = TextTransfer.getInstance();
					clipboard.setContents(new String[]{text.getText()}, new Transfer[]{textTransfer});
					clipboard.dispose();
					MessageDialog.openInformation(dialog, "Copy", "Copied to clipboard");
			}
		});
		
		final Button bindBtn = new Button(bottomComp, SWT.PUSH);
		bindBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, copyBtn, -5, null, -1, 100));
		bindBtn.setText("&Bind");
		bindBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				text.setText(SqlMakerUtil.replaceSQLParameter(message, params));
			}
		});
		
		if (StringUtil.isEmpty(params)) {
			bindBtn.setEnabled(false);
		}
		
		dialog.pack();
		dialog.open();
	}
}
