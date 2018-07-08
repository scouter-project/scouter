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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import scouter.Version;
import scouter.client.Activator;
import scouter.client.Images;
import scouter.client.message.M;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.RCPUtil;
import scouter.client.util.UIUtil;
import scouter.lang.counters.CounterConstants;
import scouter.lang.counters.CounterEngine;
import scouter.util.CastUtil;
import scouter.util.ObjectUtil;


public class GeneralPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	ComboFieldEditor serverIP;
	
	Combo /*addrCombo,*/ hostCombo,javaeeCombo;
	
	Text file, color, maxText,  alertDialogTimeout, txtLinkName, txtLinkPattern;
	
	String filePath = "";
	String colorRgb = "";
	
	private String host;
	private String javaee;
	
	private int maxBlock;
	
	int alertdialogTimeoutSec = -1;
	
	public GeneralPreferencePage() {
		super();
		noDefaultAndApplyButton();
		setDescription(M.PREFERENCE_EXPAND);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		
//		addrList = new ArrayList<String>(Arrays.asList(ServerPrefUtil.getSvrAddrArrayFromPreference(PreferenceConstants.P_SVR_ADDRESSES)));
//		addrLoginList = new ArrayList<String>(Arrays.asList(ServerPrefUtil.getSvrAddrArrayFromPreference(PreferenceConstants.P_SVR_LOGIN_ADDRESSES)));
	}
	
	@Override
	protected Control createContents(Composite parent) {
		
		((GridLayout)parent.getLayout()).marginBottom = 30;
		
		Label versionLabel = new Label(parent, SWT.NONE);
		versionLabel.setText(" - Current Version : "+Version.getClientFullVersion());
		versionLabel.setLayoutData(UIUtil.gridData(SWT.FILL));
	    
		// ----Default Object Type----
		Group layoutGroup = new Group(parent, SWT.NONE);
	    layoutGroup.setText("Default Object Type");
		layoutGroup.setLayout(UIUtil.formLayout(5, 5));
		layoutGroup.setLayoutData(UIUtil.gridData(SWT.FILL));
	    
		CounterEngine counterEngine = ServerManager.getInstance().getDefaultServer().getCounterEngine();
	    hostCombo = new Combo(layoutGroup, SWT.VERTICAL| SWT.BORDER |SWT.H_SCROLL);
	    hostCombo.setItems(counterEngine.getChildren(CounterConstants.FAMILY_HOST));
	    hostCombo.setText(host);
		hostCombo.setEnabled(true);
		hostCombo.setLayoutData(UIUtil.formData(null, -1, 0, 8, 100, -5, null, -1, 220));
		
		CLabel hostLabel = new CLabel(layoutGroup, SWT.NONE);
		hostLabel.setText("default \'Host\'");
		hostLabel.setImage(Images.getObjectIcon(CounterConstants.FAMILY_HOST, true, 0));
		hostLabel.setLayoutData(UIUtil.formData(null, -1, 0, 8, hostCombo, -5, null, -1, 130));
		
		javaeeCombo = new Combo(layoutGroup, SWT.VERTICAL| SWT.BORDER |SWT.H_SCROLL);
		javaeeCombo.setItems(counterEngine.getChildren(CounterConstants.FAMILY_JAVAEE));
		javaeeCombo.setText(javaee);
		javaeeCombo.setEnabled(true);
		javaeeCombo.setLayoutData(UIUtil.formData(null, -1, hostCombo, 8, 100, -5, null, -1, 220));
		
		CLabel javaLabel = new CLabel(layoutGroup, SWT.NONE);
		javaLabel.setText("default \'JavaEE\'");
		javaLabel.setImage(Images.getObjectIcon(CounterConstants.JAVA, true, 0));
		javaLabel.setLayoutData(UIUtil.formData(null, -1, hostLabel, 8, javaeeCombo, -5, null, -1, 130));
		
		
		// ----Mass Profiling----
		layoutGroup = new Group(parent, SWT.NONE);
	    layoutGroup.setText("Profiling");
		layoutGroup.setLayout(UIUtil.formLayout(5, 5));
		layoutGroup.setLayoutData(UIUtil.gridData(SWT.FILL));
		
		maxText = new Text(layoutGroup, SWT.BORDER | SWT.RIGHT);
		maxText.setText(""+maxBlock);
		maxText.setBackground(ColorUtil.getInstance().getColor("white"));
		maxText.setLayoutData(UIUtil.formData(null, -1, 0, -2, 100, -5, null, -1, 265));
		maxText.addVerifyListener(new VerifyListener() { // for number only input.
	        public void verifyText(VerifyEvent e) {
	            Text text = (Text)e.getSource();
	            final String oldS = text.getText();
	            String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
	            boolean isFloat = true;
				try {
					Float.parseFloat(newS);
				} catch (NumberFormatException ex) {
					isFloat = false;
				}
	            if(!isFloat)
	                e.doit = false;
	        }
	    });
		
		Label label = new Label(layoutGroup, SWT.NONE);
        label.setText("Max Block count:");
		label.setLayoutData(UIUtil.formData(null, -1, null, -1, maxText, -5, null, -1, 100));

		// ----external link----
//		layoutGroup = new Group(parent, SWT.NONE);
//		layoutGroup.setText("External link setting(for connecting 3rd party UI)");
//		layoutGroup.setLayout(UIUtil.formLayout(5, 5));
//		layoutGroup.setLayoutData(UIUtil.gridData(SWT.FILL));
//
//		txtLinkName = new Text(layoutGroup, SWT.BORDER | SWT.RIGHT);
//		txtLinkName.setText("");
//		txtLinkName.setBackground(ColorUtil.getInstance().getColor("white"));
//		txtLinkName.setLayoutData(UIUtil.formData(null, -1, 0, -2, 100, -5, null, -1, 265));
//
//		Label lblLinkTitle = new Label(layoutGroup, SWT.NONE);
//		lblLinkTitle.setText("* Link title");
//		lblLinkTitle.setLayoutData(UIUtil.formData(null, -1, null,-1, txtLinkName, -5, null, -1, 100));
//
//		Label lblLinkPattern = new Label(layoutGroup, SWT.NONE);
//		lblLinkPattern.setText("* Link url pattern");
//		lblLinkPattern.setLayoutData(UIUtil.formData(null, 0, lblLinkTitle, 10, null, 0, null, -1, 100));
//
//		txtLinkPattern = new Text(layoutGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
//		txtLinkPattern.setText("");
//		txtLinkPattern.setBackground(ColorUtil.getInstance().getColor("white"));
//		txtLinkPattern.setLayoutData(UIUtil.formData(null, 0, lblLinkPattern, 3, null, 0, null, -1, 365, 45));
//
//		Label lblPatternHint = new Label(layoutGroup, SWT.NONE);
//		lblPatternHint.setText("   - variables: ${from}, ${to}, ${objHashes}, ${objType}");
//		lblPatternHint.setLayoutData(UIUtil.formData(null, 0, txtLinkPattern, 0, null, 0, null, -1, 365));

//		layoutGroup = new Group(parent, SWT.NONE);
//	    layoutGroup.setText("Alert");
//		layoutGroup.setLayout(UIUtil.formLayout(5, 5));
//		layoutGroup.setLayoutData(UIUtil.gridData(SWT.FILL));
		
//		Label alertDialogTimeoutLabel = new Label(layoutGroup, SWT.NONE | SWT.RIGHT);
//		alertDialogTimeoutLabel.setText("Set alert dialog timeout in seconds. \'-1\' will not destroy dialog.");
//		alertDialogTimeoutLabel.setLayoutData(UIUtil.formData(null, -1, null, -1, 100, -5, null, -1));
//		
//		Label secLbl = new Label(layoutGroup, SWT.NONE);
//		secLbl.setText("sec.");
//		secLbl.setLayoutData(UIUtil.formData(null, -1, alertDialogTimeoutLabel, 7, 100, -5, null, -1, 40));
//		
//		alertDialogTimeout = new Text(layoutGroup, SWT.BORDER | SWT.RIGHT);
//		alertDialogTimeout.setText(""+alertdialogTimeoutSec);
//		alertDialogTimeout.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
//		alertDialogTimeout.setLayoutData(UIUtil.formData(null, -1, alertDialogTimeoutLabel, 5, secLbl, -5, null, -1, 220));
		
		return super.createContents(parent);
	}

	public void init(IWorkbench workbench) {
		host = PManager.getInstance().getString(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_HOST);
		javaee = PManager.getInstance().getString(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_WAS);
		maxBlock = PManager.getInstance().getInt(PreferenceConstants.P_MASS_PROFILE_BLOCK);
		//alertdialogTimeoutSec = PManager.getInstance().getInt(PreferenceConstants.P_ALERT_DIALOG_TIMEOUT);
	}
	
	@Override
	public boolean performOk() {

		boolean needResetPerspective = false;
		
		if (!ObjectUtil.equals(javaee, javaeeCombo.getText())) {
			needResetPerspective = true;
		}
		
		if (!ObjectUtil.equals(host, hostCombo.getText())) {
			needResetPerspective = true;
		}
		
		if (needResetPerspective 
				&& !MessageDialog.openConfirm(getShell(), "Reset Perspectives", "To apply \'Default Object Type\', all perspectives will be reset. Continue?")) {
			return false;
		}
		PManager.getInstance().setValue(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_HOST, hostCombo.getText());
		PManager.getInstance().setValue(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_WAS, javaeeCombo.getText());
		PManager.getInstance().setValue(PreferenceConstants.P_MASS_PROFILE_BLOCK, CastUtil.cint(maxText.getText()));
		//PManager.getInstance().setValue(PreferenceConstants.P_ALERT_DIALOG_TIMEOUT, CastUtil.cint(alertDialogTimeout.getText()));
		
		if (needResetPerspective) {
			RCPUtil.resetPerspective();
		}
		return true;
	}
	
	protected void createFieldEditors() {
	}
}