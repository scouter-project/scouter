package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.counter.views.CounterRTAllPairChart;
import scouter.client.util.ConsoleProxy;

public class OpenRTPairAllAction extends Action {

	IWorkbenchWindow window;
	int serverId;
	String objType;
	String counter;
	
	public OpenRTPairAllAction(IWorkbenchWindow window, String name, int serverId, String objType, String counter) {
		this.window = window;
		this.serverId = serverId;
		this.objType = objType;
		this.counter = counter;
		setImageDescriptor(Images.getCounterImageDescriptor(objType, counter, serverId));
		setText(name);
	}

	public void run() {
		if (window != null) {
			try {
				window.getActivePage().showView(
						CounterRTAllPairChart.ID, serverId + "&" + objType + "&" + counter ,
						IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				ConsoleProxy.errorSafe("Error opening view:" + e.getMessage());
			}
		}
	}
}
