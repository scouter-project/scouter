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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class ConfigureItemDialog extends Dialog {
	String confKey;
	String value;

	public ConfigureItemDialog(Shell parentShell, String confKey) {
		super(parentShell);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container =  (Composite) super.createDialogArea(parent);
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(true);

		container.setLayout(new GridLayout(1, true));
		Group filterGrp = new Group(container, SWT.NONE);
		filterGrp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filterGrp.setLayout(new GridLayout(2, false));

		Label label = new Label(filterGrp, SWT.NONE);
		label.setText("Service");
		label.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		Text serviceTxt = new Text(filterGrp, SWT.BORDER | SWT.SINGLE);
		serviceTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		serviceTxt.setText("asdfasfd");
		serviceTxt.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				value = ((Text)arg0.getSource()).getText();
			}
		});

		return container;
	}

	@Override
	protected void okPressed() {
		System.out.println(value);
		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return getShell().computeSize(300, SWT.DEFAULT);
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
}
