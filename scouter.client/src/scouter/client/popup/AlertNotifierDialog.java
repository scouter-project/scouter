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
package scouter.client.popup;

import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.model.TextModel;
import scouter.client.model.TextProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.util.ExUtil;
import scouter.client.util.UIUtil;
import scouter.lang.AlertLevel;
import scouter.lang.pack.AlertPack;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class AlertNotifierDialog {
	
	private final Display display;
	private Shell dialog;
	private String objName;
	private int serverId;
	
	Label timeLbl, levelLbl, titleLbl, messageLbl, objectLbl;
	AlertPack p;
	Timer timer;
	
	public AlertNotifierDialog(Display display, int serverId) {
		this.display = display;
		this.serverId = serverId;
	}
	
	public void setObjName(String objName) {
		this.objName = objName;
	}
	
	public void setPack(AlertPack p){
		this.p = p;
	}
	
	public void show(Rectangle r) {
		
		dialog = setDialogLayout();
		dialog.pack();
		
		UIUtil.setDialogDefaultFunctions(dialog);
		
		if (p.tags.size() > 0) {
			dialog.setSize(600, 400);
		} else {
			dialog.setSize(600, 300);
		}
		
		int timeout = PManager.getInstance().getInt(PreferenceConstants.P_ALERT_DIALOG_TIMEOUT);
		if (timeout > 0) {
			timer = new Timer(true);
			timer.schedule(new TimerTask() {
				public void run() {
					ExUtil.exec(dialog, new Runnable() {
						public void run() {
							close();
						}
					});
				}
			}, timeout * 1000);
		}
	    
		Monitor primaryMonitor = display.getPrimaryMonitor ();
	    Rectangle bounds = primaryMonitor.getBounds ();
	    Rectangle rect = dialog.getBounds ();
	    int x = bounds.x + (bounds.width - rect.width) / 2 ;
	    int y = bounds.y + (bounds.height - rect.height) / 2 ;
	    dialog.setLocation (x, y);
		dialog.open();
	}

	public boolean isOpen(){
		return dialog != null && !dialog.isDisposed();
	}
	
	public void close(){
		if(!dialog.isDisposed()){
			dialog.dispose();
			dialog = null;
		}
	}
	
	private Shell setDialogLayout() {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText("Alert");
		dialog.setLayout(UIUtil.formLayout(5, 5));

	    Label iconLbl = new Label(dialog, SWT.NONE);
	    iconLbl.setImage(Images.ALERT_BIG);
	    iconLbl.setLayoutData(UIUtil.formData(0, 10, 0, 10, null, -1, null, -1));

	    Composite mainComp = new Composite(dialog, SWT.NONE);
	    mainComp.setLayoutData(UIUtil.formData(iconLbl, 10, 0, 5, 100, -5, 100, -30));
	    mainComp.setLayout(UIUtil.formLayout(0, 0));
	    
	    timeLbl = new Label(mainComp, SWT.RIGHT);
	    timeLbl.setText(FormatUtil.print(new Date(p.time), "HH:mm:ss.SSS")); 
	    timeLbl.setLayoutData(UIUtil.formData(0, 5, 0, 0, 100, -5, null, -1));
	    FontData[] fD = timeLbl.getFont().getFontData();
	    fD[0].setStyle(SWT.BOLD);
	    fD[0].setHeight(20);
	    timeLbl.setFont( new Font(display,fD[0]));
	    
	    levelLbl = new Label(mainComp, SWT.RIGHT);
	    levelLbl.setText(AlertLevel.getName(p.level));
	    levelLbl.setLayoutData(UIUtil.formData(0, 5, timeLbl, 5, 100, -5, null, -1));
	    fD[0].setHeight(18);
	    levelLbl.setFont( new Font(display,fD[0]));
	    
	    Label bar = new Label(mainComp, SWT.NONE);
	    bar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
	    bar.setLayoutData(UIUtil.formData(0, 5, levelLbl, 4, 100, -5, null, -1, -1, 1));
	    
	    titleLbl = new Label(mainComp, SWT.RIGHT);
	    if(p.level == AlertLevel.ERROR){
			titleLbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		}else if(p.level == AlertLevel.WARN){
			titleLbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		}else if(p.level == AlertLevel.FATAL){
			titleLbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}else if(p.level == AlertLevel.INFO){
			titleLbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		}
		titleLbl.setText(p.title);
	    titleLbl.setLayoutData(UIUtil.formData(0, 5, levelLbl, 5, 100, -5, null, -1));
	    fD[0].setHeight(20);
	    fD[0].setStyle(SWT.BOLD);
	    titleLbl.setFont( new Font(display,fD[0]));
	    
	    
	    messageLbl = new Label(mainComp, SWT.WRAP | SWT.RIGHT);
	    messageLbl.setText(p.message);
	    messageLbl.setLayoutData(UIUtil.formData(0, 5, titleLbl, 5, 100, -5, null, -1));
	    fD[0].setHeight(11);
	    messageLbl.setFont( new Font(display,fD[0]));
	    
	    objectLbl = new Label(mainComp, SWT.WRAP | SWT.RIGHT);
	    objectLbl.setText(objName == null ? "" : objName);
	    objectLbl.setLayoutData(UIUtil.formData(0, 5, messageLbl, 5, 100, -5, null, -1));
	    fD[0].setHeight(11);
	    objectLbl.setFont( new Font(display,fD[0]));
	    objectLbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
	    
	    if(p.tags.size() > 0) {
	    	Text tagLabel = new Text(mainComp, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
	    	tagLabel.setLayoutData(UIUtil.formData(0, 5, objectLbl, 5, 100, -5, 100, 0));
	    	StringBuilder sb = new StringBuilder();
	    	Set<String> keySet = p.tags.keySet();
	    	for (String key : keySet) {
	    		int hashIndex = key.indexOf(AlertPack.HASH_FLAG);
	    		if (hashIndex > -1) {
	    			int titleIndex = key.lastIndexOf("_");
	    			String title = key.substring(titleIndex + 1);
	    			String textType = null;
	    			if (titleIndex > hashIndex) {
	    				textType = key.substring(AlertPack.HASH_FLAG.length(), titleIndex);
	    			} else {
	    				textType = key.substring(AlertPack.HASH_FLAG.length());
	    			}
	    			sb.append(title + " : ");
	    			TextModel model = TextProxy.getTextModel(textType);
	    			if (model != null) {
	    				sb.append(model.getLoadText(DateUtil.yyyymmdd(p.time), p.tags.getInt(key), serverId));
	    			}
	    			sb.append("\n");
	    		} else {
	    			sb.append(key + " : " + p.tags.get(key) + "\n");
	    		}
	    	}
	    	tagLabel.setText(sb.toString());
	    }
	    
	    Button closeBtn = new Button(dialog, SWT.PUSH);
	    closeBtn.setLayoutData(UIUtil.formData(null, -1, mainComp, 5, 100, -5, null, -1, 150));
	    closeBtn.setText("Close");
	    closeBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				close();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		dialog.setDefaultButton(closeBtn);
		
		// HANDLE 'X' BUTTON FOR CLOSE
		dialog.addListener(SWT.Close, new Listener() {
	        public void handleEvent(Event event) {
	        	close();
	        }
	    });
		
		return dialog;
	}
}
