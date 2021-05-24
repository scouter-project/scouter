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
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
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
import scouter.client.Images;
import scouter.client.constants.HelpConstants;
import scouter.client.xlog.XLogFilterStatus;
import scouter.client.xlog.views.XLogViewCommon;
import scouter.util.StringUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class XLogFilterDialog extends Dialog {
	
	Combo objCombo;
	Text serviceTxt, ipTxt, startHmsFromTxt, startHmsToTxt, resTimeFromTxt, resTimeToTxt, userAgentTxt, loginText, descText;
	Text hasDumpYn, text1Text, text2Text, text3Text, text4Text, text5Text, profileSizeText, profileByteText;
	Button onlySqlBtn, onlyApiBtn, onlyErrorBtn;
	Button onlySyncBtn, onlyAsyncBtn;
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

		Button helpBtn = new Button(container, SWT.PUSH);
		helpBtn.setText("Help");
		helpBtn.setImage(Images.help);
		helpBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		helpBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(HelpConstants.HELP_URL_XLOG_FILTER_VIEW);
			}
		});

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

		//=============== respons time ==============================

		label = new Label(filterGrp, SWT.NONE);
		label.setText("Response(ms)");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

		Composite resTimeComposite = new Composite(filterGrp, SWT.NONE);
		resTimeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		FillLayout resTimeFilllayout = new FillLayout();
		resTimeFilllayout.marginWidth = 0;
		resTimeFilllayout.marginHeight = 0;
		resTimeComposite.setLayout(resTimeFilllayout);


		resTimeFromTxt = new Text(resTimeComposite, SWT.BORDER | SWT.SINGLE);
		resTimeFromTxt.setTextLimit(6);
		resTimeFromTxt.setText(status.responseTimeFrom);
		resTimeFromTxt.addVerifyListener(numberListener);
		resTimeFromTxt.addModifyListener(arg0 -> {
			newStatus.responseTimeFrom = resTimeFromTxt.getText();
			compareHash();
		});


		label = new Label(resTimeComposite, SWT.CENTER);
		label.setText(" ~ ");

		resTimeToTxt = new Text(resTimeComposite, SWT.BORDER | SWT.SINGLE);
		resTimeToTxt.setTextLimit(6);
		resTimeToTxt.setText(status.responseTimeTo);
		resTimeToTxt.addVerifyListener(numberListener);
		resTimeToTxt.addModifyListener(arg0 -> {
			newStatus.responseTimeTo = resTimeToTxt.getText();
			compareHash();
		});

		//=============================================

		label = new Label(filterGrp, SWT.NONE);
		label.setText("StartHMS");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

		Composite startTimeComposite = new Composite(filterGrp, SWT.NONE);
		startTimeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		FillLayout filllayout = new FillLayout();
		filllayout.marginWidth = 0;
		filllayout.marginHeight = 0;
		startTimeComposite.setLayout(filllayout);


		startHmsFromTxt = new Text(startTimeComposite, SWT.BORDER | SWT.SINGLE);
		startHmsFromTxt.setTextLimit(6);
		startHmsFromTxt.setText(status.startHmsFrom);
		startHmsFromTxt.addVerifyListener(hhmmssListener);
		startHmsFromTxt.addModifyListener(arg0 -> {
			newStatus.startHmsFrom = startHmsFromTxt.getText();
			compareHash();
		});


		label = new Label(startTimeComposite, SWT.CENTER);
		label.setText(" ~ ");

		startHmsToTxt = new Text(startTimeComposite, SWT.BORDER | SWT.SINGLE);
		startHmsToTxt.setTextLimit(6);
		startHmsToTxt.setText(status.startHmsTo);
		startHmsToTxt.addVerifyListener(hhmmssListener);
		startHmsToTxt.addModifyListener(arg0 -> {
			newStatus.startHmsTo = startHmsToTxt.getText();
			compareHash();
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
		label.setText("has dump YN");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		hasDumpYn = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		hasDumpYn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		hasDumpYn.setText(status.hasDumpYn);
		hasDumpYn.addVerifyListener(ynListener);
		hasDumpYn.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.hasDumpYn = hasDumpYn.getText();
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

		label = new Label(filterGrp, SWT.NONE);
		label.setText("TEXT3");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		text3Text = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		text3Text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		text3Text.setText(status.text3);
		text3Text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.text3 = text3Text.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("TEXT4");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		text4Text = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		text4Text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		text4Text.setText(status.text4);
		text4Text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.text4 = text4Text.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("TEXT5");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		text5Text = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		text5Text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		text5Text.setText(status.text5);
		text5Text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.text5 = text5Text.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("PROFILE-SIZE");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		profileSizeText = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		profileSizeText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		profileSizeText.setText(status.profileSizeText);
		profileSizeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.profileSizeText = profileSizeText.getText();
				compareHash();
			}
		});

		label = new Label(filterGrp, SWT.NONE);
		label.setText("PROFILE-BYTE");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		profileByteText = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		profileByteText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		profileByteText.setText(status.profileBytesText);
		profileByteText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				newStatus.profileBytesText = profileByteText.getText();
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

		onlySyncBtn = new Button(checkGroup, SWT.CHECK);
		onlySyncBtn.setText("Sync");
		onlySyncBtn.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		onlySyncBtn.setSelection(status.onlySync);
		onlySyncBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				newStatus.onlySync = onlySyncBtn.getSelection();
				compareHash();
			}
		});

		onlyAsyncBtn = new Button(checkGroup, SWT.CHECK);
		onlyAsyncBtn.setText("Async");
		onlyAsyncBtn.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		onlyAsyncBtn.setSelection(status.onlyAsync);
		onlyAsyncBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				newStatus.onlyAsync = onlyAsyncBtn.getSelection();
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
				startHmsFromTxt.setText("");
				startHmsToTxt.setText("");
				resTimeFromTxt.setText("");
				resTimeToTxt.setText("");
				loginText.setText("");
				descText.setText("");
				hasDumpYn.setText("");
				text1Text.setText("");
				text2Text.setText("");
				text3Text.setText("");
				text4Text.setText("");
				text5Text.setText("");
				userAgentTxt.setText("");
				onlySqlBtn.setSelection(false);
				onlyApiBtn.setSelection(false);
				onlyErrorBtn.setSelection(false);
				onlySyncBtn.setSelection(false);
				onlyAsyncBtn.setSelection(false);
				profileSizeText.setText("");
				profileByteText.setText("");
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
				String startHmsFrom = startHmsFromTxt.getText();
				if(startHmsFrom.length() > 0 && startHmsFrom.length() < 6) {
					for(int i = startHmsFrom.length(); i < 6; i++) {
						startHmsFrom += '0';
					}
					startHmsFromTxt.setText(startHmsFrom);
				}

				String startHmsTo = startHmsToTxt.getText();
				if(startHmsTo.length() > 0 && startHmsTo.length() < 6) {
					for(int i = startHmsTo.length(); i < 6; i++) {
						startHmsTo += '0';
					}
					startHmsToTxt.setText(startHmsTo);
				}

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
			view.setFilter(newStatus);
			filterHash = newStatus.hashCode();
			compareHash();
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

	VerifyListener hhmmssListener = e -> {
		if (!StringUtil.isInteger(e.text) && !StringUtil.isEmpty(e.text)) {
			e.doit = false;
			return;
		}

		Text text = (Text) e.getSource();
		final String prev = text.getText();
		String after = prev.substring(0, e.start) + e.text + prev.substring(e.end);

		for(int i = after.length(); i < 6; i++) {
			after += '0';
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
		try {
			LocalTime.parse(after, formatter);
		} catch (DateTimeParseException ignore) {
			e.doit = false;
		}

	};

	VerifyListener numberListener = e -> {
		if (!StringUtil.isInteger(e.text) && !StringUtil.isEmpty(e.text)) {
			e.doit = false;
			return;
		}
	};

	VerifyListener ynListener = e -> {
		e.text = e.text.toUpperCase();
		if (!"".equals(e.text) && !"Y".equals(e.text) && !"N".equals(e.text)) {
			e.doit = false;
			return;
		}
	};
}
