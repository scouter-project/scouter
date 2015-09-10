package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.counter.views.CounterRealTimeMultiView;
import scouter.client.util.ConsoleProxy;

public class OpenRealTimeMultiAction extends Action {

	IWorkbenchWindow window;
	int serverId;
	int objHash;
	String objType;
	String[] counters;
	String name;
	
	public OpenRealTimeMultiAction(IWorkbenchWindow window, String name, int serverId, int objHash, String objType, String[] counters) {
		this.window = window;
		this.serverId = serverId;
		this.objHash = objHash;
		this.objType = objType;
		this.counters = counters;
		this.name = name;
		setText(name);
	}

	public void run() {
		if (window != null) {
			try {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < counters.length; i++) {
					sb.append("&");
					sb.append(counters[i]);
				}
				window.getActivePage().showView(
						CounterRealTimeMultiView.ID, serverId + "&" + objHash + "&" + objType  + "&" + name + sb.toString(),
						IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				ConsoleProxy.errorSafe("Error opening view:" + e.getMessage());
			}
		}
	}
}
