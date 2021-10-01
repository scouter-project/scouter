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
package scouter.client.cubrid.actions;

import java.awt.Point;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.cubrid.CubridSingleItem;
import scouter.client.model.AgentModelThread;
import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.UIUtil;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class AlertSettingDialog {
	private final Display display;
	private final int serverId;
	private int objhash;
	private final int counterOrdinal;
	
	Combo dbListCombo;
	Combo counterCombo;
	Text currentValue;
	Text changeValue;
	
	String value = "0";
	
	public AlertSettingDialog(Display display,int serverId,int ordinal) {
		this.display = display;
		this.serverId = serverId;
		this.counterOrdinal = ordinal;
	}

	public void show(Point p) {
		if (p != null)
			show((int) p.getX(), (int) p.getY() + 10);
	}

	public void show() {
		show(UIUtil.getMousePosition());
	}

	public void show(int x, int y) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setLayout(new GridLayout(2, true));
		dialog.setText("Warning Alert Setting");
		UIUtil.setDialogDefaultFunctions(dialog);

		AgentModelThread agentThreadA = AgentModelThread.getInstance();
		String hashString = agentThreadA.getLiveObjectHashStringWithParent(serverId, CounterConstants.CUBRID_AGENT);
		
		if (hashString == null || hashString.equals("")) { 
			MessageDialog.openError(dialog, "Error55", "Error : " + "Please Check CUBRID-AGENT!!!!");
			return;
		} else {
			objhash = Integer.parseInt(hashString);
		}
		
		loadAlertConfig();
		
		Label Label1 = new Label(dialog, SWT.NONE);
		Label1.setLayoutData(new GridData(SWT.LEFT));
		Label1.setText("Counter Name:");
		counterCombo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		ArrayList<String> multiViewList = new ArrayList<String>();
		for (CubridSingleItem view : CubridSingleItem.values()) {
			multiViewList.add(view.getTitle());
		}
		counterCombo.setItems(multiViewList.toArray(new String[CubridSingleItem.values().length]));
		counterCombo.select(counterOrdinal);
		counterCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		counterCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				value = alertConfigMap.getText(
						CubridSingleItem.values()[counterCombo.getSelectionIndex()].getCounterName());
				
				if (value == null) {
					currentValue.setText("not load Or not Setting");
				} else {
					currentValue.setText(value);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		
		Label Label2 = new Label(dialog, SWT.NONE);
		Label2.setLayoutData(new GridData(SWT.LEFT));
		Label2.setText("Current Warning Value:");
		currentValue = new Text(dialog, SWT.NONE);
		currentValue.setEditable(false);
		currentValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		value = alertConfigMap.getText(CubridSingleItem.values()[counterOrdinal].getCounterName());
		
		if (value == null) {
			currentValue.setText("not load Or not Setting");
		} else {
			currentValue.setText(value);
		}
		
		Label Label3 = new Label(dialog, SWT.NONE);
		Label3.setLayoutData(new GridData(SWT.LEFT));
		Label3.setText("Change Warning Value:");
		changeValue = new Text(dialog, SWT.NONE);
		changeValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		changeValue.setText("");
		
		Button okButton = new Button(dialog, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					try {
						int ret = saveAlertConfig(
								CubridSingleItem.values()[counterCombo.getSelectionIndex()].getCounterName(), changeValue.getText());
						
						if (ret != ErrorEnum.SUCCESS.getCode()) {
							MessageDialog.openError(dialog, "Error55", "Error : " + ErrorEnum.getErrorMsg(ret));
						} else {
							dialog.close();
						}

					} catch (Exception e) {
						MessageDialog.openError(dialog, "Error55", "Error : " + e.getMessage());
					}
					break;
				}
			}
		});

		Button cancelButton = new Button(dialog, SWT.PUSH);
		cancelButton.setText("&Cancel");
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		cancelButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					dialog.close();
					break;
				}
			}
		});

		dialog.setDefaultButton(okButton);
		dialog.pack();

		if (x > 0 && y > 0) {
			dialog.setLocation(x, y);
		}

		dialog.open();
	}

	MapValue alertConfigMap = new MapValue();

    private void loadAlertConfig() {
    	MapPack param = new MapPack();
        param.put("objHash", this.objhash);
        ExUtil.asyncRun(new Runnable() {
            public void run() {
                MapPack pack = null;
                TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
                try {
                    pack = (MapPack) tcp.getSingle(RequestCmd.CUBRID_GET_ALERT_CONFIGURE, param);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    TcpProxy.putTcpProxy(tcp);
                }
                if (pack != null) {
                    final ListValue keyList = pack.getList("key");
                    final ListValue valueList = pack.getList("value");
                    alertConfigMap.clear();
                    for (int i = 0; i < keyList.size(); i++) {
                        String key = CastUtil.cString(keyList.get(i));
                        String value = CastUtil.cString(valueList.get(i));
                        key = key == null ? "" : key;
                        value = value == null ? "" : value;
                        alertConfigMap.put(key, value);
                    }
                }
            }
        });
    }
    
    private int saveAlertConfig(String alertKey, String value) {
    	int success = ErrorEnum.SUCCESS.getCode();
    	
    	if (value == "") {
    		return ErrorEnum.VALUE_EMPTY.getCode();
    	} else if (Integer.parseInt(value) < 0) {
    		return ErrorEnum.VALUE_ERROR.getCode();
    	}
    	
        TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
        try {
            MapPack param = new MapPack();
            param.put("key", alertKey);
            param.put("value", value);
            param.put("objHash", this.objhash);

            MapPack out = null;
            out = (MapPack) tcp.getSingle(RequestCmd.CUBRID_SET_ALERT_CONFIGURE, param);
            
            if (out != null) {
                String config = out.getText("result");
                if ("true".equalsIgnoreCase(config)) {
                    success = ErrorEnum.SUCCESS.getCode();
                } else {
                    success = ErrorEnum.SERVER_ERROR.getCode();
                }
            }
            
        } catch (Throwable throwable) {
            success = ErrorEnum.THROW_ERROR.getCode();
            ConsoleProxy.errorSafe(throwable.getMessage() + " : error on save AlertConfig.");
        } finally {
            TcpProxy.putTcpProxy(tcp);
        }
        return success;
    }
    
    enum ErrorEnum {
		SUCCESS(0,"SUCCESS"),
		VALUE_EMPTY(1,"Please check, ChangeValue is Empty"),
		VALUE_ERROR(2,"Please check, ChangeValue is less zero"),
		SERVER_ERROR(3,"Error Server"),
    	THROW_ERROR(4,"Unkown Error");

		private final int errorCode;
	    private final String errorMsg;

		private ErrorEnum(int errorCode, String errorMsg) {
			this.errorCode = errorCode;
			this.errorMsg = errorMsg;
		}

		public int getCode() {
			return errorCode;
		}

		public String getMsg() {
			return errorMsg;
		}
		
		public static String getErrorMsg(int code) {
			return ErrorEnum.values()[code].getMsg();
		}
    }
}

