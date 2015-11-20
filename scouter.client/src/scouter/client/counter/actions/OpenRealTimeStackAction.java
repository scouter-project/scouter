package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.client.views.CounterMapStackView;

public class OpenRealTimeStackAction extends Action {

	IWorkbenchWindow window;
	int serverId;
	int objHash;
	String[] counters;
	String name;
	
	public OpenRealTimeStackAction(IWorkbenchWindow window, String name, int serverId, int objHash, String[] counters) {
		this.window = window;
		this.serverId = serverId;
		this.objHash = objHash;
		this.counters = counters;
		this.name = name;
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.total));
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
						CounterMapStackView.ID, serverId + "&" + objHash + "&" + name + sb.toString(),
						IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				ConsoleProxy.errorSafe("Error opening view:" + e.getMessage());
			}
		}
	}
}
