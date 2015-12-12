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
package scouter.client.visitor.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.Images;
import scouter.client.counter.actions.OpenVisitorLoadAction;
import scouter.client.counter.actions.OpenVisitorTotalLoadAction;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.views.DigitalCountView;
import scouter.lang.pack.MapPack;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class VisitorLoadView extends DigitalCountView implements ILoadCalendarDialog {
	
	public static final String ID = VisitorLoadView.class.getName();
	
	private int serverId;
	
	String requestCmd;
	MapPack param;
	String date;
	
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
		date = ids[1];
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Load", ImageUtil.getImageDescriptor(Images.calendar)) {
			public void run() {
				CalendarDialog dialog = new CalendarDialog(getSite().getShell().getDisplay(), VisitorLoadView.this);
				dialog.show(-1, -1, DateUtil.getTime(date, "yyyyMMdd"));
			}
		});
		Menu contextMenu = new Menu(canvas);
		MenuItem loadCalendaer = new MenuItem(contextMenu, SWT.PUSH);
		loadCalendaer.setText("&Load");
		loadCalendaer.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				CalendarDialog dialog = new CalendarDialog(getSite().getShell().getDisplay(), VisitorLoadView.this);
				dialog.show();
			}
		});
		canvas.setMenu(contextMenu);
	}

	public void setInput(String title, String requestCmd, MapPack param){
    	super.title = title == null ? super.title : title;
    	this.requestCmd = requestCmd;
    	this.param = param;
    	load();
    }
	
	@Override
	public void dispose() {
		super.dispose();
	}


	@Override
	public void setFocus() {
	}

	public void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				Value v = null;
				try {
					v = tcp.getSingleValue(requestCmd, param);
				} catch(Exception e){
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (v != null) {
					value = FormatUtil.print(CastUtil.clong(v), "#,##0");
				} else {
					value = "unavailable";
				}
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						canvas.redraw();
					}
				});
			}
		});
	}

	public void onPressedOk(String date) {
		if (param.getText("objType") == null) {
			new OpenVisitorLoadAction(getSite().getWorkbenchWindow(), serverId, date, param.getInt("objHash")).run();
		} else {
			new OpenVisitorTotalLoadAction(getSite().getWorkbenchWindow(), serverId, date, param.getText("objType")).run();
		}
	}

	public void onPressedOk(long startTime, long endTime) {
	}

	public void onPressedCancel() {
	}
}
