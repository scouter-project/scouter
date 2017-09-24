/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.configuration.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ColoringWord;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.JavaLineStyler;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.HashUtil;
import scouter.util.StringUtil;

import java.util.ArrayList;

public class AlertScriptingView extends ViewPart {
	private enum SaveResult {
		NA, SUCCESS, FAIL
	}

	public static final String ID = AlertScriptingView.class.getName();

	private static final String DEFAULT_RULE_CONTENTS =
			"// void process(RealCounter $counter, PluginHelper $$) {\n"+
			"// create your java code below..\n"+
			"\n"+
			"\n"+
			"// }";

	private static final String DEFAULT_CONFIG_CONTENTS =
			"#history_size=150\n" +
			"#silent_time=300\n" +
			"#check_term=20";

	private StyledText ruleText;
	private StyledText configText;

	private int serverId;
	private String familyName;
	private String counterName;
	private String counterDisplayName;
	private String displayName;

	SashForm mainSashForm;
	SashForm bottomSashForm;

	int ruleContentsHash = 0;
	int configContentsHash = 0;

	private ArrayList<ColoringWord> configKeyword;

	@Override
	public void setFocus() {
//		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
//		slManager.setMessage("CTRL + S : save configurations, CTRL + A : select all text");
	}

	public void createPartControl(Composite parent) {
		initializeColoring();

		parent.setLayout(new FillLayout());
		mainSashForm = new SashForm(parent, SWT.VERTICAL);
		mainSashForm.SASH_WIDTH = 1;

		createRuleContentsForm(mainSashForm);

		bottomSashForm = new SashForm(mainSashForm, SWT.HORIZONTAL);
		bottomSashForm.SASH_WIDTH = 1;

		createConfigContentsForm(bottomSashForm);
		createConsoleForm(bottomSashForm);

		bottomSashForm.setWeights(new int[] {1, 3});
		bottomSashForm.setMaximizedControl(null);

		mainSashForm.setWeights(new int[] {2, 1});
		mainSashForm.setMaximizedControl(null);

		initialToolBar();
	}

	public void setInput(int serverId, String familyName, String counterName, String counterDisplayName){
		this.serverId = serverId;
		this.familyName = familyName;
		this.counterName = counterName;
		this.counterDisplayName = counterDisplayName;

		Server server = ServerManager.getInstance().getServer(serverId);
		if (server != null) {
			this.displayName = server.getName();
			setPartName("Alert Scripting [" + this.familyName + " : " + this.counterDisplayName + "]");
			loadAlertScriptingContents();
			loadAlertScriptingConfigContents();
		}
	}

	public void initializeColoring(){
		configKeyword = new ArrayList<>();
		configKeyword.add(new ColoringWord("history_size", SWT.COLOR_BLUE, true));
		configKeyword.add(new ColoringWord("silent_time", SWT.COLOR_BLUE, true));
		configKeyword.add(new ColoringWord("check_term", SWT.COLOR_BLUE, true));
	}

	private class SaveKeyListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent keyEvent) {
			if(keyEvent.stateMask == SWT.CTRL || keyEvent.stateMask == SWT.COMMAND){
				if(keyEvent.keyCode == 's' || keyEvent.keyCode == 'S') {
					saveContents();
				}
			}
		}
		@Override
		public void keyReleased(KeyEvent keyEvent) {}
	}

	private void saveContents() {
		SaveResult configResult = saveConfigContents();
		if (configResult == SaveResult.SUCCESS || configResult == SaveResult.NA) {
			SaveResult ruleResult = saveRuleContents();
			if (ruleResult == SaveResult.FAIL) {
				openSaveFailDialog();
			} else if (ruleResult == SaveResult.SUCCESS || configResult == SaveResult.SUCCESS) {
				openSaveSuccessDialog();
			}
		} else {
			openSaveFailDialog();
		}
	}

	private void openSaveSuccessDialog() {
		MessageDialog.open(MessageDialog.INFORMATION
				, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
				, "Success"
				, "Successfully saved."
				, SWT.NONE);
	}

	private void openSaveFailDialog() {
		MessageDialog.open(MessageDialog.ERROR
				, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
				, "Fail"
				, "Save failed!"
				, SWT.NONE);
	}

	private SaveResult saveConfigContents() {
		String contents = ruleText.getText();
		if (HashUtil.hash(contents) == ruleContentsHash) {
			return SaveResult.NA;
		}
		MapPack paramPack = new MapPack();
		paramPack.put("counterName", this.counterName);
		paramPack.put("contents", contents);
		MapPack resultPack = getResultMapPack(RequestCmd.SAVE_ALERT_SCRIPTING_CONTETNS, paramPack);
		if (resultPack.getBoolean("success")) {
			ruleContentsHash = HashUtil.hash(contents);
			return SaveResult.SUCCESS;
		} else {
			return SaveResult.FAIL;
		}
	}

	private SaveResult saveRuleContents() {
		String contents = configText.getText();
		if (HashUtil.hash(contents) == configContentsHash) {
			return SaveResult.NA;
		}
		MapPack paramPack = new MapPack();
		paramPack.put("counterName", this.counterName);
		paramPack.put("contents", contents);
		MapPack resultPack = getResultMapPack(RequestCmd.SAVE_ALERT_SCRIPTING_CONFIG_CONTETNS, paramPack);
		if (resultPack.getBoolean("success")) {
			ruleContentsHash = HashUtil.hash(contents);
			return SaveResult.SUCCESS;
		} else {
			return SaveResult.FAIL;
		}
	}

	private void createRuleContentsForm(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, true));

		ruleText = new StyledText(comp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		ruleText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ruleText.addLineStyleListener(new JavaLineStyler());

		ruleText.addKeyListener(new SaveKeyListener());
	}

	private void createConfigContentsForm(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, true));

		configText = new StyledText(comp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		configText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		configText.addLineStyleListener(new CustomLineStyleListener(true, configKeyword, true));
		configText.addKeyListener(new SaveKeyListener());
	}

	private void createConsoleForm(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, true));

		StyledText contentsText = new StyledText(comp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		contentsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	private void initialToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(new Action("Save", ImageUtil.getImageDescriptor(Images.save)) {
			@Override
			public void run() {
				saveContents();
			}
		});
	}

	private MapPack getResultMapPack(final String requestCmd, final MapPack param) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			return (MapPack) tcp.getSingle(requestCmd, param);
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}

	private void loadAlertScriptingContents() {
		MapPack param = new MapPack();
		param.put("counterName", this.counterName);
		ExUtil.asyncRun(() -> {
			MapPack resultMapPack = getResultMapPack(RequestCmd.GET_ALERT_SCRIPTING_CONTETNS, param);
			if (resultMapPack != null) {
				String contents = StringUtil.emptyToDefault(resultMapPack.getText("contents"), DEFAULT_RULE_CONTENTS);
				ExUtil.exec(ruleText, () -> ruleText.setText(contents));
				ruleContentsHash = HashUtil.hash(contents);
			}
		});
	}

	private void loadAlertScriptingConfigContents() {
		MapPack param = new MapPack();
		param.put("counterName", this.counterName);
		ExUtil.asyncRun(() -> {
			MapPack resultMapPack = getResultMapPack(RequestCmd.GET_ALERT_SCRIPTING_CONFIG_CONTETNS, param);
			if (resultMapPack != null) {
				String contents = StringUtil.emptyToDefault(resultMapPack.getText("contents"), DEFAULT_CONFIG_CONTENTS);
				ExUtil.exec(configText, () -> configText.setText(contents));
				configContentsHash = HashUtil.hash(contents);
			}
		});
	}
}
