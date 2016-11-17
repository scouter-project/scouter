/*
 *  Copyright 2016 the original author or authors. 
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
 */
package scouter.client.stack.base;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;

public class PopupMenuListener implements Listener {

	public void handleEvent(Event event) {
		try {
			MenuItem item = (MenuItem)event.widget;
			MainProcessor mainProcessor = MainProcessor.instance();
			String menuText = item.getText();
			if((item.getStyle() & SWT.CHECK) > 0){
				if("Exclude Stack".endsWith(menuText)){
					mainProcessor.setExcludeStack(item.getSelection());
				}else if("Remove Line(Performance Tree)".endsWith(menuText)){
					mainProcessor.setRemoveLine(item.getSelection());
				}else if("Inner Percent(Performance Tree)".endsWith(menuText)){
					mainProcessor.setInerPercent(item.getSelection());
				}else if("Sort by Function".endsWith(menuText)){
					mainProcessor.setSortByFunction(item.getSelection());
				}else if("Simple Dump Time List".endsWith(menuText)){
					mainProcessor.setSimpleDumpTimeList(item.getSelection());
				}else if("Use Default Parser Configuration".endsWith(menuText)){
					mainProcessor.setDefaultConfiguration(item.getSelection());
				}else if("Analyze All Threads In Stack(No Filter)".endsWith(menuText)){
					mainProcessor.setAnalyzeAllThread(item.getSelection());
				}
			}else{
				mainProcessor.processMenu(menuText);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}
}
