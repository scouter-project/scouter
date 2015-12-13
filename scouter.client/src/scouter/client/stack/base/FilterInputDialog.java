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
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.stack.utils.ResourceUtils;
import scouter.client.util.UIUtil;

public class FilterInputDialog {
    public enum TASK { NONE, PERFORMANCE_TREE, SERVICE_CALL, THREAD_STACK, FILTER_ANALYZER };

    private boolean m_isAscending = true;
    private Text m_field = null;
    private TASK m_jobType = TASK.NONE;
    private Shell m_shell = null;

    public FilterInputDialog(Shell shell, boolean isAscending, scouter.client.stack.base.FilterInputDialog.TASK jobtype) {
        m_shell = new Shell(shell);
        m_shell.setText("Input Filter String");
        m_shell.setImage(Images.filter);
        m_shell.setSize(700, 120);
        
        FormLayout layout = new FormLayout();
        layout.marginHeight = 5;
        layout.marginWidth = 5;
    	m_shell.setLayoutData(layout);
    	
        int [] screen = UIUtil.getScreenSize();
        m_shell.setLocation((screen[0]/2)-350, (screen[1]/2)-75);        
        
        m_field = new Text(m_shell, SWT.BORDER | SWT.LEFT);
        m_field.setEditable(true);
        m_field.setTextLimit(100);
        m_field.setSize(682, 25);
        m_field.setLocation(5, 5);

        Button button = new Button(m_shell, SWT.NONE);
        button.setText("Execute");
        button.setAlignment(SWT.CENTER);
        button.setSize(100, 35);
        button.setLocation(480, 40);
        button.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
                String filter = m_field.getText();
                if ( filter != null && filter.length() > 0 ) {
	                MainProcessor mainProcessor = MainProcessor.instance();
	                switch(m_jobType){
	                	case PERFORMANCE_TREE:
	                		mainProcessor.createAnalyzedPerformance(filter, m_isAscending);
	                		break;
	                	case SERVICE_CALL:
	                		mainProcessor.viewServiceCall(filter);
	                		break;
	                	case THREAD_STACK:
	                		mainProcessor.viewThreadStack(filter);
	                		break;
	                	case FILTER_ANALYZER:
	                		mainProcessor.analyzeFilterStack(filter, m_isAscending);
	                		break;
	                }
                }
                m_shell.close();
			}
        });
        
        m_shell.setDefaultButton(button);
        
        button = new Button(m_shell, SWT.NONE);
        button.setText("Cancel");
        button.setAlignment(SWT.CENTER);
        button.setSize(100, 35);
        button.setLocation(588, 40);
        button.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				if(event.type == SWT.Selection){
					m_shell.close();
				}
			}
        });
        
        m_isAscending = isAscending;
        m_jobType = jobtype;

        m_field.setFocus();
        m_shell.open();      
    }    
}
