/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.cubrid.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.cubrid.views.CubridSingleDailyPeriodMultiView;
import scouter.client.cubrid.views.CubridSinglePeriodMultiView;
import scouter.client.cubrid.views.CubridSingleRealTimeMultiView;
import scouter.client.cubrid.CubridSingleItem;
import scouter.client.cubrid.CubridTypePeriod;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.util.DateUtil;

public class MultiViewDialogAction extends Action {

	IWorkbenchWindow window;
	final private int serverId;
	final private CubridTypePeriod periodType;

	public MultiViewDialogAction(IWorkbenchWindow window, int serverId, CubridTypePeriod periodType) {
		this.window = window;
		this.serverId = serverId;
		this.periodType = periodType;
		setText(periodType.getTitle());
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.add));
	}

	public void run() {

		if (periodType == CubridTypePeriod.REALTIME) {
			AddRealTimeDialog dialog = new AddRealTimeDialog(window.getShell().getDisplay(), serverId,
					new AddRealTimeDialog.IAddSingleRealTimeDialog() {
						@Override
						public void onPressedOk(String dbName, CubridSingleItem viewType, long timeRange) {
							System.out.println("MultiViewDialogAction dbName : " + dbName);
							try {
								window.getActivePage().showView(CubridSingleRealTimeMultiView.ID,
										serverId + "&" + dbName + "&" + viewType.ordinal() + "&" + timeRange,
										IWorkbenchPage.VIEW_ACTIVATE);
							} catch (PartInitException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void onPressedCancel() {
						}
					});

			dialog.show();
		} else if (periodType == CubridTypePeriod.PAST_LESS_1DAY) {
			AddShortPeriodCalendarDialog dialog = new AddShortPeriodCalendarDialog(window.getShell().getDisplay(), serverId,
					new AddShortPeriodCalendarDialog.IAddSingleShortPeriodDialog() {

						@Override
						public void onPressedOk(String dbName, CubridSingleItem viewType, long stime, long etime) {
							try {
								window.getActivePage().showView(CubridSinglePeriodMultiView.ID,
										serverId + "&" + dbName + "&" + viewType.ordinal() + "&" + stime + "&" + etime,
										IWorkbenchPage.VIEW_ACTIVATE);
							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						@Override
						public void onPressedCancel() {
						}
					});
			int hourRange = DateUtil.getHour(TimeUtil.getCurrentTime(serverId));
			int MiniteRange = DateUtil.getMin(TimeUtil.getCurrentTime(serverId));
			if (hourRange > 4) {
				dialog.show(TimeUtil.getCurrentTime(serverId) - DateUtil.MILLIS_PER_HOUR * 4,
						TimeUtil.getCurrentTime(serverId));
			} else {
				dialog.show(TimeUtil.getCurrentTime(serverId) - DateUtil.MILLIS_PER_HOUR * hourRange - DateUtil.MILLIS_PER_MINUTE * MiniteRange,
						TimeUtil.getCurrentTime(serverId));
			}
		} else {
			AddLongPeriodCalendarDialog dialog = new AddLongPeriodCalendarDialog(window.getShell().getDisplay(), serverId,
					new AddLongPeriodCalendarDialog.IAddSingleLongPeriodDialog() {

						@Override
						public void onPressedOk(String dbName, CubridSingleItem viewType, String sDate,
								String eDate) {
							try {
								window.getActivePage().showView(CubridSingleDailyPeriodMultiView.ID,
										serverId + "&" + dbName + "&" + viewType.ordinal() + "&" + sDate + "&" + eDate,
										IWorkbenchPage.VIEW_ACTIVATE);
							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

						@Override
						public void onPressedCancel() {
						}
					});
			dialog.show(TimeUtil.getCurrentTime(serverId), TimeUtil.getCurrentTime(serverId));
		}
	}

}
