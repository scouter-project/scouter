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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.xlog.views.XLogViewPainter;
import scouter.util.CastUtil;

public class XLogYValueMaxDialog {
	
	private final Display display;
	private Shell dialog;
	Text maxValue;
	XLogViewPainter viewPainter;
	
	public XLogYValueMaxDialog(Display display, XLogViewPainter viewPainter) {
		this.display = display;
		this.viewPainter = viewPainter;
	}
	
	public void show() {
		dialog = setDialogLayout();
		dialog.pack();
		
		Rectangle rect = dialog.getBounds ();
		Point cursorLocation = Display.getCurrent().getCursorLocation();
	    dialog.setLocation (cursorLocation.x - (rect.width / 2), cursorLocation.y - (rect.height / 2));
	    
		dialog.open();
	}
	
	public void show(final String date) {
		dialog = setDialogLayout();
		dialog.pack();
		
		// POSITION SETTING - SCREEN CENTER
	    Monitor primaryMonitor = display.getPrimaryMonitor ();
	    Rectangle bounds = primaryMonitor.getBounds ();
	    Rectangle rect = dialog.getBounds ();
	    int x = bounds.x + (bounds.width - rect.width) / 2 ;
	    int y = bounds.y + (bounds.height - rect.height) / 2 ;
	    dialog.setLocation (x, y);
	    
		dialog.open();
	}

	public void close(){
		if(!dialog.isDisposed()){
			dialog.dispose();
			dialog = null;
		}
	}
	
	private Shell setDialogLayout() {
		
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText("Set Max Value");
		
		GridLayout gridLayout = new GridLayout(3, false);
	    gridLayout.verticalSpacing = 8;
	    dialog.setLayout(gridLayout);

	    Label label = new Label(dialog, SWT.NULL);
	    label.setText("Y axis max : ");

	    maxValue = new Text(dialog, SWT.SINGLE | SWT.BORDER);
	    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.widthHint = 100;
	    gridData.horizontalSpan = 2;
	    maxValue.setLayoutData(gridData);
		maxValue.setText(CastUtil.cString(viewPainter.getYValue()));
		maxValue.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == 27){
					close();
				}
			}
			public void keyReleased(KeyEvent e) {}
		});
		
		Label warn = new Label(dialog, SWT.NULL);
		warn.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		warn.setText("Max value > 0.05");
		gridData = new GridData();
	    gridData.horizontalSpan = 3;
	    gridData.horizontalAlignment = GridData.END;
	    warn.setLayoutData(gridData);
	    
	    Button setValueBtn = new Button(dialog, SWT.PUSH);
	    setValueBtn.setText("Set value");
	    gridData = new GridData();
	    gridData.horizontalSpan = 3;
	    gridData.horizontalAlignment = GridData.END;
	    setValueBtn.setLayoutData(gridData);
		setValueBtn.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					double newVal = CastUtil.cdouble(maxValue.getText());
					if(newVal < 0.05){
						newVal = 0.05;
					}
					viewPainter.setYValueMaxValue(newVal);
					close();
				break;
				}
			}
		});
		
		maxValue.selectAll();
		
		dialog.setDefaultButton(setValueBtn);
		
		return dialog;
	}

	
	
}
