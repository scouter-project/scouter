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
package scouter.client.xlog.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import scouter.client.xlog.XLogFilterStatus;
import scouter.client.xlog.views.XLogViewCommon;

public class XLogFilterDialog extends Dialog {
	
	Combo objCombo;
	Text serviceTxt, ipTxt, userAgentTxt, loginText, descText, text1Text, text2Text;
	Button onlySqlBtn, onlyApiBtn, onlyErrorBtn;
	Button clearBtn, applyBtn;
	
	XLogViewCommon view;
	XLogFilterStatus status;
	XLogFilterStatus newStatus;
	int filterHash;

	public XLogFilterDialog(XLogViewCommon view) {
		super(view.getSite().getShell());
		this.view = view;
	}
	
	public void setStatus(XLogFilterStatus status) {
		this.status = status;
	}

	protected Control createDialogArea(Composite parent) {
		Composite container =  (Composite) super.createDialogArea(parent);
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
		newStatus = status.clone();
		this.filterHash = status.hashCode();
		container.setLayout(new GridLayout(1, true));
		Group filterGrp = new Group(container, SWT.NONE);
		filterGrp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filterGrp.setLayout(new GridLayout(2, false));
		
		Label label = new Label(filterGrp, SWT.NONE);
		label.setText("Object");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		objCombo = new Combo(filterGrp, SWT.VERTICAL| SWT.BORDER |SWT.H_SCROLL);
		objCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		objCombo.setItems(view.getExistObjNames());
		objCombo.setText(status.objName);
		objCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.objName = objCombo.getText();
				compareHash();
			}
		});
		
		label = new Label(filterGrp, SWT.NONE);
		label.setText("Service");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		serviceTxt = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		serviceTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		serviceTxt.setText(status.service);
		serviceTxt.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.service = serviceTxt.getText();
				compareHash();
			}
		});
		
		label = new Label(filterGrp, SWT.NONE);
		label.setText("IP");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		ipTxt = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		ipTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ipTxt.setText(status.ip);
		ipTxt.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.ip = ipTxt.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("LOGIN");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		loginText = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		loginText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		loginText.setText(status.login);
		loginText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.login = loginText.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("DESC");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		descText = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		descText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		descText.setText(status.desc);
		descText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.desc = descText.getText();
				compareHash();
			}
		});
		
		label = new Label(filterGrp, SWT.NONE);
		label.setText("User-Agent");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		userAgentTxt = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		userAgentTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		userAgentTxt.setText(status.userAgent);
		userAgentTxt.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.userAgent = userAgentTxt.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("TEXT1");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		text1Text = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		text1Text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		text1Text.setText(status.text1);
		text1Text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.text1 = text1Text.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("TEXT2");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		text2Text = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		text2Text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		text2Text.setText(status.text2);
		text2Text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.text2 = text2Text.getText();
				compareHash();
			}
		});
		
		Group checkGroup = new Group(filterGrp, SWT.NONE);
		checkGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		checkGroup.setLayout(new GridLayout(3, true));
		
		onlySqlBtn = new Button(checkGroup, SWT.CHECK);
		onlySqlBtn.setText("SQL");
		onlySqlBtn.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		onlySqlBtn.setSelection(status.onlySql);
		onlySqlBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				newStatus.onlySql = onlySqlBtn.getSelection();
				compareHash();
			}
		});
		
		onlyApiBtn = new Button(checkGroup, SWT.CHECK);
		onlyApiBtn.setText("ApiCall");
		onlyApiBtn.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		onlyApiBtn.setSelection(status.onlyApicall);
		onlyApiBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				newStatus.onlyApicall = onlyApiBtn.getSelection();
				compareHash();
			}
		});
		
		onlyErrorBtn = new Button(checkGroup, SWT.CHECK);
		onlyErrorBtn.setText("Error");
		onlyErrorBtn.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		onlyErrorBtn.setSelection(status.onlyError);
		onlyErrorBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				newStatus.onlyError = onlyErrorBtn.getSelection();
				compareHash();
			}
		});
		
		Composite btnComp = new Composite(container, SWT.NONE);
		btnComp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		btnComp.setLayout(new RowLayout());
		
		RowData rd = new RowData();
		rd.width = 90;
		clearBtn = new Button(btnComp, SWT.PUSH);
		clearBtn.setLayoutData(rd);
		clearBtn.setText("&Clear");
		clearBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				objCombo.setText("");
				serviceTxt.setText("");
				ipTxt.setText("");
				loginText.setText("");
				descText.setText("");
				text1Text.setText("");
				text2Text.setText("");
				userAgentTxt.setText("");
				onlySqlBtn.setSelection(false);
				onlyApiBtn.setSelection(false);
				onlyErrorBtn.setSelection(false);
				newStatus = new XLogFilterStatus();
				if (newStatus.hashCode() != filterHash) {
					applyBtn.setEnabled(true);
				}
			}
		});
		applyBtn = new Button(btnComp, SWT.PUSH);
		applyBtn.setLayoutData(rd);
		applyBtn.setText("&Apply");
		applyBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				view.setFilter(newStatus);
				filterHash = newStatus.hashCode();
				compareHash();
			}
		});
		applyBtn.setEnabled(false);
		return container;
	}
	
	private void compareHash() {
		if (newStatus.hashCode() != filterHash) {
			applyBtn.setEnabled(true);
		} else {
			applyBtn.setEnabled(false);
		}
	}
	
	
	
	@Override
	protected void okPressed() {
		if (newStatus.hashCode() != filterHash) {
		}
		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return getShell().computeSize(300, SWT.DEFAULT);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("XLog Filter");
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}
