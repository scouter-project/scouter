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
package scouter.client.util;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.actions.OpenActiveServiceListAction;
import scouter.client.actions.OpenActiveSpeedAction;
import scouter.client.actions.OpenEQViewAction;
import scouter.client.actions.OpenServiceGroupElapsedAction;
import scouter.client.actions.OpenServiceGroupTPSAction;
import scouter.client.actions.OpenVerticalEQViewAction;
import scouter.client.actions.SetColorAction;
import scouter.client.batch.actions.OpenCxtmenuBatchActiveListAction;
import scouter.client.batch.actions.OpenCxtmenuBatchHistoryAction;
import scouter.client.configuration.actions.DefineObjectTypeAction;
import scouter.client.configuration.actions.OpenAgentConfigureAction;
import scouter.client.constants.MenuStr;
import scouter.client.context.actions.OpenAPIDebugViewAction;
import scouter.client.context.actions.OpenCxtmenuActiveServiceListAction;
import scouter.client.context.actions.OpenCxtmenuDumpActiveServiceListAction;
import scouter.client.context.actions.OpenCxtmenuDumpFileListAction;
import scouter.client.context.actions.OpenCxtmenuDumpHeapHistoAction;
import scouter.client.context.actions.OpenCxtmenuDumpThreadDumpAction;
import scouter.client.context.actions.OpenCxtmenuDumpThreadListAction;
import scouter.client.context.actions.OpenCxtmenuEnvAction;
import scouter.client.context.actions.OpenCxtmenuFileSocketAction;
import scouter.client.context.actions.OpenCxtmenuHeapHistoViewAction;
import scouter.client.context.actions.OpenCxtmenuObjectClassListAction;
import scouter.client.context.actions.OpenCxtmenuObjectThreadDumpAction;
import scouter.client.context.actions.OpenCxtmenuPropertiesAction;
import scouter.client.context.actions.OpenCxtmenuResetCacheAction;
import scouter.client.context.actions.OpenCxtmenuSystemGcAction;
import scouter.client.context.actions.OpenCxtmenuThreadListAction;
import scouter.client.counter.actions.OpenDailyServiceCountAction;
import scouter.client.counter.actions.OpenPastDateViewAction;
import scouter.client.counter.actions.OpenPastLongDateAllAction;
import scouter.client.counter.actions.OpenPastLongDateTotalAction;
import scouter.client.counter.actions.OpenPastTimeAllAction;
import scouter.client.counter.actions.OpenPastTimeTotalAction;
import scouter.client.counter.actions.OpenPastTimeViewAction;
import scouter.client.counter.actions.OpenRTPairAllAction;
import scouter.client.counter.actions.OpenRTPairAllAction2;
import scouter.client.counter.actions.OpenRealTimeAllAction;
import scouter.client.counter.actions.OpenRealTimeMultiAction;
import scouter.client.counter.actions.OpenRealTimeStackAction;
import scouter.client.counter.actions.OpenRealTimeTotalAction;
import scouter.client.counter.actions.OpenRealTimeViewAction;
import scouter.client.counter.actions.OpenSummaryAction;
import scouter.client.counter.actions.OpenTodayAllAction;
import scouter.client.counter.actions.OpenTodayServiceCountAction;
import scouter.client.counter.actions.OpenTodayTotalAction;
import scouter.client.counter.actions.OpenTodayViewAction;
import scouter.client.counter.actions.OpenTypeSummaryAction;
import scouter.client.counter.actions.OpenUniqueTotalVisitorAction;
import scouter.client.counter.actions.OpenUniqueVisitorAction;
import scouter.client.counter.views.CounterLoadDateView;
import scouter.client.counter.views.CounterLoadTimeView;
import scouter.client.counter.views.CounterPastLongDateAllView;
import scouter.client.counter.views.CounterPastLongDateTotalView;
import scouter.client.counter.views.CounterPastTimeAllView;
import scouter.client.counter.views.CounterPastTimeTotalView;
import scouter.client.counter.views.CounterRealDateView;
import scouter.client.counter.views.CounterRealTimeAllView;
import scouter.client.counter.views.CounterRealTimeTotalView;
import scouter.client.counter.views.CounterRealTimeView;
import scouter.client.counter.views.CounterTodayAllView;
import scouter.client.counter.views.CounterTodayTotalView;
import scouter.client.heapdump.actions.HeapDumpAction;
import scouter.client.heapdump.actions.HeapDumpListAction;
import scouter.client.host.actions.OpenDiskUsageAction;
import scouter.client.host.actions.OpenTopAction;
import scouter.client.maria.actions.OpenDbRealtimeWaitCountAction;
import scouter.client.model.AgentObject;
import scouter.client.model.TextProxy;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.stack.actions.OpenStackDialogAction;
import scouter.client.stack.actions.TurnOffStackAction;
import scouter.client.stack.actions.TurnOnStackAction;
import scouter.client.xlog.actions.OpenXLogLoadTimeAction;
import scouter.client.xlog.actions.OpenXLogRealTimeAction;
import scouter.lang.Counter;
import scouter.lang.ObjectType;
import scouter.lang.counters.CounterConstants;
import scouter.lang.counters.CounterEngine;
import scouter.util.DateUtil;

public class MenuUtil implements IMenuCreator{

	private Menu menu;
	private ArrayList<Action> menuActions = null;
	private int disableInx = -1;
	int[] separator;
	
	public MenuUtil() {
		super();
	}

	public MenuUtil(ArrayList<Action> menuActions, int disableInx) {
		super();
		this.menuActions = menuActions;
		this.disableInx = disableInx;
	}
	
	public MenuUtil(ArrayList<Action> menuActions, int disableInx, int[] separator) {
		super();
		this.menuActions = menuActions;
		this.disableInx = disableInx;
		this.separator = separator;
	}

	public void dispose() {
		if (menu != null) { 
			menu.dispose(); 
			menu = null; 
		} 
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public Menu getMenu(Control parent) {
		if (menu != null){
			menu.dispose();
			menu = null;
		}
		
		menu = new Menu(parent); 
		
		for(int inx = 0 ; inx < menuActions.size() ; inx++){
			
			final Action act = menuActions.get(inx);
			
			if(act.getText() == null || "".equals(act.getText())){
				new MenuItem(menu, SWT.SEPARATOR);
				continue;
			}
			
			MenuItem submenu = new MenuItem(menu, SWT.CASCADE);
			submenu.setText(act.getText());
			submenu.setImage(act.getImageDescriptor().createImage());
			if(inx == disableInx){
				submenu.setEnabled(false);
			}
			
			submenu.addSelectionListener(new SelectionListener() {
				
				public void widgetSelected(SelectionEvent e) {
					act.run();
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		return menu;
	}

	public static void createMenu(IWorkbenchWindow window, IToolBarManager man,
			ArrayList<Action> menuArray, Image image) {
		
		createMenu(window, man, menuArray, image, -1);
	}
	
	public static void createMenu(IWorkbenchWindow window, IToolBarManager man,
			ArrayList<Action> menuArray, Image image, int disableInx) {
		if(man.isEmpty()){
			if(menuArray.size() == 1){
				man.add(menuArray.get(0));
			}else{
				Action act = new Action("Show related Views", SWT.DROP_DOWN){};
				act.setImageDescriptor(ImageUtil.getImageDescriptor(image));
				act.setMenuCreator(new MenuUtil(menuArray, disableInx));
				man.add(act);
			}
		}
	}
	
	public static HashMap<String, Action> getCounterActionList(IWorkbenchWindow window, CounterEngine counterEngine, int serverId){
		HashMap<String, Action> actions = new HashMap<String, Action>();
		ArrayList<String> objTypeAndCounter = counterEngine.getAllCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(
					objType + ":" + counterName,
					new OpenRealTimeAllAction(window, label, objType, counterName, Images
							.getCounterImage(objType, counterName, serverId), serverId));
		}
		return actions;
	}
	
	public static HashMap<String, Action> getPastCounterActionList(IWorkbenchWindow window, CounterEngine counterEngine, String curdate, int serverId){
		HashMap<String, Action> actions = new HashMap<String, Action>();
		long st = DateUtil.yyyymmdd(curdate);
		long et = st + DateUtil.MILLIS_PER_FIVE_MINUTE;
		ArrayList<String> objTypeAndCounter = counterEngine.getAllCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_TIME_ALL,
					new OpenPastTimeAllAction(window, label, objType, counterName,
							Images.getCounterImage(objType, counterName, serverId), st, et, serverId));
		}
		
		objTypeAndCounter = counterEngine.getAllCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_DATE_ALL,
					new OpenPastLongDateAllAction(window, label, objType, counterName,
							Images.getCounterImage(objType, counterName, serverId), curdate, curdate, serverId));
		}
		
		objTypeAndCounter = counterEngine.getTotalCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_TIME_TOTAL,
					new OpenPastTimeTotalAction(window, label, objType,
							counterName, Images.getCounterImage(objType, counterName, serverId), st, et, serverId));
		}
		
		objTypeAndCounter = counterEngine.getTotalCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_DATE_TOTAL,
					new OpenPastLongDateTotalAction(window, label, objType,
							counterName, Images.getCounterImage(objType, counterName, serverId), curdate, curdate, serverId));
		}
		return actions;
	}
	
	public static void addObjectContextMenu(IMenuManager mgr, IWorkbenchWindow win, AgentObject object) {
		int serverId = object.getServerId();
    	String objType = object.getObjType();
    	int objHash = object.getObjHash();
    	String objName = object.getObjName();
    	Server server = ServerManager.getInstance().getServer(serverId);  
    	CounterEngine counterEngine = server.getCounterEngine();
    	String[] counterNames = counterEngine.getSortedCounterName(objType);
    	
    	MenuManager performanceCounter = new MenuManager(MenuStr.PERFORMANCE_COUNTER,  ImageUtil.getImageDescriptor(Images.CTXMENU_RTC), MenuStr.PERFORMANCE_COUNTER_ID);
    	mgr.add(performanceCounter);
    	
    	if (object.isAlive() && counterNames != null) {
	    	for(int inx = 0 ; inx < counterNames.length ; inx++){
	    		String counter = counterNames[inx];
	    		String counterDisplay = counterEngine.getCounterDisplayName(objType, counter);
				performanceCounter.add(new OpenRealTimeViewAction(win, counterDisplay, counter, Images.getCounterImage(objType, counter, serverId), objHash, objName, objType, serverId));
			}
    	}
    	
    	if(counterEngine.isChildOf(objType, CounterConstants.FAMILY_JAVAEE)) {
    		performanceCounter.add(new Separator());
    		performanceCounter.add(new OpenUniqueVisitorAction(win, serverId, objHash));
    		performanceCounter.add(new OpenSummaryAction(win, serverId, objHash));
    	} else if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_HOST)) {
    		performanceCounter.add(new Separator());
    		performanceCounter.add(new OpenRealTimeStackAction(win, "Sys/User CPU", serverId, objHash, 
    				new String[] {CounterConstants.HOST_SYSCPU, CounterConstants.HOST_USERCPU}));
    	} else if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_MARIA)) {
    		performanceCounter.add(new Separator());
    		performanceCounter.add(new OpenRealTimeMultiAction(win, "Opened Tables", serverId, objHash, objType
    				, new String[] {"OT_DEF", "OT_COUNT"}));
    		performanceCounter.add(new OpenRealTimeMultiAction(win, "Temporary Tables", serverId, objHash, objType
    				, new String[] {"DTEMP_TBL", "MTEMP_TBL"}));
    		performanceCounter.add(new OpenRealTimeMultiAction(win, "Table Locks", serverId, objHash, objType
    				, new String[] {"TBL_LOCK", "TBL_LOCK_W"}));
    		performanceCounter.add(new OpenDbRealtimeWaitCountAction(serverId, objHash));
    	}
    	
    	if (object.isAlive()) {
	    	MenuManager performanceSnapshot = new MenuManager(MenuStr.PERFORMANCE_REQUEST, Images.CAPTURE, MenuStr.PERFORMANCE_REQUEST_ID);
	    	mgr.add(performanceSnapshot);
	    	
			if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_JAVAEE)) {
				performanceSnapshot.add(new OpenCxtmenuThreadListAction(win, MenuStr.THREAD_LIST, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuActiveServiceListAction(win, MenuStr.ACTIVE_SERVICE_LIST, objHash, objType, serverId));
				performanceSnapshot.add(new OpenCxtmenuObjectClassListAction(win, MenuStr.LOADED_CLASS_LIST, objHash, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_HEAPHISTOGRAM))
					performanceSnapshot.add(new OpenCxtmenuHeapHistoViewAction(win, MenuStr.HEAP_HISTOGRAM, objHash, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_THREADDUMP))
					performanceSnapshot.add(new OpenCxtmenuObjectThreadDumpAction(win, MenuStr.THREAD_DUMP, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuEnvAction(win, MenuStr.ENV, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuFileSocketAction(win, "Socket", objHash, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_SYSTEMGC))
					performanceSnapshot.add(new OpenCxtmenuSystemGcAction(MenuStr.SYSTEM_GC, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuResetCacheAction("Reset Text Cache", objHash, serverId));
				performanceSnapshot.add(new Separator());
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_HEAPDUMP)) {
					MenuManager heapDump = new MenuManager(MenuStr.HEAP_DUMP, MenuStr.HEAP_DUMP_ID);
					performanceSnapshot.add(heapDump);
					heapDump.add(new HeapDumpAction(win, MenuStr.HEAP_DUMP_RUN, ""+objHash, objHash, objName, TimeUtil.getCurrentTime(serverId), Images.heap, serverId));
					heapDump.add(new HeapDumpListAction(win, MenuStr.HEAP_DUMP_LIST, objName, objHash, Images.heap, serverId));
				}
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_FILEDUMP)) {
					MenuManager dumpMgr = new MenuManager(MenuStr.FILEDUMP, MenuStr.FILEDUMP_ID);
					performanceSnapshot.add(dumpMgr);
					dumpMgr.add(new OpenCxtmenuDumpFileListAction(win, MenuStr.LIST_DUMP_FILES, objHash, serverId));
					dumpMgr.add(new Separator());
					dumpMgr.add(new OpenCxtmenuDumpActiveServiceListAction(MenuStr.DUMP_ACTIVE_SERVICE_LIST, objHash, serverId));
					dumpMgr.add(new OpenCxtmenuDumpThreadDumpAction(MenuStr.DUMP_THREAD_DUMP, objHash, serverId));
					dumpMgr.add(new OpenCxtmenuDumpThreadListAction(MenuStr.DUMP_THREAD_LIST, objHash, serverId));
					dumpMgr.add(new OpenCxtmenuDumpHeapHistoAction(MenuStr.DUMP_HEAPHISTO, objHash, serverId));
				}
				mgr.add(new Separator());
				MenuManager stackMgr = new MenuManager(MenuStr.STACK_ANALYZER, ImageUtil.getImageDescriptor(Images.page_white_stack), MenuStr.STACK_ANALYZER_ID);
				mgr.add(stackMgr);
				stackMgr.add(new TurnOnStackAction(serverId, objHash));
				stackMgr.add(new TurnOffStackAction(serverId, objHash));
				stackMgr.add(new Separator());
				stackMgr.add(new OpenStackDialogAction(serverId, objHash));
				
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_CONFIGURE)) {
					mgr.add(new Separator());
					mgr.add(new OpenAgentConfigureAction(win, MenuStr.CONFIGURE, objHash, serverId));
				}
			} else if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_HOST)) {
				performanceSnapshot.add(new OpenCxtmenuEnvAction(win, MenuStr.ENV, objHash, serverId));
				performanceSnapshot.add(new OpenTopAction(win, MenuStr.TOP, objHash, serverId));
				performanceSnapshot.add(new OpenDiskUsageAction(win, MenuStr.DISK_USAGE, objHash, serverId));
				
				mgr.add(new Separator());
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_CONFIGURE))
					mgr.add(new OpenAgentConfigureAction(win, MenuStr.CONFIGURE, objHash, serverId));
			} else if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_BATCH)) {
				performanceCounter.add(new OpenCxtmenuBatchHistoryAction(win, MenuStr.BATCH_HISTORY, objHash, serverId));
				mgr.add(new Separator());
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_CONFIGURE))
					mgr.add(new OpenAgentConfigureAction(win, MenuStr.CONFIGURE, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuBatchActiveListAction(win, MenuStr.BATCH_ACTIVE_LIST, objHash, objType, serverId));
			} 
    	}
    	if (server.isAllowAction(GroupPolicyConstants.ALLOW_DEFINEOBJTYPE)) {
	    	if (counterEngine.isUnknownObjectType(objType)) {
	    		mgr.add(new DefineObjectTypeAction(win, serverId, objType, DefineObjectTypeAction.DEFINE_MODE));
	    	} else {
	    		mgr.add(new DefineObjectTypeAction(win, serverId, objType, DefineObjectTypeAction.EDIT_MODE));
	    	}
    	}
		mgr.add(new SetColorAction(win, objHash));
		mgr.add(new OpenCxtmenuPropertiesAction(win, MenuStr.PROPERTIES, objHash, serverId));
		if (false) {
			mgr.add(new Separator());
			mgr.add(new OpenAPIDebugViewAction(win, objHash, serverId));
		}
	}
	
	public static void createCounterContextMenu(final String id, Control control, final int serverId, final String objType, final String counter) {
		MenuManager mgr = new MenuManager(); 
		mgr.setRemoveAllWhenShown(true);
		final CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		ObjectType objectType = counterEngine.getObjectType(objType);
		if (objectType == null) return;
		final Counter counterObj = objectType.getCounter(counter);
		mgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				if (mgr == null) return;
				IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (counterObj.isAll()) {
					Action act = new OpenRealTimeAllAction(win, "Current All", objType, counter, Images.all, serverId);
					if (CounterRealTimeAllView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
				if (counterObj.isTotal()) {
					Action act = new OpenRealTimeTotalAction(win, "Current Total", objType, counter, Images.total, serverId);
					if (CounterRealTimeTotalView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
				if (counterObj.isAll()) {
					Action act = new OpenTodayAllAction(win, "Today All", objType, counter, Images.all, serverId);
					if (CounterTodayAllView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
				if (counterObj.isTotal()) {
					Action act = new OpenTodayTotalAction(win, "Today Total", objType, counter, Images.total, serverId);
					if (CounterTodayTotalView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
				mgr.add(new Separator());
				if (counterObj.isAll()) {
					Action act = new OpenPastTimeAllAction(win, "Past All", objType, counter, Images.all, -1, -1, serverId);
					if (CounterPastTimeAllView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
				if (counterObj.isTotal()) {
					Action act = new OpenPastTimeTotalAction(win, "Past Total", objType, counter, Images.total, -1, -1, serverId);
					if (CounterPastTimeTotalView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
				if (counterObj.isAll()) {
					Action act = new OpenPastLongDateAllAction(win, "Daily All", objType, counter, Images.all, null, null, serverId);
					if (CounterPastLongDateAllView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
				if (counterObj.isTotal()) {
					Action act = new OpenPastLongDateTotalAction(win, "Daily Total", objType, counter, Images.total, null, null, serverId);
					if (CounterPastLongDateTotalView.ID.equals(id)) {
						act.setEnabled(false);
					}
					mgr.add(act);
				}
			}
		});
		Menu menu = mgr.createContextMenu(control); 
		control.setMenu(menu); 
	}
	
	public static void createCounterContextMenu(final String id, Control control, final int serverId, final int objHash, final String objType, final String counter) {
		MenuManager mgr = new MenuManager(); 
		mgr.setRemoveAllWhenShown(true);
		final String objName = TextProxy.object.getText(objHash);
		mgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				Action act = new OpenRealTimeViewAction(win, "Current", counter, Images.CTXMENU_RTC, objHash, objName, objType, serverId);
				if (CounterRealTimeView.ID.equals(id)) {
					act.setEnabled(false);
				}
				mgr.add(act);
				act = new OpenTodayViewAction(win, "Today", counter, Images.CTXMENU_RDC, objHash, objName, objType, serverId);
				if (CounterRealDateView.ID.equals(id)) {
					act.setEnabled(false);
				}
				mgr.add(act);
				mgr.add(new Separator());
				act = new OpenPastTimeViewAction(win, "Past", Images.CTXMENU_RTC, objHash, objType, null, objName, counter, serverId);
				if (CounterLoadTimeView.ID.equals(id)) {
					act.setEnabled(false);
				}
				mgr.add(act);
				act = new OpenPastDateViewAction(win, "Daily", Images.CTXMENU_RDC, objHash, objType, null, objName, counter, serverId);
				if (CounterLoadDateView.ID.equals(id)) {
					act.setEnabled(false);
				}
				mgr.add(act);
			}
		});
		Menu menu = mgr.createContextMenu(control); 
		control.setMenu(menu); 
	}
	
	public static void addObjTypeSpecialMenu(IWorkbenchWindow win, IMenuManager mgr, int serverId, String objType, CounterEngine counterEngine) {
		if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_JAVAEE)) {
			mgr.add(new Separator());
			mgr.add(new OpenRTPairAllAction(win, "Heap Memory", serverId, objType, CounterConstants.JAVA_HEAP_TOT_USAGE));
			mgr.add(new OpenEQViewAction(win, serverId, objType));
			mgr.add(new OpenVerticalEQViewAction(win, serverId, objType));
			mgr.add(new OpenActiveServiceListAction(win, objType, Images.thread, serverId));
			mgr.add(new OpenActiveSpeedAction(win,objType, Images.TYPE_ACTSPEED, serverId));
			mgr.add(new OpenXLogRealTimeAction(win, MenuStr.XLOG, objType, Images.star, serverId));
			mgr.add(new OpenTodayServiceCountAction(win, MenuStr.SERVICE_COUNT, objType, CounterConstants.WAS_SERVICE_COUNT, Images.bar, serverId));
			MenuManager serviceGroupMgr = new MenuManager("Serivce Group", ImageUtil.getImageDescriptor(Images.sum), "scouter.menu.id.javee.servicegroup");
			mgr.add(serviceGroupMgr);
			serviceGroupMgr.add(new OpenServiceGroupTPSAction(win, serverId, objType));
			serviceGroupMgr.add(new OpenServiceGroupElapsedAction(win, serverId, objType));
			mgr.add(new OpenUniqueTotalVisitorAction(win, serverId, objType));
			mgr.add(new OpenTypeSummaryAction(win, serverId, objType));
			mgr.add(new OpenRTPairAllAction(win, "File Descriptor", serverId, objType, CounterConstants.JAVA_FD_USAGE));
		} else if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_DATASOURCE)) {
			mgr.add(new Separator());
			mgr.add(new OpenRTPairAllAction2(win, "Pool Chart", serverId, objType, CounterConstants.DATASOURCE_CONN_MAX, CounterConstants.DATASOURCE_CONN_ACTIVE));
		}
	}
	
	public static void addPastObjTypeSpecialMenu(IWorkbenchWindow win, IMenuManager mgr, int serverId, String objType, CounterEngine counterEngine, String date) {
		long st = DateUtil.yyyymmdd(date);
		long et = st + DateUtil.MILLIS_PER_FIVE_MINUTE;
		if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_JAVAEE)) {
			mgr.add(new Separator());
			mgr.add(new OpenXLogLoadTimeAction(win, objType, Images.transrealtime, serverId, st, et));
			mgr.add(new OpenDailyServiceCountAction(win, objType, CounterConstants.WAS_SERVICE_COUNT, Images.TYPE_SERVICE_COUNT, serverId, date));
		}
	}
}
