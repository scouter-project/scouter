package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.counter.views.CounterRTAllPairChart2;
import scouter.client.util.ConsoleProxy;

public class OpenRTPairAllAction2 extends Action {

	IWorkbenchWindow window;
	int serverId;
	String objType;
	String counter1;
	String counter2;
	
	public OpenRTPairAllAction2(IWorkbenchWindow window, String name, int serverId, String objType, String counter1, String counter2) {
		this.window = window;
		this.serverId = serverId;
		this.objType = objType;
		this.counter1 = counter1;
		this.counter2 = counter2;
		setImageDescriptor(Images.getCounterImageDescriptor(objType, counter1, serverId));
		setText(name);
	}

	public void run() {
		if (window != null) {
			try {
				window.getActivePage().showView(
						CounterRTAllPairChart2.ID, serverId + "&" + objType + "&" + counter1 + "&" + counter2 ,
						IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				ConsoleProxy.errorSafe("Error opening view:" + e.getMessage());
			}
		}
	}
}
