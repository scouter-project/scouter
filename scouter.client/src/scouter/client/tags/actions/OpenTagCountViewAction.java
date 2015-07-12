package scouter.client.tags.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.popup.CalendarObjTypeDialog;
import scouter.client.popup.CalendarObjTypeDialog.ICalendarCallback;
import scouter.client.tags.TagCountView;
import scouter.client.util.ImageUtil;

public class OpenTagCountViewAction extends Action implements ICalendarCallback {
	
	IWorkbenchWindow window;
	int serverId;

	public OpenTagCountViewAction(IWorkbenchWindow window, int serverId) {
		this.window = window;
		this.serverId = serverId;
		setText("&Tag Count");
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.bar));
	}

	public void run() {
		try {
			CalendarObjTypeDialog dialog = new CalendarObjTypeDialog(window.getShell().getDisplay(), this, serverId);
			dialog.show();
		} catch (Exception e) {
			MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
		}
		
	}

	public void onPressedOk(String date, String objType) {
		try {
			TagCountView v = (TagCountView) window.getActivePage().showView(TagCountView.ID, serverId + "", IWorkbenchPage.VIEW_ACTIVATE);
			if (v != null) {
				v.setInput(date, objType);
			}
		} catch (Exception e) {
			MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
		}
	}
}
