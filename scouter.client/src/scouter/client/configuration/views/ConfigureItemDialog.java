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
package scouter.client.configuration.views;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import scouter.lang.conf.ValueType;
import scouter.util.StringUtil;

public class ConfigureItemDialog extends TitleAreaDialog {
	String confKey;
	String valueOrg;
	String objName;
	String desc;
	ValueType valueType;
	boolean isServer;

	String value;
	ConfApplyScopeEnum applyScope = ConfApplyScopeEnum.THIS;

	public ConfigureItemDialog(Shell parentShell, String confKey, String valueOrg, String objName, String desc, ValueType valueType, boolean isServer) {
		super(parentShell);
		this.confKey = confKey;
		this.objName = objName;
		this.desc = desc;
		this.valueType = (valueType == null) ? ValueType.VALUE : valueType;
		this.valueOrg = valueOrg;
		this.value = shape(valueOrg);
		this.isServer = isServer;
	}

	@Override
	public void create() {
		super.create();
		setTitle("\"" + confKey + "\" (" + objName + ")");

		if (StringUtil.isNotEmpty(desc)) {
			setMessage(desc, IMessageProvider.INFORMATION);
		} else {
			setMessage("Set the value of : " + confKey, IMessageProvider.INFORMATION);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container =  (Composite) super.createDialogArea(parent);
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(true);
		container.setLayout(new GridLayout(1, true));

		Group group = new Group(container, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		group.setLayout(new GridLayout(1, false));

//		Label label = new Label(filterGrp, SWT.NONE);
//		label.setText("Service");
//		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

		if (valueType == ValueType.VALUE) {
			Text serviceTxt = new Text(group, SWT.BORDER | SWT.SINGLE);
			serviceTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			serviceTxt.setText(value);
			serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());
		} else if (valueType == ValueType.COMMA_SEPARATED_VALUE) {
			Text serviceTxt = new Text(group, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			gridData.heightHint = 100;
			serviceTxt.setLayoutData(gridData);
			serviceTxt.setText(value);
			serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());
		} else if (valueType == ValueType.NUM) {
			Text serviceTxt = new Text(group, SWT.BORDER | SWT.SINGLE);
			serviceTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			serviceTxt.addVerifyListener(e -> e.doit = isNumber(e.text));
			serviceTxt.setText(value);
			serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());
		} else if (valueType == ValueType.BOOL) {
			Button button = new Button(group, SWT.CHECK);
			button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			button.setText(confKey);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						value = "true";
					} else {
						value = "false";
					}
				}
			});
			button.setSelection("true".equals(value));
		} else {
			Text serviceTxt = new Text(group, SWT.BORDER | SWT.SINGLE);
			serviceTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			serviceTxt.setText(value);
			serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());
		}

		if (!isServer) {
			Group applyTypeGroup = new Group(group, SWT.NONE);
			applyTypeGroup.setLayout(new RowLayout(SWT.VERTICAL));
			//applyTypeGroup.setLayout(new GridLayout(1, false));
			applyTypeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			applyTypeGroup.setText("Select title");

			Button rdOnlyThis = new Button(applyTypeGroup, SWT.RADIO);
			rdOnlyThis.setText("to this object (you should save it manually after done)");
			rdOnlyThis.setSelection(true);

			Button rdForType = new Button(applyTypeGroup, SWT.RADIO);
			rdForType.setText("to all same type objects in this collector.(the configuration will be saved automatically)");

			Button rdForTypeAll = new Button(applyTypeGroup, SWT.RADIO);
			rdForTypeAll.setText("to all same type objects for all collectors.(the configuration will be saved automatically)");

			rdOnlyThis.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						applyScope = ConfApplyScopeEnum.THIS;
					}
				}
			});

			rdForType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						applyScope = ConfApplyScopeEnum.TYPE_IN_SERVER;
					}
				}
			});

			rdForTypeAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (((Button) e.widget).getSelection()) {
						applyScope = ConfApplyScopeEnum.TYPE_ALL;
					}
				}
			});
		}

		return container;
	}

	private boolean isNumber(String v) {
		char[] chars = new char[v.length()];
		v.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			if (!('0' <= chars[i] && chars[i] <= '9')) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return getShell().computeSize(600, SWT.DEFAULT);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configure item detail");
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private String unShape(String value0) {
		if(valueType == ValueType.COMMA_SEPARATED_VALUE) {
			return value0.replaceAll("\\s+", ",");
		}
		return value0;
	}
	
	private String shape(String value0) {
		if(valueType == ValueType.COMMA_SEPARATED_VALUE) {
			return value0.replace(',', '\n');
		}
		return value0;
	}

	public String getValue() {
		return unShape(value);
	}

	public ConfApplyScopeEnum getApplyScope() {
		return applyScope;
	}
}
