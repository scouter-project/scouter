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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import scouter.client.util.UIUtil;

public class WhiteBoardView extends ViewPart {
	
	public final static String ID = WhiteBoardView.class.getName();
	
	private StyledText text;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(UIUtil.formLayout(5, 5));
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		text.setLayoutData(UIUtil.formData(0, 0, 0, 0, 100, 0, 100, 0));
		text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL) {
					if (e.keyCode == 'a' || e.keyCode == 'A') {
						text.selectAll();
					}
				}
			}
		});
	}
	
	public void setInput(String title, String content) {
		if (title != null) {
			this.setPartName(title);
		}
		text.setText(content);
	}
	
	public void setFocus() {
	}
}
