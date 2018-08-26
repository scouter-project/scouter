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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import scouter.client.model.AgentModelThread;
import scouter.lang.conf.ValueType;
import scouter.lang.conf.ValueTypeDesc;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ConfigureItemDialog extends TitleAreaDialog {
	String confKey;
	String valueOrg;
	String objName;
	String desc;
	ValueType valueType;
	ValueTypeDesc valueTypeDesc;
	boolean isServer;
	int objHash;
	String objType;

	String value;
	List<Text> texts = new ArrayList<>();

	ConfApplyScopeEnum applyScope = ConfApplyScopeEnum.THIS;
	Widget lastFocusedText;

	public ConfigureItemDialog(Shell parentShell, String confKey, String valueOrg, String objName, String desc,
							   ValueType valueType, ValueTypeDesc valueTypeDesc, boolean isServer, int objHash) {
		super(parentShell);
		this.confKey = confKey;
		this.objName = objName;
		this.desc = desc;
		this.valueType = (valueType == null) ? ValueType.VALUE : valueType;
		this.valueTypeDesc = (valueTypeDesc == null) ? new ValueTypeDesc() : valueTypeDesc;
		this.valueOrg = valueOrg;
		if(this.valueOrg == null) {
			this.valueOrg = "";
		}
		this.value = shape(valueOrg);

		this.isServer = isServer;
		this.objHash = objHash;
		if (!isServer) {
			this.objType = AgentModelThread.getInstance().getAgentObject(objHash).getObjType();
		}
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

		ScrolledComposite scrolled = new ScrolledComposite(container, SWT.V_SCROLL | SWT.H_SCROLL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		scrolled.setLayoutData(data);

		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		Group group = new Group(scrolled, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scrolled.setContent(group);
		scrolled.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if (valueType == ValueType.VALUE) {
			group.setLayout(new GridLayout(1, false));
			Text serviceTxt = new Text(group, SWT.BORDER | SWT.SINGLE);
			serviceTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			serviceTxt.setText(value);
			serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());

		} else if (valueType == ValueType.COMMA_COLON_SEPARATED_VALUE) {
			data.heightHint = 250;

			FocusListener focusListener = new FocusListener() {
				@Override
				public void focusGained(FocusEvent focusEvent) {
					lastFocusedText = focusEvent.widget;
				}

				@Override
				public void focusLost(FocusEvent focusEvent) {
				}
			};

			String[] strings = valueTypeDesc.getStrings();
			boolean[] booleans = valueTypeDesc.getBooleans();
			if(strings == null || strings.length == 0) {
				group.setLayout(new GridLayout(1, false));
				Text serviceTxt = new Text(group, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.heightHint = 150;
				serviceTxt.setLayoutData(gridData);
				serviceTxt.setText(value);
				serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());

			} else {
				group.setLayout(new GridLayout(strings.length, false));

				Composite buttonArea = new Composite(group, SWT.NONE);
				buttonArea.setLayout(new RowLayout());
				buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, strings.length, 1));

				Button buttonAdd = new Button(buttonArea, SWT.PUSH);
				buttonAdd.setText("+");

				Button buttonRemove = new Button(buttonArea, SWT.PUSH);
				buttonRemove.setText("-");

				buttonAdd.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						for(int i = 0; i < strings.length; i++) {
							Text text = new Text(group, SWT.CENTER);
							text.setText("");
							text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
							text.addFocusListener(focusListener);

							texts.add(text);
						}
						group.layout(true, true);
						group.setSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
						scrolled.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					}
				});

				buttonRemove.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (lastFocusedText != null) {
							int selectedIndex = texts.indexOf(lastFocusedText);
							if(selectedIndex < 0) return;
							int firstTextIndex = selectedIndex / strings.length * strings.length;
							for (int i = firstTextIndex; i < firstTextIndex + strings.length; i++) {
								texts.get(i).dispose();
								group.layout(true, true);
								group.setSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
								scrolled.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
							}

							boolean focused = false;
							for (int i = firstTextIndex + strings.length; i < texts.size(); i = i + strings.length) {
								if (!texts.get(i).isDisposed()) {
									texts.get(i).setFocus();
									focused = true;
									break;
								}
							}
							if (!focused) {
								for (int i = firstTextIndex - strings.length; i >= 0; i = i - strings.length) {
									if (!texts.get(i).isDisposed()) {
										texts.get(i).setFocus();
										break;
									}
								}
							}
						}
					}
				});

				for (int i = 0; i < strings.length; i++) {
					String colName = strings[i];
					Label col = new Label(group, SWT.CENTER);
					col.setText(colName);
					col.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					if (booleans != null && booleans.length > i) {
						if (booleans[i]) {
							FontData fontData = col.getFont().getFontData()[0];
							Font font = new Font(null, new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
							col.setFont(font);
						}
					}
				}

				String[] lines = StringUtil.split(valueOrg, ',');
				for (String line : lines) {
					String[] values = StringUtil.split(line, ':');
					if (values.length < 0) {
						continue;
					}
					for(int i = 0; i < strings.length; i++) {
						Text text = new Text(group, SWT.CENTER);
						if (values.length > i && StringUtil.isNotEmpty(values[i])) {
							text.setText(values[i]);
						}
						text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						text.addFocusListener(focusListener);

						texts.add(text);
					}
				}

				for(int i = 0; i < strings.length; i++) {
					Text text = new Text(group, SWT.CENTER);
					text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					text.addFocusListener(focusListener);

					texts.add(text);
				}
			}

		} else if (valueType == ValueType.COMMA_SEPARATED_VALUE) {
			group.setLayout(new GridLayout(1, false));
			Text serviceTxt = new Text(group, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			gridData.heightHint = 100;
			serviceTxt.setLayoutData(gridData);
			serviceTxt.setText(value);
			serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());

		} else if (valueType == ValueType.NUM) {
			group.setLayout(new GridLayout(1, false));
			Text serviceTxt = new Text(group, SWT.BORDER | SWT.SINGLE);
			serviceTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			serviceTxt.addVerifyListener(e -> e.doit = isNumber(e.text));
			serviceTxt.setText(value);
			serviceTxt.addModifyListener(e -> value = ((Text) e.getSource()).getText());

		} else if (valueType == ValueType.BOOL) {
			group.setLayout(new GridLayout(1, false));
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
			group.setLayout(new GridLayout(1, false));
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
			rdForType.setText("to all same type objects(" + objType + ") in this collector.(the configuration will be saved automatically)");

			Button rdForTypeAll = new Button(applyTypeGroup, SWT.RADIO);
			rdForTypeAll.setText("to all same type objects(" + objType + ") for all collectors.(the configuration will be saved automatically)");

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
		if (!checkData()) return;

		super.okPressed();
	}

	private boolean checkData() {
		if (valueType == ValueType.COMMA_COLON_SEPARATED_VALUE) {
			String[] strings = valueTypeDesc.getStrings();
			boolean[] booleans = valueTypeDesc.getBooleans();

			if (strings != null || strings.length > 0) {
				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < texts.size(); i = i + strings.length) {
					if (texts.get(i).isDisposed()) {
						continue;
					}

					boolean allBlank = true;
					for (int j = i; j < i + strings.length; j++) {
						if (StringUtil.isNotEmpty(texts.get(j).getText())) {
							allBlank = false;
							break;
						}
					}

					if (allBlank) {
						for (int j = i; j < i + strings.length; j++) {
							texts.get(j).dispose();
						}
					}
				}

				for (int i = 0; i < texts.size(); i++) {
					if (texts.get(i).isDisposed()) {
						continue;
					}
					int itemGroupPos = i % strings.length;
					if (booleans != null && booleans.length > itemGroupPos && booleans[itemGroupPos]) {
						if (StringUtil.isEmpty(texts.get(i).getText())) {
							MessageDialog.openWarning(this.getShell(), "Missing required", "Missing required.");
							texts.get(i).setFocus();
							return false;
						}
					}
				}

				for (int i = 0; i < texts.size(); i++) {
					if (texts.get(i).isDisposed()) {
						continue;
					}

					if (i != 0 && i % strings.length == 0) {
						sb.append(',');
					} else if (i != 0) {
						sb.append(':');
					}

					sb.append(texts.get(i).getText());
				}
				value = sb.toString();
			}
		}
		return true;
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
