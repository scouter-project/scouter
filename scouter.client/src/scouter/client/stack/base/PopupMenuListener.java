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
