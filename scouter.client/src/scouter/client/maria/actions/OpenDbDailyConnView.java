package scouter.client.maria.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.maria.views.DbDailyTotalConnView;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.util.DateUtil;

public class OpenDbDailyConnView extends Action {
	
	final private int serverId;
	String date;
	
	public OpenDbDailyConnView(int serverId) {
		this(serverId, null);
	}
	
	public OpenDbDailyConnView(int serverId, String date) {
		this.serverId = serverId;
		setText("Open Daily Connection");
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.calendar));
	}

	public void run() {
		try {
			CalendarDialog dialog = new CalendarDialog(Display.getDefault(), new ILoadCalendarDialog() {
				public void onPressedOk(long startTime, long endTime) {
				}

				public void onPressedOk(String date) {
					try {
						PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.showView(DbDailyTotalConnView.ID, serverId + "&" + date, IWorkbenchPage.VIEW_ACTIVATE);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}

				public void onPressedCancel() {
				}
			});
			if (date == null) {
				dialog.show();				
			} else {
				dialog.show(-1, -1, DateUtil.yyyymmdd(date));
			}
			
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		}
	}
}
