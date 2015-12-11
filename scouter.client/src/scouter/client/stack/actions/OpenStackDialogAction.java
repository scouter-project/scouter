package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

import scouter.client.model.TextProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.stack.dialog.StackListDialog;

public class OpenStackDialogAction extends Action {
	public final static String ID = OpenStackDialogAction.class.getName();
	
	int serverId;
	int objHash;

	public OpenStackDialogAction(int serverId, int objHash) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.setText("Analyze");
	}
	
	public void run(){
		new CalendarDialog(Display.getDefault(), new ILoadCalendarDialog() {
			public void onPressedOk(String date) {
				String objName = TextProxy.object.getText(objHash);
				new StackListDialog(serverId, objName, date).open();
			}
			public void onPressedOk(long startTime, long endTime) {
				
			}
			public void onPressedCancel() {
			}
		}).show();
	}
}