/*
 *  Copyright 2015 LG CNS.
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
package scouter.client.stack.base;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

@SuppressWarnings("serial")
public class ProgressBarWindow extends JFrame{
	JProgressBar m_progressBar = null;
	static public void startProgressWindow(final String title, final WindowObject object){
		new Thread(){
			public void run() {
				object.setWindowObject(new ProgressBarWindow(title));
		}			
		}.start();
	}
	
	public ProgressBarWindow(String title){
		super(title);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container content = this.getContentPane();
		m_progressBar = new JProgressBar();
		m_progressBar.setValue(0);
		m_progressBar.setStringPainted(true);
		Border border = BorderFactory.createTitledBorder("Reading...");
		m_progressBar.setBorder(border);
		content.add(m_progressBar, BorderLayout.NORTH);
		this.setSize(300, 100);
		this.setVisible(true);
	}
	
	public void setValue(int value){
		m_progressBar.setValue(value);
		if(value == 100){
			try { Thread.sleep(500); }catch(Exception ex){}
			this.dispose();
		}
	}
}
