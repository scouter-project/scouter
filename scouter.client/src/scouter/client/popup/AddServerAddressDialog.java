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

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;

import scouter.client.util.UIUtil;
import scouter.net.NetConstants;
import scouter.util.StringUtil;

public class AddServerAddressDialog {
	
	private final Display display;
	ServerAddressAddition callback;
	Set<String> addr;
	
	private Shell dialog;
	
	Text newAddressTxt;
	
	public AddServerAddressDialog(Display display, ServerAddressAddition serverAddressAddition, Set<String> addr) {
		this.display = display;
		this.callback = serverAddressAddition;
		this.addr = addr;
	}
	
	public void show(Rectangle r) {
		dialog = setDialogLayout();
		dialog.pack();
		
		UIUtil.setDialogDefaultFunctions(dialog);
		
		Point cursorLocation = Display.getCurrent().getCursorLocation();
	    dialog.setLocation (cursorLocation.x, cursorLocation.y);
		dialog.open();
	}

	public void close(){
		if(!dialog.isDisposed()){
			dialog.dispose();
			dialog = null;
		}
	}
	
	private Shell setDialogLayout() {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText("Add");
		
		dialog.setLayout(UIUtil.formLayout(5, 5));

	    Label newAddressLbl = new Label(dialog, SWT.NONE);
	    newAddressLbl.setText("Server address (IP:Port)");
	    newAddressLbl.setLayoutData(UIUtil.formData(0, 5, 0, 5, null, -1, null, -1));
	    
	    final ToolTip formatTip = new ToolTip(dialog, SWT.BALLOON);
	    formatTip.setMessage("IP:Port ex) 127.0.0.1:" + NetConstants.SERVER_TCP_PORT);
	    
	    final ToolTip existTip = new ToolTip(dialog, SWT.BALLOON);
        existTip.setMessage("Server Address is already exist!");

	    newAddressTxt = new Text(dialog, SWT.BORDER);
	    newAddressTxt.setLayoutData(UIUtil.formData(0, 5, newAddressLbl, 10, 100, -5, null, -1, 250));
	    
	    final Button confirmBtn = new Button(dialog, SWT.PUSH);
	    confirmBtn.setLayoutData(UIUtil.formData(null, -1, newAddressTxt, 10, 100, -5, null, -1, 100));
	    confirmBtn.setText("Ok");
	    confirmBtn.setEnabled(false);
	    confirmBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				String newAddr = newAddressTxt.getText();
				newAddr.replaceAll("\\s+","");
				if(StringUtil.isNotEmpty(newAddr)){
					if (isInvalidFormat(newAddr)) {
						newAddressTxt.setFocus();
						newAddressTxt.selectAll();
		                Point loc = newAddressTxt.toDisplay(newAddressTxt.getLocation());
		                formatTip.setLocation(loc.x, loc.y - newAddressTxt.getSize().y);
		                formatTip.setVisible(true);
						return;
					} else if(addr.contains(newAddr)){
						newAddressTxt.setFocus();
						newAddressTxt.selectAll();
		                Point loc = newAddressTxt.toDisplay(newAddressTxt.getLocation());
		                existTip.setLocation(loc.x, loc.y - newAddressTxt.getSize().y);
		                existTip.setVisible(true);
						return;
					}
					callback.addServerAddress(newAddr);
					dialog.close();
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	    
	    newAddressTxt.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (newAddressTxt.getText().length() > 0) {
					confirmBtn.setEnabled(true);
				} else {
					confirmBtn.setEnabled(false);
				}
			}
		});
	    
		dialog.setDefaultButton(confirmBtn);
		
		return dialog;
	}
	
	private boolean isInvalidFormat(String text) {
		if (StringUtil.isEmpty(text)) {
			return true;
		}
		String[] strs = text.split(":");
		if (strs == null || strs.length != 2) {
			return true;
		}
		return false;
	}
	
	public interface ServerAddressAddition{
		public void addServerAddress(String newAddress);
	}
	
}
