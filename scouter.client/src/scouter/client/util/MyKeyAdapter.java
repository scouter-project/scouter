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
package scouter.client.util;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class MyKeyAdapter extends KeyAdapter {
	private String fileName = "";

	private Shell superShell = null;

	public MyKeyAdapter(Shell shell, String fname) {
		fileName = fname;
		superShell = shell;
	}

	public void save(String file, String value) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
		bw.write(value);
		bw.close();
	}
	
	public void setFileName(String fileName){
		this.fileName = fileName;
	}

	public void keyPressed(KeyEvent ev) {
		if (ev.stateMask == SWT.CTRL) {
			if (ev.keyCode == 97) {
				if(ev.widget instanceof Text){
					((Text) ev.widget).selectAll();
				}else if(ev.widget instanceof StyledText){
					((StyledText) ev.widget).selectAll();
				}
			} else if (ev.keyCode == 115) {
				FileDialog dialog = new FileDialog(superShell, SWT.SAVE);
				dialog.setFileName(fileName);
				String file = dialog.open();
				if (file == null)
					return;
				try {
					if(ev.widget instanceof Text){
						save(file, ((Text) ev.widget).getText());
					}else if(ev.widget instanceof StyledText){
						save(file, ((StyledText) ev.widget).getText());
					}
				} catch (Exception ex) {
					ConsoleProxy.errorSafe(ex.toString());
				}
			}
		}
		super.keyPressed(ev);
	}

}