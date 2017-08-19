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
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import scouter.client.Activator;
import scouter.client.message.M;
import scouter.client.util.UIUtil;
import scouter.client.xlog.views.XLogColumnEnum;
import scouter.util.CastUtil;

import java.util.HashMap;

public class ChartPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	GridData gdata;
	
	Text lineWidthTxt;
	Text xlogIgnoreTxt;
	Text xlogMxCntTxt;
	Text xlogMxDragCntTxt;

	int xLogIgnoreTime;
	int xLogMaxCount;
	int xLogDragMaxCount;

	HashMap<XLogColumnEnum, Boolean> xLogColumnVisibleMap = new HashMap<>();
	HashMap<XLogColumnEnum, Button> xLogColumnCheckButtonMap = new HashMap<>();

	private int lineWidth;

	public ChartPreferencePage() {
		super();
		noDefaultAndApplyButton();
		setDescription(M.PREFERENCE_EXPAND_CHART);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}
	
	@Override
	protected Control createContents(Composite parent) {
		
		((GridLayout)parent.getLayout()).marginBottom = 0;
		
		final Group layoutGroup = new Group(parent, SWT.NONE);
		layoutGroup.setText(M.PREFERENCE_CHARTSETTING);
	    
		layoutGroup.setLayout(UIUtil.formLayout(5, 5));
		gdata = new GridData();
		gdata.horizontalAlignment = SWT.FILL;		
		layoutGroup.setLayoutData(gdata);
	    
		IntegerVerifyListener verifyListener = new IntegerVerifyListener();

		Label lineWidthLbl = new Label(layoutGroup, SWT.RIGHT);
		lineWidthLbl.setText(M.PREFERENCE_CHARTLINE_WIDTH);
		lineWidthLbl.setLayoutData(UIUtil.formData(null, -1, 0, 0, null, -1, null, -1, 160));
		
		lineWidthTxt = new Text(layoutGroup, SWT.BORDER);
		lineWidthTxt.setText(""+lineWidth); //$NON-NLS-1$
		lineWidthTxt.setLayoutData(UIUtil.formData(lineWidthLbl, 10, 0, -2, 100, -5, null, -1));
		lineWidthTxt.addVerifyListener(verifyListener);

		Label ignoreLbl = new Label(layoutGroup, SWT.RIGHT);
		ignoreLbl.setText(M.PREFERENCE_CHARTXLOG_IGNORE_TIME);
		ignoreLbl.setLayoutData(UIUtil.formData(null, -1, lineWidthTxt, 5, null, -1, null, -1, 160));
		
		xlogIgnoreTxt = new Text(layoutGroup, SWT.BORDER);
		xlogIgnoreTxt.setText(Integer.toString(xLogIgnoreTime));
		xlogIgnoreTxt.setLayoutData(UIUtil.formData(ignoreLbl, 10, lineWidthTxt, 3, 100, -5, null, -1, 150));
		xlogIgnoreTxt.addVerifyListener(verifyListener);

		Label maxCntLbl = new Label(layoutGroup, SWT.RIGHT);
		maxCntLbl.setText(M.PREFERENCE_CHARTXLOG_MAX_COUNT);
		maxCntLbl.setLayoutData(UIUtil.formData(null, -1, xlogIgnoreTxt, 5, null, -1, null, -1, 160));
		
		xlogMxCntTxt = new Text(layoutGroup, SWT.BORDER);
		xlogMxCntTxt.setText(Integer.toString(xLogMaxCount));
		xlogMxCntTxt.setLayoutData(UIUtil.formData(ignoreLbl, 10, xlogIgnoreTxt, 3, 100, -5, null, -1, 150));
		xlogMxCntTxt.addVerifyListener(verifyListener);

		Label maxDragCntLbl = new Label(layoutGroup, SWT.RIGHT);
		maxDragCntLbl.setText(M.PREFERENCE_CHARTXLOG_MAX_DRAG_COUNT);
		maxDragCntLbl.setLayoutData(UIUtil.formData(null, -1, xlogMxCntTxt, 5, null, -1, null, -1, 160));

		xlogMxDragCntTxt = new Text(layoutGroup, SWT.BORDER);
		xlogMxDragCntTxt.setText(Integer.toString(xLogDragMaxCount));
		xlogMxDragCntTxt.setLayoutData(UIUtil.formData(ignoreLbl, 10, xlogMxCntTxt, 3, 100, -5, null, -1, 150));
		xlogMxDragCntTxt.addVerifyListener(verifyListener);
		
		//XLog column
		final Group layoutGroup2 = new Group(parent, SWT.NONE);
		layoutGroup2.setText("XLog Columns");
	    
		layoutGroup2.setLayout(UIUtil.formLayout(5, 5));
		GridData gdata2 = new GridData();
		gdata2.horizontalAlignment = SWT.FILL;		
		layoutGroup2.setLayoutData(gdata2);

		boolean isFirstCol = true;
		Label prevLabel = null;
		for (XLogColumnEnum xLogColumn : XLogColumnEnum.values()) {
			Label label = new Label(layoutGroup2, SWT.RIGHT);
			label.setText(xLogColumn.getTitle() + " :");
			label.setLayoutData(UIUtil.formData(null, -1, isFirstCol ? 0 : prevLabel, 1, null, -1, null, -1, 160));

			Button button = new Button(layoutGroup2, SWT.CHECK);
			button.setSelection(xLogColumnVisibleMap.get(xLogColumn));
			button.setLayoutData(UIUtil.formData(label, 10, isFirstCol ? 0 : prevLabel, -1, 100, -5, null, -1));
			xLogColumnCheckButtonMap.put(xLogColumn, button);
			
			prevLabel = label;
			isFirstCol = false;
		}

		return super.createContents(parent);
	}
	
	public void init(IWorkbench workbench) {
		lineWidth = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH); 
		xLogIgnoreTime = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_IGNORE_TIME);
		xLogMaxCount = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_MAX_COUNT);
		xLogDragMaxCount = PManager.getInstance().getInt(PreferenceConstants.P_XLOG_DRAG_MAX_COUNT);

		for (XLogColumnEnum xLogColumnEnum : XLogColumnEnum.values()) {
			xLogColumnVisibleMap.put(xLogColumnEnum, PManager.getInstance().getBoolean(xLogColumnEnum.getInternalID()));
		}
	}
	
	public boolean performOk() {
		PManager.getInstance().setValue(PreferenceConstants.P_CHART_LINE_WIDTH, CastUtil.cint(lineWidthTxt.getText()));
		PManager.getInstance().setValue(PreferenceConstants.P_XLOG_IGNORE_TIME, CastUtil.cint(xlogIgnoreTxt.getText()));
		PManager.getInstance().setValue(PreferenceConstants.P_XLOG_MAX_COUNT, CastUtil.cint(xlogMxCntTxt.getText()));
		PManager.getInstance().setValue(PreferenceConstants.P_XLOG_DRAG_MAX_COUNT, CastUtil.cint(xlogMxDragCntTxt.getText()));

		for (XLogColumnEnum xLogColumnEnum : XLogColumnEnum.values()) {
			PManager.getInstance().setValue(xLogColumnEnum.getInternalID(), xLogColumnCheckButtonMap.get(xLogColumnEnum).getSelection());
		}

		return true;
	}

	protected void createFieldEditors() {
	}
	
}