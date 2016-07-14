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
package scouter.client.views;

import java.util.Date;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.popup.AlertNotifierDialog;
import scouter.client.threads.AlertProxyThread;
import scouter.client.threads.AlertProxyThread.IAlertListener;
import scouter.client.util.ChartUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.lang.AlertLevel;
import scouter.lang.pack.AlertPack;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class AlertView extends ViewPart implements IAlertListener {
	public static final String ID = AlertView.class.getName();

	AlertProxyThread proxyThread = AlertProxyThread.getInstance();

	private Table table = null;

	AlertNotifierDialog alertDialog;
	
	//Action alertFatalAct, alertWarnAct, alertErrorAct, alertInfoAct;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(ChartUtil.gridlayout(1));
		table = build(parent);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				TableItem[] item = table.getSelection();
				if (item == null || item.length == 0)
					return;
				Object o = item[0].getData();
				if (o == null || !(o instanceof AlertData)) {
					return;
				}
				AlertData d = (AlertData) o;
				String objName = TextProxy.object.getLoadText(DateUtil.yyyymmdd(d.p.time), d.p.objHash, d.serverId);
				alertDialog = new AlertNotifierDialog(getViewSite().getShell().getDisplay(), d.serverId);
				alertDialog.setObjName(objName);
				alertDialog.setPack(d.p);
				alertDialog.show(getViewSite().getShell().getBounds());
			}
		});

		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();

//		alertFatalAct = new Action("Fatal", IAction.AS_CHECK_BOX){ 
//	        public void run(){
//	        	PManager.getInstance().setValue(PreferenceConstants.NOTIFY_FATAL_ALERT, isChecked());
//	        }
//	    };  
//	    alertFatalAct.setImageDescriptor(Images.ALERT_FATAL);
//	    alertFatalAct.setChecked(PManager.getInstance().getBoolean(PreferenceConstants.NOTIFY_FATAL_ALERT));
//	    man.add(alertFatalAct);
//	    
//	    alertWarnAct = new Action("Warn", IAction.AS_CHECK_BOX){ 
//	    	public void run(){    
//	    		PManager.getInstance().setValue(PreferenceConstants.NOTIFY_WARN_ALERT, isChecked());
//	    	}
//	    };  
//	    alertWarnAct.setImageDescriptor(Images.ALERT_WARN);
//	    alertWarnAct.setChecked(PManager.getInstance().getBoolean(PreferenceConstants.NOTIFY_WARN_ALERT));
//	    man.add(alertWarnAct);
//	    
//	    alertErrorAct = new Action("Error", IAction.AS_CHECK_BOX){ 
//	    	public void run(){    
//	    		PManager.getInstance().setValue(PreferenceConstants.NOTIFY_ERROR_ALERT, isChecked());
//	    	}
//	    };  
//	    alertErrorAct.setImageDescriptor(Images.ALERT_ERROR);
//	    alertErrorAct.setChecked(PManager.getInstance().getBoolean(PreferenceConstants.NOTIFY_ERROR_ALERT));
//	    man.add(alertErrorAct);
//	    
//	    alertInfoAct = new Action("Info", IAction.AS_CHECK_BOX){ 
//	    	public void run(){    
//	    		PManager.getInstance().setValue(PreferenceConstants.NOTIFY_INFO_ALERT, isChecked());
//	    	}
//	    };  
//	    alertInfoAct.setImageDescriptor(Images.ALERT_INFO);
//	    alertInfoAct.setChecked(PManager.getInstance().getBoolean(PreferenceConstants.NOTIFY_INFO_ALERT));
//	    man.add(alertInfoAct);
//	    
//	    man.add(new Separator());
		
		man.add(new Action("clear", ImageUtil.getImageDescriptor(Images.minus)) {
			public void run() {
				reload();
			}
		});
		
		proxyThread.addAlertListener(this);
	}

	private void reload() {
		proxyThread.reset();
		table.removeAll();
	}

	private Table build(Composite parent) {
		final Table table = new Table(parent, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn[] cols = new TableColumn[5];
		cols[0] = UIUtil.create(table, SWT.LEFT, "TIME", cols.length, 0, false, 100);
		cols[1] = UIUtil.create(table, SWT.CENTER, "LEVEL", cols.length, 1, false, 50);
		cols[2] = UIUtil.create(table, SWT.LEFT, "TITLE", cols.length, 2, false, 200);
		cols[3] = UIUtil.create(table, SWT.LEFT, "MESSAGE", cols.length, 3, false, 200);
		cols[4] = UIUtil.create(table, SWT.LEFT, "OBJECT", cols.length, 5, false, 80);

		return table;
	}

	public void setFocus() {
	}

	public void dispose() {
		super.dispose();
		proxyThread.removeAlertListener(this);
	}

	static class AlertData {
		public int serverId;
		public AlertPack p;
		
		public AlertData(int serverId, AlertPack p) {
			this.serverId = serverId;
			this.p = p;
		}
	}
	
	public void ariseAlert(final int serverId, final AlertPack alert) {
		ExUtil.exec(table, new Runnable() {
			public void run() {
				while (table.getItemCount() > 500) {
					table.remove(table.getItemCount() - 1);
				}
				AlertData data = new AlertData(serverId, alert);
				TableItem t = new TableItem(table, SWT.NONE, 0);
				t.setText(new String[] { //
						FormatUtil.print(new Date(alert.time), "HH:mm:ss.SSS"),
						AlertLevel.getName(alert.level), //
						alert.title, //
						alert.message,//
						TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime()), data.p.objHash, serverId),//
				});
				t.setData(data);
			}
		});
	}
}