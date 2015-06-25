/*
 *  Copyright 2015 LG CNS.
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

import scouter.client.Images;
import scouter.client.actions.OpenActiveServiceListAction;
import scouter.client.actions.OpenActiveSpeedAction;
import scouter.client.actions.OpenEQViewAction;
import scouter.client.actions.OpenServiceGroupAction;
import scouter.client.actions.SetColorAction;
import scouter.client.configuration.actions.DefineObjectTypeAction;
import scouter.client.constants.MenuStr;
import scouter.client.context.actions.OpenAPIDebugViewAction;
import scouter.client.context.actions.OpenCxtmenuActiveServiceListAction;
import scouter.client.context.actions.OpenCxtmenuConfigureAgentViewAction;
import scouter.client.context.actions.OpenCxtmenuCounterLoadDateViewAction;
import scouter.client.context.actions.OpenCxtmenuCounterLoadTimeViewAction;
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
import scouter.client.counter.actions.OpenObjectRealTimeViewAction;
import scouter.client.counter.actions.OpenObjectTodayViewAction;
import scouter.client.counter.actions.OpenPastLongDateAllAction;
import scouter.client.counter.actions.OpenPastLongDateTotalAction;
import scouter.client.counter.actions.OpenPastTimeAllAction;
import scouter.client.counter.actions.OpenPastTimeTotalAction;
import scouter.client.counter.actions.OpenRealTimeAllAction;
import scouter.client.counter.actions.OpenRealTimeTotalAction;
import scouter.client.counter.actions.OpenTodayAllAction;
import scouter.client.counter.actions.OpenTodayServiceCountAction;
import scouter.client.counter.actions.OpenTodayTotalAction;
import scouter.client.heapdump.actions.HeapDumpAction;
import scouter.client.heapdump.actions.HeapDumpListAction;
import scouter.client.host.actions.OpenDiskUsageAction;
import scouter.client.host.actions.OpenMemInfoAction;
import scouter.client.host.actions.OpenNetStatAction;
import scouter.client.host.actions.OpenTopAction;
import scouter.client.host.actions.OpenWhoAction;
import scouter.client.model.AgentObject;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.xlog.actions.OpenXLogLoadTimeAction;
import scouter.client.xlog.actions.OpenXLogRealTimeAction;
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
					objType + ":" + counterName + ":" + CounterConstants.REAL_TIME_ALL,
					new OpenRealTimeAllAction(window, label, objType, counterName, Images
							.getCounterImage(objType, counterName, serverId), serverId));
		}
		
		objTypeAndCounter = counterEngine.getAllCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.TODAY_ALL,
					new OpenTodayAllAction(window, label, objType, counterName,
							Images.getCounterImage(objType, counterName, serverId), serverId));
		}
		
		objTypeAndCounter = counterEngine.getTotalCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.REAL_TIME_TOTAL,
					new OpenRealTimeTotalAction(window, label, objType, counterName,
							Images.getCounterImage(objType, counterName, serverId), serverId));
		}
		objTypeAndCounter = counterEngine.getTotalCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.TODAY_TOTAL,
					new OpenTodayTotalAction(window, label, objType, counterName,
							Images.getCounterImage(objType, counterName, serverId), serverId));
		}

		objTypeAndCounter = counterEngine.getAllCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_TIME_ALL,
					new OpenPastTimeAllAction(window, label, objType, counterName,
							Images.getCounterImage(objType, counterName, serverId), -1, -1, serverId));
		}
		
		objTypeAndCounter = counterEngine.getAllCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_DATE_ALL,
					new OpenPastLongDateAllAction(window, label, objType, counterName,
							Images.getCounterImage(objType, counterName, serverId), null, null, serverId));
		}
		
		objTypeAndCounter =  counterEngine.getTotalCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_TIME_TOTAL,
					new OpenPastTimeTotalAction(window, label, objType,
							counterName, Images.getCounterImage(objType, counterName, serverId), -1, -1, serverId));
		}
		
		objTypeAndCounter =  counterEngine.getTotalCounterList();
		for (int inx = 0; inx < objTypeAndCounter.size(); inx++) {
			String[] splitedKey = objTypeAndCounter.get(inx).split(":");
			String objType = splitedKey[0];
			String label = splitedKey[1];
			String counterName = splitedKey[2];
			actions.put(objType + ":" + counterName + ":" + CounterConstants.PAST_DATE_TOTAL,
					new OpenPastLongDateTotalAction(window, label, objType,
							counterName, Images.getCounterImage(objType, counterName, serverId), null, null, serverId));
		}
		
		ArrayList<String> objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.TOTAL_ACTIVE_SPEED);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(objType + ":" + CounterConstants.TOTAL_ACTIVE_SPEED, new OpenActiveSpeedAction(window,
					objTypeDisplay, objType, Images.getObjectIcon(objType, true, serverId), serverId));
		}
		
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.ACTIVE_EQ);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objType = splitedKey[1];
			actions.put(objType + ":" + CounterConstants.ACTIVE_EQ, new OpenEQViewAction(window, serverId, objType));
		}
		
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.ACTIVE_THREAD_LIST);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(objType + ":" + CounterConstants.ACTIVE_THREAD_LIST, new OpenActiveServiceListAction(window,
					objTypeDisplay, objType, Images.getObjectIcon(objType, true, serverId), serverId));
		}
		
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.TRANX_REALTIME);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(
					objType + ":" + CounterConstants.TRANX_REALTIME,
					new OpenXLogRealTimeAction(window, objTypeDisplay, objType, Images.getObjectIcon(
							objType, true, serverId), serverId));
		}
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.TRANX_LOADTIME);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(
					objType + ":" + CounterConstants.TRANX_LOADTIME,
					new OpenXLogLoadTimeAction(window, objTypeDisplay, objType, Images.getObjectIcon(
							objType, true, serverId), serverId));
		}
		
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.TODAY_SERVICE_COUNT);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(
					objType + ":" + CounterConstants.TODAY_SERVICE_COUNT,
					new OpenTodayServiceCountAction(window, objTypeDisplay, objType, CounterConstants.WAS_SERVICE_COUNT, Images.getObjectIcon(
							objType, true, serverId), serverId));
		}
		
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.DAILY_SERVICE_COUNT);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(
					objType + ":" + CounterConstants.DAILY_SERVICE_COUNT,
					new OpenDailyServiceCountAction(window, objTypeDisplay, objType, CounterConstants.WAS_SERVICE_COUNT, Images.getObjectIcon(
							objType, true, serverId), serverId));
		}
		
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.SERVICE_GROUP);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objType = splitedKey[1];
			actions.put(
					objType + ":" + CounterConstants.SERVICE_GROUP,
					new OpenServiceGroupAction(window, serverId, objType));
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
		
		ArrayList<String> objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.TRANX_LOADTIME);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(
					objType + ":" + CounterConstants.TRANX_LOADTIME,
					new OpenXLogLoadTimeAction(window, objTypeDisplay, objType, Images.getObjectIcon(
							objType, true, serverId), serverId, st, et));
		}
		
		objTypeList = counterEngine.getObjTypeListWithDisplay(CounterConstants.DAILY_SERVICE_COUNT);
		for (int inx = 0; inx < objTypeList.size(); inx++) {
			String[] splitedKey = objTypeList.get(inx).split(":");
			String objTypeDisplay = splitedKey[0];
			String objType = splitedKey[1];
			actions.put(
					objType + ":" + CounterConstants.DAILY_SERVICE_COUNT,
					new OpenDailyServiceCountAction(window, objTypeDisplay, objType, CounterConstants.WAS_SERVICE_COUNT, Images.getObjectIcon(
							objType, true, serverId), serverId, curdate));
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
    	
    	if (counterNames != null) {
	    	for(int inx = 0 ; inx < counterNames.length ; inx++){
	    		String counter = counterNames[inx];
	    		String counterDisplay = counterEngine.getCounterDisplayName(objType, counter);
	    		MenuManager counterMenu = new MenuManager(counterDisplay, Images.getCounterImageDescriptor(objType, counter, serverId), "scouter."+objType+"."+counter);
				performanceCounter.add(counterMenu);
				MenuManager liveMenuManager = new MenuManager(MenuStr.LIVE_CHART
						, ImageUtil.getImageDescriptor(Images.monitor)
						, "scouter.menu.live.id."+objType+"."+counter);
				counterMenu.add(liveMenuManager);
				MenuManager loadMenuManager = new MenuManager(MenuStr.LOAD_CHART
						, ImageUtil.getImageDescriptor(Images.drive)
						, "scouter.menu.load.id."+objType+"."+counter);
				counterMenu.add(loadMenuManager);
				liveMenuManager.add(new OpenObjectRealTimeViewAction(win, MenuStr.TIME_COUNTER, counter, Images.CTXMENU_RTC, objHash, objName, objType, serverId));
				liveMenuManager.add(new OpenObjectTodayViewAction(win, MenuStr.DAILY_COUNTER, counter, Images.CTXMENU_RDC, objHash, objName, objType, serverId));
				loadMenuManager.add(new OpenCxtmenuCounterLoadTimeViewAction(win, MenuStr.TIME_COUNTER, Images.CTXMENU_RTC, objHash, objType, null, objName, counter, serverId));
				loadMenuManager.add(new OpenCxtmenuCounterLoadDateViewAction(win, MenuStr.DAILY_COUNTER, Images.CTXMENU_RDC, objHash, objType, null, objName, counter, serverId));
			}
    	}
    	
    	if (object.isAlive()) {
	    	MenuManager performanceSnapshot = new MenuManager(MenuStr.PERFORMANCE_STATUS, Images.CAPTURE, MenuStr.PERFORMANCE_STATUS_ID);
	    	mgr.add(performanceSnapshot);
	    	
	    	
			boolean javaee = counterEngine.isChildOf(objType, CounterConstants.FAMILY_JAVAEE);
			if (javaee) {
				performanceSnapshot.add(new OpenCxtmenuThreadListAction(win, MenuStr.THREAD_LIST, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuActiveServiceListAction(win, MenuStr.ACTIVE_SERVICE_LIST, objHash, objType, serverId));
				performanceSnapshot.add(new OpenCxtmenuObjectClassListAction(win, MenuStr.LOADED_CLASS_LIST, objHash, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_HEAPHISTOGRAM))
					performanceSnapshot.add(new OpenCxtmenuHeapHistoViewAction(win, MenuStr.HEAP_HISTOGRAM, objHash, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_THREADDUMP))
					performanceSnapshot.add(new OpenCxtmenuObjectThreadDumpAction(win, MenuStr.THREAD_DUMP, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuEnvAction(win, MenuStr.ENV, objHash, serverId));
				performanceSnapshot.add(new OpenCxtmenuFileSocketAction(win, MenuStr.FILE_SOCKET, objHash, serverId));
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
			} else if (counterEngine.isChildOf(objType, CounterConstants.FAMILY_HOST)) {
				performanceSnapshot.add(new OpenCxtmenuEnvAction(win, MenuStr.ENV, objHash, serverId));
				performanceSnapshot.add(new OpenTopAction(win, MenuStr.TOP, objHash, serverId));
				performanceSnapshot.add(new OpenDiskUsageAction(win, MenuStr.DISK_USAGE, objHash, serverId));
				performanceSnapshot.add(new OpenNetStatAction(win, MenuStr.NET_STAT, objHash, serverId));
				performanceSnapshot.add(new OpenWhoAction(win, MenuStr.WHO, objHash, serverId));
				performanceSnapshot.add(new OpenMemInfoAction(win, MenuStr.MEM_INFO, objHash, serverId));
			} 
    	}
    	mgr.add(new Separator());
		if (server.isAllowAction(GroupPolicyConstants.ALLOW_CONFIGURE))
			mgr.add(new OpenCxtmenuConfigureAgentViewAction(win, MenuStr.CONFIGURE, objHash, serverId));
    	if (server.isAllowAction(GroupPolicyConstants.ALLOW_DEFINEOBJTYPE)) {
	    	if (counterEngine.isUnknownObjectType(objType)) {
	    		mgr.add(new DefineObjectTypeAction(win, serverId, objType, DefineObjectTypeAction.DEFINE_MODE));
	    	} else {
	    		mgr.add(new DefineObjectTypeAction(win, serverId, objType, DefineObjectTypeAction.EDIT_MODE));
	    	}
			mgr.add(new Separator());
    	}
		
		mgr.add(new SetColorAction(win, objHash));
		mgr.add(new Separator());
		mgr.add(new OpenCxtmenuPropertiesAction(win, MenuStr.PROPERTIES, objHash, serverId));
		if (false) {
			mgr.add(new Separator());
			mgr.add(new OpenAPIDebugViewAction(win, objHash, serverId));
		}
	}
}
