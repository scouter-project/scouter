package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.counter.views.CounterPTAllPairChart;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.util.DateUtil;

public class OpenPTPairAllAction extends Action implements ILoadCalendarDialog {

	IWorkbenchWindow window;
	int serverId;
	String objType;
	String counter;
	
	public OpenPTPairAllAction(IWorkbenchWindow window, String name, int serverId, String objType, String counter) {
		this.window = window;
		this.serverId = serverId;
		this.objType = objType;
		this.counter = counter;
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.calendar));
		setText(name);
	}

	public void run() {
		CalendarDialog dialog = new CalendarDialog(window.getShell().getDisplay(), this);
		dialog.showWithTime(-1, -1, TimeUtil.getCurrentTime(serverId) - DateUtil.MILLIS_PER_FIVE_MINUTE);
	}

	public void onPressedOk(long startTime, long endTime) {
		if (window != null) {
			try {
				CounterPTAllPairChart chart = (CounterPTAllPairChart) window.getActivePage().showView(
						CounterPTAllPairChart.ID, serverId + "&" + objType + "&" + counter ,
						IWorkbenchPage.VIEW_ACTIVATE);
				if (chart != null) {
					chart.setInput(startTime, endTime);
				}
			} catch (PartInitException e) {
				ConsoleProxy.errorSafe("Error opening view:" + e.getMessage());
			}
		}
	}

	public void onPressedOk(String date) {
		
	}

	public void onPressedCancel() {
		
	}
}
