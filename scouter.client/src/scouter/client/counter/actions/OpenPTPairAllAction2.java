package scouter.client.counter.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.counter.views.CounterPTAllPairChart2;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.util.DateUtil;

public class OpenPTPairAllAction2 extends Action implements ILoadCalendarDialog {

	IWorkbenchWindow window;
	int serverId;
	String objType;
	String counter1;
	String counter2;
	
	public OpenPTPairAllAction2(IWorkbenchWindow window, String name, int serverId, String objType, String counter1, String counter2) {
		this.window = window;
		this.serverId = serverId;
		this.objType = objType;
		this.counter1 = counter1;
		this.counter2= counter2;
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
				CounterPTAllPairChart2 chart = (CounterPTAllPairChart2) window.getActivePage().showView(
						CounterPTAllPairChart2.ID, serverId + "&" + objType + "&" + counter1 + "&" + counter2,
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
