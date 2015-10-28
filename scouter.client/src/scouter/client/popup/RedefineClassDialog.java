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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.util.ChartUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.UIUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;

public class RedefineClassDialog {
	
	private final Display display;
	private Shell dialog;
	Text addClassTxt;
	int objHash;
	
	public RedefineClassDialog(Display display) {
		this.display = display;
	}
	
	public void show(int objHash, final String value, int serverId, Rectangle r) {
		if(value == null || "".equals(value))
			return;
		
		this.objHash = objHash;
		
		dialog = setDialogLayout(serverId);
		dialog.pack();
		
	    dialog.setLocation (r.x + 100, r.y + 100);
	    
	    String[] classes = value.split(",");
	    
	    for(int inx = 0 ; inx < classes.length ; inx++){
	    	String classNm = classes[inx].trim();
	    	if(classNm != null && !"".equals(classNm))
	    		createTableRow(table, classNm);
	    }
		dialog.open();
	}

	public void close(){
		if(!dialog.isDisposed()){
			dialog.dispose();
			dialog = null;
		}
	}
	
	private int selected = -1;
	private Table table = null;
	
	private Shell setDialogLayout(int serverId) {
		final int servId = serverId;
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText("Redefine Classes");
		
		dialog.setLayout(UIUtil.formLayout(5, 5));

	    Label label = new Label(dialog, SWT.RIGHT);
	    label.setText("Class Name : ");
	    label.setLayoutData(UIUtil.formData(null, -1, 0, 2, null, -1, null, -1, 100));

	    addClassTxt = new Text(dialog, SWT.SINGLE | SWT.BORDER);
	    addClassTxt.setLayoutData(UIUtil.formData(label, 10, 0, 2, null, -1, null, -1, 310));
		addClassTxt.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == 27){
					close();
				}else if(e.keyCode == 13){
					String newClassNm = addClassTxt.getText();
					if(newClassNm != null && !"".equals(newClassNm)){
						createTableRow(table, newClassNm);
					}
					//addClassTxt.selectAll();
					addClassTxt.setText("");
				}
			}
			public void keyReleased(KeyEvent e) {}
		});
		
		Button addClassBtn = new Button(dialog, SWT.PUSH);
	    addClassBtn.setLayoutData(UIUtil.formData(addClassTxt, 10, null, -1, null, -1, null, -1, 70));
	    addClassBtn.setText("Add");
	    addClassBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					String newClassNm = addClassTxt.getText();
					if(newClassNm != null && !"".equals(newClassNm)){
						createTableRow(table, newClassNm);
					}
					addClassTxt.setText("");
				break;
				}
			}
		});
	    
	    Label listLabel = new Label(dialog, SWT.RIGHT);
	    listLabel.setText("Class Names : ");
	    listLabel.setLayoutData(UIUtil.formData(null, -1, label, 12, null, -1, null, -1, 100));
	    
	    table = new Table(dialog, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
	    table.setLayoutData(UIUtil.formData(listLabel, 10, label, 12, null, -1, null, -1, 300, 150));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayout(ChartUtil.gridlayout(1));

		TableColumn[] cols = new TableColumn[1];
		cols[0] = UIUtil.create(table, SWT.LEFT, "Class Name", cols.length, 0, false, 310);
	    
		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] selection = table.getSelection();
				if (selection.length < 0)
					return;

				selected = table.getSelectionIndex();
			}
		});
		
	    Button removeBtn = new Button(dialog, SWT.PUSH);
	    removeBtn.setLayoutData(UIUtil.formData(addClassTxt, 10, label, 12, null, -1, null, -1, 70));
	    removeBtn.setText("Remove");
	    removeBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if(selected != -1){
						table.remove(selected);
						selected = -1;
					}
				break;
				}
			}
		});
	    
	    Button redefineBtn = new Button(dialog, SWT.PUSH);
	    redefineBtn.setLayoutData(UIUtil.formData(null, -1, table, 10, 100, 0, null, -1, 200));
	    redefineBtn.setText("Redefine class");
	    redefineBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					TableItem[] items = table.getItems();
					if(items == null || items.length <= 0){
						return;
					}
					
					ArrayList<String> classNames = new ArrayList<String>();
					for(int i = 0 ; i < items.length ; i++){
						classNames.add(items[i].getText(0));
					}
					String newClassNm = addClassTxt.getText();
					if(newClassNm != null && !"".equals(newClassNm)){
						classNames.add(newClassNm);
					}
					requestRedefineClass(classNames, servId);
					close();
				break;
				}
			}
		});
	    
	    
		addClassTxt.selectAll();
		addClassTxt.setFocus();
		
		//dialog.setDefaultButton(addClassBtn);
		
		return dialog;
	}
	
	private void requestRedefineClass(ArrayList<String> classes, int serverId){
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objHash", this.objHash);
			
			ListValue classNames = param.newList("classNames");
			for(int inx = 0 ; inx < classes.size() ; inx++){
				classNames.add(classes.get(inx));
			}
			
			@SuppressWarnings("unused")
			MapPack out = (MapPack) tcp.getSingle(RequestCmd.REDEFINE_CLASSES, param);
		} catch(Exception e){
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}
	private void createTableRow(Table table, String clsNm){
		TableItem t = new TableItem(table, SWT.NONE, 0);
		t.setText(new String[] { clsNm });	
	}
		
	@SuppressWarnings("unused")
	private Table build(Composite parent) {
		final Table table = new Table(parent, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(false);
		table.setLinesVisible(true);

		TableColumn[] cols = new TableColumn[1];
		cols[0] = UIUtil.create(table, SWT.LEFT, "Class Name", cols.length, 0, false, 200);

		return table;
	}

}
