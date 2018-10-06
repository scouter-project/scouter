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
package scouter.client.xlog.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.constants.HelpConstants;
import scouter.client.model.AgentModelThread;
import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.xlog.XLogFilterStatus;
import scouter.client.xlog.XLogYAxisEnum;
import scouter.client.xlog.dialog.XLogFilterDialog;
import scouter.client.xlog.dialog.XLogSummaryIPDialog;
import scouter.client.xlog.dialog.XLogSummaryRefererDialog;
import scouter.client.xlog.dialog.XLogSummaryServiceDialog;
import scouter.client.xlog.dialog.XLogSummaryUserAgentDialog;
import scouter.client.xlog.dialog.XLogSummaryUserDialog;
import scouter.client.xlog.views.XLogViewPainter.ITimeChange;
import scouter.util.DateTimeHelper;
import scouter.util.LongKeyLinkedMap;
import scouter.util.StringUtil;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public abstract class XLogViewCommon extends ViewPart implements ITimeChange, IObjectCheckListener {
	protected XLogViewPainter viewPainter;
	protected XLogViewMouse mouse;
	protected Canvas canvas;
	protected LongKeyLinkedMap<XLogData> twdata  = new LongKeyLinkedMap<XLogData>();
	protected String objType;
	
	public String yyyymmdd;
	
	long lastRedraw = 0L;
	
	XLogFilterDialog filterDialog;
	XLogFilterStatus filterStatus = new XLogFilterStatus();
	
	XLogSummaryServiceDialog summaryServiceDialog;
	XLogSummaryIPDialog summaryIpDialog;
	XLogSummaryUserAgentDialog summaryUserAgentDialog;
	XLogSummaryRefererDialog summaryRefererDialog;
	XLogSummaryUserDialog summaryUserDialog;
	
	Menu contextMenu;
	
	protected MenuItem onlySqlItem, onlyApiCallItem, onlyErrorItem;
	protected Display display;
	protected Shell shell;
	protected ToolTip toolTip;
	
	Action onlySqlAction, onlyApicallAction, onlyErrorAction, helpAction;

	protected abstract void openInExternalLink();
	protected abstract void clipboardOfExternalLink();

	protected String makeExternalUrl(int serverId) {
		Server server = ServerManager.getInstance().getServer(serverId);
		String linkName = server.getExtLinkName();
		String linkUrl = server.getExtLinkUrlPattern();

		String objHashes = AgentModelThread.getInstance().getLiveObjectHashStringWithParent(serverId, objType);
		if (StringUtil.isEmpty(objHashes)) {
			return "";
		}

		String from = viewPainter.lastDrawTimeStart > 0 ? String.valueOf(viewPainter.lastDrawTimeStart)
				: String.valueOf(System.currentTimeMillis() - DateTimeHelper.MILLIS_PER_FIVE_MINUTE);

		String to = viewPainter.lastDrawTimeStart > 0 ? String.valueOf(viewPainter.lastDrawTimeEnd)
				: String.valueOf(System.currentTimeMillis());

		linkUrl = linkUrl.replace("$[objHashes]", objHashes);
		linkUrl = linkUrl.replace("$[from]", from);
		linkUrl = linkUrl.replace("$[to]", to);
		linkUrl = linkUrl.replace("$[objType]", objType);

		return linkUrl;
	}

	public void create(Composite parent, IToolBarManager man) {

		helpAction = new Action("help", ImageUtil.getImageDescriptor(Images.help)) {
			public void run() {
				org.eclipse.swt.program.Program.launch(HelpConstants.HELP_URL_XLOG_VIEW);
			}
		};
		man.add(helpAction);

		man.add(new Separator());

		Action filterOpenAction = new Action("filter", ImageUtil.getImageDescriptor(Images.filter)) {
			public void run() {
				filterDialog.setStatus(filterStatus);
				filterDialog.open();
			}
		};
		man.add(filterOpenAction);
		man.add(new Separator());

		onlySqlAction = new Action("Only SQL", IAction.AS_CHECK_BOX) {
			public void run() {
				filterStatus.onlySql = isChecked();
				onlySqlItem.setSelection(isChecked());
				setFilter(filterStatus);
			}
		};
		onlySqlAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.database_go));
		man.add(onlySqlAction);

		onlyApicallAction = new Action("Only ApiCall", IAction.AS_CHECK_BOX) {
			public void run() {
				filterStatus.onlyApicall = isChecked();
				onlyApiCallItem.setSelection(isChecked());
				setFilter(filterStatus);
			}
		};
		onlyApicallAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.link));
		man.add(onlyApicallAction);

		onlyErrorAction = new Action("Only Error", IAction.AS_CHECK_BOX) {
			public void run() {
				filterStatus.onlyError = isChecked();
				onlyErrorItem.setSelection(isChecked());
				setFilter(filterStatus);
			}
		};
		onlyErrorAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.error));
		man.add(onlyErrorAction);

		man.add(new Separator());

		canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		canvas.setLayout(new GridLayout());

		mouse = new XLogViewMouse(twdata, canvas);
		viewPainter = new XLogViewPainter(twdata, mouse, this);
		viewPainter.set(canvas.getClientArea());
		mouse.setPainter(viewPainter);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				try {
					viewPainter.drawImageBuffer(e.gc);
					viewPainter.drawSelectArea(e.gc);
				} catch (Throwable t) {
				}
			}
		});
		canvas.addMouseListener(mouse);
		canvas.addMouseMoveListener(mouse);
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				final int key = e.keyCode;
				ExUtil.asyncRun(new Runnable() {
					public void run() {
						viewPainter.keyPressed(key);
						viewPainter.build();
						ExUtil.exec(canvas, new Runnable() {
							public void run() {
								canvas.redraw();
							}
						});
					}
				});
			}
		});
		
		filterDialog = new XLogFilterDialog(this);
		summaryServiceDialog = new XLogSummaryServiceDialog(getViewSite().getShell().getDisplay(), twdata);
		summaryIpDialog = new XLogSummaryIPDialog(getViewSite().getShell().getDisplay(), twdata);
		summaryUserAgentDialog = new XLogSummaryUserAgentDialog(getViewSite().getShell().getDisplay(), twdata);
		summaryRefererDialog = new XLogSummaryRefererDialog(getViewSite().getShell().getDisplay(), twdata);
		summaryUserDialog = new XLogSummaryUserDialog(getViewSite().getShell().getDisplay(), twdata);
		createContextMenu();
		ObjectSelectManager.getInstance().addObjectCheckStateListener(this);
	}
	
	private void createContextMenu() {
		contextMenu = new Menu(canvas);
	    MenuItem filterItem = new MenuItem(contextMenu, SWT.CASCADE);
	    filterItem.setText("Filter");
	    Menu filterMenu = new Menu(contextMenu);
	    filterItem.setMenu(filterMenu);
	    
	    MenuItem detailFilterItem = new MenuItem(filterMenu, SWT.PUSH);
	    detailFilterItem.setText("&Open Filter Dialog");
	    detailFilterItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				filterDialog.setStatus(filterStatus);
				filterDialog.open();
			}
		});
	    
	    new MenuItem(filterMenu, SWT.SEPARATOR);
	    
	    onlySqlItem = new MenuItem(filterMenu, SWT.CHECK);
	    onlySqlItem.setText("SQL");
	    onlySqlItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				onlySqlAction.setChecked(onlySqlItem.getSelection());
				onlySqlAction.run();
			}
		});
	    onlyApiCallItem = new MenuItem(filterMenu, SWT.CHECK);
	    onlyApiCallItem.setText("API Call");
	    onlyApiCallItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				onlyApicallAction.setChecked(onlyApiCallItem.getSelection());
				onlyApicallAction.run();
			}
		});
	    onlyErrorItem = new MenuItem(filterMenu, SWT.CHECK);
	    onlyErrorItem.setText("Error");
	    onlyErrorItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				onlyErrorAction.setChecked(onlyErrorItem.getSelection());
				onlyErrorAction.run();
			}
		});
	    
	    MenuItem yAxisItem = new MenuItem(contextMenu, SWT.CASCADE);
	    yAxisItem.setText("Y Axis");
	    Menu yAxisMenu = new Menu(contextMenu);
	    yAxisItem.setMenu(yAxisMenu);
	    for (final XLogYAxisEnum yaxis : XLogYAxisEnum.values()) {
	    	 MenuItem item = new MenuItem(yAxisMenu, SWT.RADIO);
	    	 item.setText(yaxis.getName());
	    	 item.addListener(SWT.Selection, new Listener(){
	    		 public void handleEvent(Event event) {
	    			 viewPainter.setYAxisMode(yaxis);
	    			 viewPainter.build();
	    			 canvas.redraw();
	    		 }
	    	 });
	    	 if (yaxis.isDefault()) {
		    	 item.setSelection(true);
		    	 item.notifyListeners(SWT.Selection, new Event());
	    	 }
	    }

		new MenuItem(filterMenu, SWT.SEPARATOR);
		MenuItem extLinkItem = new MenuItem(contextMenu, SWT.CASCADE);
		extLinkItem.setText("Open in 3rd-party UI");
		Menu extLinkMenu = new Menu(contextMenu);
		extLinkItem.setMenu(extLinkMenu);

		MenuItem openExternal = new MenuItem(extLinkMenu, SWT.PUSH);
		openExternal.setText("Open in 3rd party UI");
		openExternal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openInExternalLink();
			}
		});

		MenuItem copyExternal = new MenuItem(extLinkMenu, SWT.PUSH);
		copyExternal.setText("Copy to clip board for 3rd party UI");
		copyExternal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				clipboardOfExternalLink();
			}
		});

	    new MenuItem(contextMenu, SWT.SEPARATOR);
	    MenuItem summaryItem = new MenuItem(contextMenu, SWT.CASCADE);
	    summaryItem.setText("Summary");
	    Menu summaryMenu = new Menu(contextMenu);
	    summaryItem.setMenu(summaryMenu);
	    MenuItem serviceSummary = new MenuItem(summaryMenu, SWT.PUSH);
	    serviceSummary.setText("Service");
	    serviceSummary.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				long etime = viewPainter.getLastTime();
				long stime = etime - viewPainter.getTimeRange();
				summaryServiceDialog.setRange(stime, etime);
				summaryServiceDialog.show();
			}
		});
	    MenuItem ipSummary = new MenuItem(summaryMenu, SWT.PUSH);
	    ipSummary.setText("IP");
	    ipSummary.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				long etime = viewPainter.getLastTime();
				long stime = etime - viewPainter.getTimeRange();
				summaryIpDialog.setRange(stime, etime);
				summaryIpDialog.show();
			}
		});
	    MenuItem userAgentSummary = new MenuItem(summaryMenu, SWT.PUSH);
	    userAgentSummary.setText("User-Agent");
	    userAgentSummary.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				long etime = viewPainter.getLastTime();
				long stime = etime - viewPainter.getTimeRange();
				summaryUserAgentDialog.setRange(stime, etime);
				summaryUserAgentDialog.show();
			}
		});
	    MenuItem refererSummary = new MenuItem(summaryMenu, SWT.PUSH);
	    refererSummary.setText("Referer");
	    refererSummary.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				long etime = viewPainter.getLastTime();
				long stime = etime - viewPainter.getTimeRange();
				summaryRefererDialog.setRange(stime, etime);
				summaryRefererDialog.show();
			}
		});
	    MenuItem userSummary = new MenuItem(summaryMenu, SWT.PUSH);
	    userSummary.setText("User");
	    userSummary.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				long etime = viewPainter.getLastTime();
				long stime = etime - viewPainter.getTimeRange();
				summaryUserDialog.setRange(stime, etime);
				summaryUserDialog.show();
			}
		});
	    
	    canvas.setMenu(contextMenu);
	}
	
	public int getMaxCount() {
		return PManager.getInstance().getInt(PreferenceConstants.P_XLOG_MAX_COUNT);
	}

	public void setObjType(String objType){
		mouse.setObjType(objType);
	}
	
	public void setDate(String yyyymmdd){
		this.yyyymmdd = yyyymmdd;
		viewPainter.yyyymmdd = yyyymmdd;
		mouse.yyyymmdd = yyyymmdd;
	}
	
	public void setFocus() {
	}

	public void dispose() {
		super.dispose();
		ObjectSelectManager.getInstance().removeObjectCheckStateListener(this);
		if (this.viewPainter != null) {
			viewPainter.dispose();
		}
		twdata.clear();
	}
	
	public String[] getExistObjNames() {
		if(twdata == null)
			return new String[0];
		Set<String> objNames = new HashSet<String>();
		Enumeration<XLogData> en = twdata.values();
		while (en.hasMoreElements()) {
			XLogData data = en.nextElement();
			String objName = data.objName;
			if (objName == null)
				objName = TextProxy.object.getLoadText(yyyymmdd, data.p.objHash, data.serverId);
			objNames.add(objName);
		}
		return objNames.toArray(new String[objNames.size()]);
	}
	
	static class CountComparator implements Comparator<XLogSnapshotData> {
        public int compare(XLogSnapshotData a, XLogSnapshotData b) {
        	int x1 = a.cnt;
        	int x2 = b.cnt;
        	if (x1 == x2) {
        		return a.sum > b.sum ? -1 : 1;
        	}
        	return x1 > x2 ? -1 : 1;
        }
    }
	
	ObjectSelectManager objSelMgr = ObjectSelectManager.getInstance();
	
	public void timeRangeChanged(long time_start, long time_end) {
		XLogData first = twdata.getFirstValue();
		XLogData last = twdata.getLastValue();
		
		if (first == null || last == null) {
			loadAdditinalData(time_start, time_end, false);
		} else if (first.p.endTime > time_end ) {
			loadAdditinalData(time_start, time_end, true);
		} else if (last.p.endTime < time_start) {
			loadAdditinalData(time_start, time_end, false);
		} else {
			long firstTime = first.p.endTime;
			long lastTime = last.p.endTime;
			if (firstTime >= time_start && firstTime <= time_end) {
				// time_start----------firstTime------------time_end
				loadAdditinalData(time_start, firstTime, true);
			} 
			if (lastTime >= time_start && lastTime <= time_end) {
				// time_start---------lastTime---------time_end
				loadAdditinalData(lastTime, time_end, false);
			} 
		}
	}
	public abstract void loadAdditinalData(long stime, long etime, boolean reverse);

	public void notifyChangeState() {
		viewPainter.build();
		canvas.redraw();
	}
	
	public void setFilter(XLogFilterStatus filter) {
		this.filterStatus = filter;
		onlySqlAction.setChecked(filterStatus.onlySql);
		onlySqlItem.setSelection(filterStatus.onlySql);
		onlyApicallAction.setChecked(filterStatus.onlyApicall);
		onlyApiCallItem.setSelection(filterStatus.onlyApicall);
		onlyErrorAction.setChecked(filterStatus.onlyError);
		onlyErrorItem.setSelection(filterStatus.onlyError);
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				viewPainter.setFilterStatus(filterStatus);
				viewPainter.build();
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						canvas.redraw();
					}
				});
			}
		});
	}
}
