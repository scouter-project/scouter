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
package scouter.client.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import scouter.client.Activator;
import scouter.client.message.M;
import scouter.client.util.UIUtil;
import scouter.util.CastUtil;

public class ChartPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	GridData gdata;
	
	Text lineWidthTxt;
	Text xlogIgnoreTxt;
	Text xlogMxCntTxt;
	
	int xLogIgnoreTime;
	int xLogMaxCount;
	
	private int lineWidth;
	
	public ChartPreferencePage() {
		super();
		noDefaultAndApplyButton();
		setDescription(M.PREFERENCE_EXPAND_CHART);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}
	
	@Override
	protected Control createContents(Composite parent) {
		
		((GridLayout)parent.getLayout()).marginBottom = 30;
		
		final Group layoutGroup = new Group(parent, SWT.NONE);
	    layoutGroup.setText(M.PREFERENCE_CHARTSETTING);
	    
		layoutGroup.setLayout(UIUtil.formLayout(5, 5));
		gdata = new GridData();
		gdata.horizontalAlignment = SWT.FILL;		
		layoutGroup.setLayoutData(gdata);
	    
		IntegerVerifyListener verifyListener = new IntegerVerifyListener();

		
		Label lineWidthLbl = new Label(layoutGroup, SWT.RIGHT);
		lineWidthLbl.setText(M.PREFERENCE_CHARTLINE_WIDTH);
		lineWidthLbl.setLayoutData(UIUtil.formData(null, -1, 0, 10, null, -1, null, -1, 160));
		
		lineWidthTxt = new Text(layoutGroup, SWT.BORDER);
		lineWidthTxt.setText(""+lineWidth); //$NON-NLS-1$
		lineWidthTxt.setLayoutData(UIUtil.formData(lineWidthLbl, 10, 0, 8, 100, -5, null, -1));
		lineWidthTxt.addVerifyListener(verifyListener);

		Label ignoreLbl = new Label(layoutGroup, SWT.RIGHT);
		ignoreLbl.setText(M.PREFERENCE_CHARTXLOG_IGNORE_TIME);
		ignoreLbl.setLayoutData(UIUtil.formData(null, -1, lineWidthTxt, 10, null, -1, null, -1, 160));
		
		xlogIgnoreTxt = new Text(layoutGroup, SWT.BORDER);
		xlogIgnoreTxt.setText(Integer.toString(xLogIgnoreTime));
		xlogIgnoreTxt.setLayoutData(UIUtil.formData(ignoreLbl, 10, lineWidthTxt, 8, 100, -5, null, -1, 150));
		xlogIgnoreTxt.addVerifyListener(verifyListener);

		Label maxCntLbl = new Label(layoutGroup, SWT.RIGHT);
		maxCntLbl.setText(M.PREFERENCE_CHARTXLOG_MAX_COUNT);
		maxCntLbl.setLayoutData(UIUtil.formData(null, -1, xlogIgnoreTxt, 10, null, -1, null, -1, 160));
		
		xlogMxCntTxt = new Text(layoutGroup, SWT.BORDER);
		xlogMxCntTxt.setText(Integer.toString(xLogMaxCount));
		xlogMxCntTxt.setLayoutData(UIUtil.formData(ignoreLbl, 10, xlogIgnoreTxt, 8, 100, -5, null, -1, 150));
		xlogMxCntTxt.addVerifyListener(verifyListener);

		return super.createContents(parent);
	}
	
	public void init(IWorkbench workbench) {
		lineWidth = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH); 
		xLogIgnoreTime = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_IGNORE_TIME);
		xLogMaxCount = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_MAX_COUNT);
	}
	
	public boolean performOk() {
		PManager.getInstance().setValue(PreferenceConstants.P_CHART_LINE_WIDTH, CastUtil.cint(lineWidthTxt.getText()));
		PManager.getInstance().setValue(PreferenceConstants.P_XLOG_IGNORE_TIME, CastUtil.cint(xlogIgnoreTxt.getText()));
		PManager.getInstance().setValue(PreferenceConstants.P_XLOG_MAX_COUNT, CastUtil.cint(xlogMxCntTxt.getText()));
		return true;
	}

	protected void createFieldEditors() {
	}
	
}