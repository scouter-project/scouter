/*
 *  Copyright 2015 the original author or authors.
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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import scouter.client.Images;
import scouter.client.model.ICounterObjectSelector;
import scouter.client.model.TextProxy;
import scouter.client.util.TimeUtil;
import scouter.util.DateUtil;

public class XYGraphObjectSelectionDialog {
	
	private final Display display;
	private Shell dialog;
	private ICounterObjectSelector objSelector;
	private int serverId;
	
	ArrayList<Integer> objHashAll;
	ArrayList<Integer> newSelected;
	
	public XYGraphObjectSelectionDialog(Display display, ICounterObjectSelector objSelector, int serverId) {
		this.display = display;
		this.objSelector = objSelector;
		this.serverId = serverId;
	}
	
	public void show(ArrayList<Integer> objHashAll, ArrayList<Integer> objHashSelected) {
		this.objHashAll = objHashAll;
		newSelected = new ArrayList<Integer>(objHashSelected);
		
		dialog = setDialogLayout();
		dialog.pack();
		
		Rectangle rect = dialog.getBounds ();
		Point cursorLocation = Display.getCurrent().getCursorLocation();
	    dialog.setLocation (cursorLocation.x - (rect.width / 2), cursorLocation.y - (rect.height / 2));
	    
		dialog.open();
	}
	
	public void close(){
		if(!dialog.isDisposed()){
			dialog.dispose();
			dialog = null;
		}
	}
	
	ArrayList<Button> checkboxes;
	private Shell setDialogLayout() {
		
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText("Select Objects");
		
		GridLayout gridLayout = new GridLayout(3, false);
	    gridLayout.verticalSpacing = 8;
	    gridLayout.marginLeft = 10;
	    gridLayout.marginTop = 5;
	    dialog.setLayout(gridLayout);

	    checkboxes = new ArrayList<Button>(); 
	    
	    for(int inx = 0 ; inx < objHashAll.size() ; inx++){
	    	int objHash = objHashAll.get(inx);
	    	Button checkBox = new Button(dialog, SWT.CHECK);
	    	checkBox.setText(TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId));
	    	if(newSelected.indexOf(objHash) >= 0)
	    		checkBox.setSelection(true);
	    	GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    	data.widthHint = 300;
	    	data.horizontalSpan = 3;
	    	checkBox.setLayoutData(data);
	    	checkBox.setData(objHash);
	    	checkBox.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					int objHashSelected = (Integer) ((Button)e.widget).getData();
					if(((Button)e.widget).getSelection()){
						if(newSelected.indexOf(objHashSelected) < 0)
							newSelected.add(objHashSelected);
					}else{
						if(newSelected.indexOf(objHashSelected) >= 0)
							newSelected.remove(newSelected.indexOf(objHashSelected));
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
	    	checkboxes.add(checkBox);
	    }
	    
	    Button selectAllBtn = new Button(dialog, SWT.PUSH);
	    selectAllBtn.setText("Select All");
	    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalAlignment = GridData.CENTER;
	    gridData.widthHint = 100;
	    selectAllBtn.setLayoutData(gridData);
	    selectAllBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				for(Button b:checkboxes){
					b.setSelection(true);
				}
				newSelected = new ArrayList<Integer>(objHashAll);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Button deselectAllBtn = new Button(dialog, SWT.PUSH);
		deselectAllBtn.setText("Deselect All");
	    gridData = new GridData();
	    gridData.horizontalAlignment = GridData.CENTER;
	    gridData.widthHint = 100;
	    deselectAllBtn.setLayoutData(gridData);
	    deselectAllBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				for(Button b:checkboxes){
					b.setSelection(false);
				}
				newSelected = new ArrayList<Integer>();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	    
	    Button applyBtn = new Button(dialog, SWT.PUSH);
	    applyBtn.setText("Apply");
	    gridData = new GridData();
	    gridData.horizontalAlignment = GridData.CENTER;
	    gridData.widthHint = 100;
	    applyBtn.setLayoutData(gridData);
		applyBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				objSelector.setSelections(newSelected);
				close();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		dialog.setDefaultButton(applyBtn);
		
		return dialog;
	}

}
