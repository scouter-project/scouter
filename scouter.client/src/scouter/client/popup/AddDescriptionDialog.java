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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.util.UIUtil;

public class AddDescriptionDialog {
	
	private final Display display;
	private final File targetLocation;
	DescriptionSetter descriptionSetter;
	
	public static final String descriptionFileName = "profile.desc";
	
	private Shell dialog;
	
	Text descTxt;
	
	public AddDescriptionDialog(Display display, File targetLocation, DescriptionSetter descriptionSetter) {
		this.display = display;
		if(targetLocation.isDirectory()){
			this.targetLocation = targetLocation;
		}else{
			this.targetLocation = targetLocation.getParentFile();
		}
		this.descriptionSetter = descriptionSetter;
	}
	
	public void show(int serverId, Rectangle r) {
		dialog = setDialogLayout(serverId);
		dialog.pack();
		
		UIUtil.setDialogDefaultFunctions(dialog);
		
		getOldDescription();
		
		dialog.setSize(400, 150);
	    dialog.setLocation (r.x + 100, r.y + 100);
		dialog.open();
	}

	private void getOldDescription() {
		File file = new File(targetLocation.getPath() + "/" + descriptionFileName);
		if (!file.exists()) {
			return;
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String s;
			while ((s = in.readLine()) != null) {
				descTxt.setText(s);
			}
			in.close();
		} catch (IOException e) {
		}
	}

	public void close(){
		if(!dialog.isDisposed()){
			dialog.dispose();
			dialog = null;
		}
	}
	
	private Shell setDialogLayout(int serverId) {
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText("Add Description");
		
		dialog.setLayout(UIUtil.formLayout(5, 5));

	    Label iconLbl = new Label(dialog, SWT.NONE);
	    iconLbl.setImage(Images.COMMENT);
	    iconLbl.setLayoutData(UIUtil.formData(0, 10, 0, 10, null, -1, null, -1));

	    Label descLbl = new Label(dialog, SWT.WRAP);
	    descLbl.setText("Add description to "+targetLocation.getName());
	    descLbl.setLayoutData(UIUtil.formData(iconLbl, 10, 0, 10, 100, -10, null, -1));
	    
	    descTxt = new Text(dialog, SWT.BORDER);
	    descTxt.setLayoutData(UIUtil.formData(0, 30, descLbl, 10, 100, -30, null, -1));
	    
	    Button confirmBtn = new Button(dialog, SWT.PUSH);
	    confirmBtn.setLayoutData(UIUtil.formData(null, -1, descTxt, 10, 100, -30, null, -1, 150));
	    confirmBtn.setText("Confirm");
	    confirmBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				String description = descTxt.getText();
				if(description != null && !"".equals(description)){
					requestDescription(description);
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	    
		dialog.setDefaultButton(confirmBtn);
		
		return dialog;
	}
	
	private void requestDescription(String newDesc) {
		File file = new File(targetLocation.getPath() + "/"	+ descriptionFileName);
		if (file.exists()) {
			file.delete();
		}
		try {
			FileWriter fw = new FileWriter(targetLocation.getPath() + "/" + descriptionFileName);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(newDesc);
			bw.close();
			fw.close();
		} catch (IOException e) {
		}
		descriptionSetter.afterDescriptionCreated();
		dialog.close();
	}
	
	public interface DescriptionSetter{
		public void afterDescriptionCreated();
	}
	
}
