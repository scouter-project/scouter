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
package scouter.client.stack.base;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import scouter.client.stack.utils.ResourceUtils;
import scouter.client.util.UIUtil;

public class ProgressBarWindow{
	private Shell m_shell = null;
	private ProgressBar m_progressBar = null;
	
	public ProgressBarWindow(Shell shell, String title){
        m_shell = new Shell(shell, SWT.TITLE);
        m_shell.setText(title);
        m_shell.setLayout(new FillLayout());
        int [] pos = UIUtil.getScreenSize();
        m_shell.setBounds((pos[0]/2)-75, (pos[1]/2)-15, 150, 40);
        
        m_progressBar = new ProgressBar(m_shell, SWT.HORIZONTAL);
		m_progressBar.setMinimum(0);
		m_progressBar.setMaximum(100);
		m_progressBar.setBounds(0, 0, 140, 20);

		m_progressBar.setState(SWT.ERROR);
        m_shell.open();
	}
	
	public void setValue(int value){
		m_progressBar.setSelection(value);
	}
	
	public void close(){
		m_shell.close();
	}	
}
