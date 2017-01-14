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
package scouter.client.group.view;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.actions.OpenAddGroupAction;
import scouter.client.actions.OpenEQGroupViewAction;
import scouter.client.actions.OpenManageGroupAction;
import scouter.client.actions.OpenServiceGroupElapsedGroupAction;
import scouter.client.actions.OpenServiceGroupTPSGroupAction;
import scouter.client.actions.OpenVerticalEQGroupViewAction;
import scouter.client.constants.MenuStr;
import scouter.client.context.actions.OpenCxtmenuAssginGroupAction;
import scouter.client.counter.actions.OpenActiveSpeedGroupViewAction;
import scouter.client.counter.actions.OpenPastDateGroupAllViewAction;
import scouter.client.counter.actions.OpenPastDateGroupCountViewAction;
import scouter.client.counter.actions.OpenPastDateGroupTotalViewAction;
import scouter.client.counter.actions.OpenPastTimeGroupAllViewAction;
import scouter.client.counter.actions.OpenPastTimeGroupTotalViewAction;
import scouter.client.counter.actions.OpenPastTimeTranXGroupViewAction;
import scouter.client.counter.actions.OpenRealTimeGroupAllViewAction;
import scouter.client.counter.actions.OpenRealTimeGroupTotalViewAction;
import scouter.client.counter.actions.OpenRealTimeTranXGroupViewAction;
import scouter.client.counter.actions.OpenTodayGroupAllViewAction;
import scouter.client.counter.actions.OpenTodayGroupCountViewAction;
import scouter.client.counter.actions.OpenTodayGroupTotalViewAction;
import scouter.client.group.GroupManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.DummyObject;
import scouter.client.model.GroupObject;
import scouter.client.model.HierarchyObject;
import scouter.client.model.RefreshThread;
import scouter.client.popup.AddGroupDialog.IAddGroup;
import scouter.client.popup.GroupAssignmentDialog.IGroupAssign;
import scouter.client.popup.ManageGroupDialog.IManageGroup;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.DummyAction;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.MenuUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.Counter;
import scouter.lang.counters.CounterConstants;
import scouter.lang.counters.CounterEngine;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;
import scouter.util.LinkedMap;

public class GroupNavigationView extends ViewPart implements RefreshThread.Refreshable, IAddGroup, IManageGroup, IGroupAssign {
	
	public static final String ID = GroupNavigationView.class.getName();
	
	IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
	private RefreshThread thread;
	private TreeViewer viewer;
	private Tree tree;
	LinkedMap<String, HierarchyObject> groupMap = new LinkedMap<String, HierarchyObject>();
	
	TreeItem selectedItem;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		Composite mainComp = new Composite(parent, SWT.NONE);
		tree = new Tree(mainComp, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		viewer = new TreeViewer(tree);
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setAlignment(SWT.LEFT);
		column1.setText("Group/Object");
		TreeColumn column2 = new TreeColumn(tree, SWT.RIGHT);
		column2.setAlignment(SWT.RIGHT);
		column2.setText("Perf");
		TreeColumnLayout layout = new TreeColumnLayout();
		mainComp.setLayout( layout );
		layout.setColumnData(column1, new ColumnWeightData( 68 ));
		layout.setColumnData(column2, new ColumnWeightData( 22 ));
		viewer.setContentProvider(new TreeContentProvider());
		viewer.setLabelProvider(new TreeLabelProvider());
		viewer.setInput(groupMap);
		createContextMenu(viewer, new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager){
            	if (selectedItem == null) {
            		manager.add(new OpenAddGroupAction(win, GroupNavigationView.this));
            	} else {
            		ISelection selection = viewer.getSelection();
            		if (selection instanceof IStructuredSelection) {
                        IStructuredSelection sel = (IStructuredSelection)selection;
                        Object[] elements = sel.toArray();
                        if (elements == null || elements.length < 1) {
                        	return;
                        }
                        Object selObject = elements[elements.length - 1];
                        if (selObject instanceof GroupObject) {
                        	GroupObject grpObj = (GroupObject) selObject;
                        	String grpName = grpObj.getName();
                        	String objType = grpObj.getObjType();
                        	boolean userGroupObj = !(grpObj.getParent() != null && grpObj.getParent() instanceof DummyObject);
                        	AgentObject defaultObj =  (grpObj.getFirstChild() != null) ? (AgentObject) grpObj.getFirstChild() : null;
                        	Server server = ServerManager.getInstance().getDefaultServer();
                        	if (defaultObj != null) {
                        		server = ServerManager.getInstance().getServer(defaultObj.getServerId());
                        	}
                        	manager.add(new DummyAction(grpName, Images.getObjectIcon(objType, false, server.getId())));
                        	if (userGroupObj) {
	                        	manager.add(new Separator());
	                        	manager.add(new OpenManageGroupAction(win, grpName, objType, GroupNavigationView.this));
                        		manager.add(new RemoveGroupAction(grpName));
                        		manager.add(new Separator());
	                        	Set<Counter> counters = listCounters(objType);
	                        	for (Counter counter : counters) {
	                        		String name = counter.getName();
	                        		String display = counter.getDisplayName();
	                        		MenuManager counterMenu = new MenuManager(display
	                        				, Images.getCounterImageDescriptor(objType, name, server.getId()), "scouter.group."+objType+"."+name);
	                        		manager.add(counterMenu);
	                        		MenuManager liveMenuManager = new MenuManager(MenuStr.LIVE_CHART
	                						, ImageUtil.getImageDescriptor(Images.monitor)
	                						, "scouter.group.live.id."+objType+"."+name);
	                        		counterMenu.add(liveMenuManager);
	                				MenuManager loadMenuManager = new MenuManager(MenuStr.LOAD_CHART
	                						, ImageUtil.getImageDescriptor(Images.drive)
	                						, "scouter.group.load.id."+objType+"."+name);
	                				counterMenu.add(loadMenuManager);
	                        		if (counter.isAll()) {
	                        			liveMenuManager.add(new OpenRealTimeGroupAllViewAction(win, MenuStr.TIME_ALL, name, grpObj));
	                        		}
	                        		if (counter.isTotal()) {
	                        			liveMenuManager.add(new OpenRealTimeGroupTotalViewAction(win, MenuStr.TIME_TOTAL, name, grpObj));
	                        		}
	                        		if (counter.isAll()) {
	                        			liveMenuManager.add(new OpenTodayGroupAllViewAction(win, MenuStr.DAILY_ALL, name, grpObj));
	                        		}
	                        		if (counter.isTotal()) {
	                        			liveMenuManager.add(new OpenTodayGroupTotalViewAction(win, MenuStr.DAILY_TOTAL, name, grpObj));
	                        		}
	                        		if (counter.isAll()) {
	                        			loadMenuManager.add(new OpenPastTimeGroupAllViewAction(win, MenuStr.TIME_ALL, name, grpObj));
	                        		}
	                        		if (counter.isTotal()) {
	                        			loadMenuManager.add(new OpenPastTimeGroupTotalViewAction(win, MenuStr.TIME_TOTAL, name, grpObj));
	                        		}
	                        		if (counter.isAll()) {
	                        			loadMenuManager.add(new OpenPastDateGroupAllViewAction(win, MenuStr.DAILY_ALL, name, grpObj));
	                        		}
	                        		if (counter.isTotal()) {
	                        			loadMenuManager.add(new OpenPastDateGroupTotalViewAction(win, MenuStr.DAILY_TOTAL, name, grpObj));
	                        		}
	                        	}
                        		manager.add(new Separator());
                        		if (isChildOf(objType, CounterConstants.FAMILY_JAVAEE)) {
                        			manager.add(new OpenEQGroupViewAction(win, grpObj.getName()));
                        			manager.add(new OpenVerticalEQGroupViewAction(win, grpObj.getName()));
                        			manager.add(new OpenActiveSpeedGroupViewAction(win, MenuStr.ACTIVE_SPEED_REAL, grpObj));
                        			MenuManager xLogMenu = new MenuManager(MenuStr.XLOG, ImageUtil.getImageDescriptor(Images.transrealtime), MenuStr.XLOG_ID);
                        			manager.add(xLogMenu);
                    				xLogMenu.add(new OpenRealTimeTranXGroupViewAction(win, MenuStr.REALTIME_XLOG, grpObj));
                    				xLogMenu.add(new OpenPastTimeTranXGroupViewAction(win, MenuStr.PASTTIME_XLOG, grpObj));
                    				MenuManager scMenu = new MenuManager(MenuStr.HOURLY_CHART, ImageUtil.getImageDescriptor(Images.bar), MenuStr.HOURLY_CHART_ID);
                    				manager.add(scMenu);
                					scMenu.add(new OpenPastDateGroupCountViewAction(win, MenuStr.LOAD_SERVICE_COUNT, CounterConstants.WAS_SERVICE_COUNT, grpObj));
                					scMenu.add(new OpenTodayGroupCountViewAction(win, MenuStr.TODAY_SERVICE_COUNT, CounterConstants.WAS_SERVICE_COUNT, grpObj));
                					MenuManager serviceGroupMgr = new MenuManager("Serivce Group", ImageUtil.getImageDescriptor(Images.sum), "scouter.menu.id.group.javee.servicegroup");
                					manager.add(serviceGroupMgr);
                					serviceGroupMgr.add(new OpenServiceGroupTPSGroupAction(win, grpName));
                					serviceGroupMgr.add(new OpenServiceGroupElapsedGroupAction(win, grpName));
                        		}
                        	}
                        } else if (selObject instanceof AgentObject) {
                        	AgentObject agent = (AgentObject) selObject;
                        	int objHash = agent.getObjHash();
                        	String objName = agent.getObjName();
                        	String objType = agent.getObjType();
                        	int serverId = agent.getServerId();
                        	manager.add(new DummyAction(ServerManager.getInstance().getServer(serverId).getName()
                        			, Images.SERVER_INACT));
                        	manager.add(new Separator());
                        	manager.add(new OpenCxtmenuAssginGroupAction(win, serverId, objHash, objName, objType, GroupNavigationView.this));
                        	manager.add(new Separator());
                        	MenuUtil.addObjectContextMenu(manager, win, agent);
                        }
            		}
            	}
            }
        });
		tree.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				selectedItem = tree.getItem(point);
			}
		});

		createQuickMenus();
		thread = new RefreshThread(this, 3000);
		thread.setName(ID);
		thread.start();
	}
	
	private void createContextMenu(Viewer viewer, IMenuListener listener){
        MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(listener);
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(contextMenu, viewer);
    }
	
	private void createQuickMenus(){
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Expand All", ImageUtil.getImageDescriptor(Images.expand)) {
			public void run() {
				viewer.expandAll();
			}
		});
		man.add(new Action("Collapse All", ImageUtil.getImageDescriptor(Images.collapse)) {
			public void run() {
				viewer.collapseAll();
			}
		});
		man.add(new Separator());
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				forceRefresh();
			}
		});
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void dispose() {
		super.dispose();
		if(thread != null && thread.isAlive()){
			thread.shutdown();
			thread = null;
		}
	}
	
	private void forceRefresh() {
		AgentModelThread.getInstance().fetchObjectList();
		refresh();
	}
	
	public void refresh() {
		organizeGroup();
		try {
			ExUtil.exec(tree, new Runnable() {
				public void run() {
					viewer.refresh();
				}
			});
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}
	
	private Set<Counter> listCounters(String objType) {
		Set<Counter> counterSet = new TreeSet<Counter>();
		Set<Integer> serverList = ServerManager.getInstance().getOpenServerList();
		for (int serverId : serverList) {
			Server server = ServerManager.getInstance().getServer(serverId);
			CounterEngine engine = server.getCounterEngine();
			counterSet.addAll(engine.getCounterObjectSet(objType));
		}
		return counterSet;
	}
	
	private boolean isActionEnable(String objType, String attrName) {
		boolean result = false;
		Set<Integer> serverList = ServerManager.getInstance().getOpenServerList();
		for (int serverId : serverList) {
			Server server = ServerManager.getInstance().getServer(serverId);
			CounterEngine engine = server.getCounterEngine();
			if (engine.isTrueAction(objType, attrName)) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	private boolean isChildOf(String objType, String family) {
		boolean result = false;
		Set<Integer> serverList = ServerManager.getInstance().getOpenServerList();
		for (int serverId : serverList) {
			Server server = ServerManager.getInstance().getServer(serverId);
			CounterEngine engine = server.getCounterEngine();
			if (engine.isChildOf(objType, family)) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	class TreeContentProvider implements ITreeContentProvider {
		public void dispose() {
			
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof LinkedMap) {
				 Enumeration e = ((LinkedMap) inputElement).values();
				 Object[] objArray = new Object[((LinkedMap) inputElement).size()];
				 int cnt = 0;
				 while (e.hasMoreElements()) {
					 objArray[cnt] = e.nextElement();
					 cnt++;
				 }
				 return objArray;
			}
			return new Object[0];
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof HierarchyObject){
				if (parentElement instanceof AgentObject) {
					return new Object[0];
				} else {
					return ((HierarchyObject) parentElement).getSortedChildArray();
				}
				
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof HierarchyObject) {
				return ((HierarchyObject) element).getParent();
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof HierarchyObject){
				if(element instanceof AgentObject) {
					return false;
				} else {
					return ((HierarchyObject) element).getChildSize() > 0;
				}
			}
			return false;
		}
	}
	
	class TreeLabelProvider implements ITableLabelProvider, IColorProvider  {
		public void addListener(ILabelProviderListener listener) {
			
		}
		public void dispose() {
			
		}
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
			
		}
		public Color getForeground(Object element) {
			if (element instanceof AgentObject) {
				AgentObject a = (AgentObject) element;
				if(!a.isAlive()){
					return ColorUtil.getInstance().getColor("gray");
				}
			}
			return null;
		}
		public Color getBackground(Object element) {
			return null;
		}
		public Image getColumnImage(Object element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (element instanceof DummyObject) {
					DummyObject dummyObj = (DummyObject) element;
					if (dummyObj.getChildSize() > 0) {
						return Images.folder;
					} else {
						return Images.default_context;
					}
				} else if (element instanceof GroupObject) {
					GroupObject grpObj = (GroupObject) element;
					return Images.getObjectIcon(grpObj.getObjType(), true, 0);
				} else if (element instanceof AgentObject) {
					AgentObject agent = (AgentObject) element;
					if (agent.isAlive()) {
						return Images.active;
					} else {
						return Images.dead;
					}
				}
				break;
			case 1:
//				if (element instanceof AgentObject) {
//					AgentObject a = (AgentObject) element;
//					if (a.isAlive()) {
//						return ImageCache.getInstance().getObjectImage(a.getObjHash());
//					}
//				}
//				break;
			}
			return null;
		}
		public String getColumnText(Object obj, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (obj instanceof AgentObject) {
					AgentObject agent = (AgentObject) obj;
					return agent.getObjName();
				} else if (obj instanceof DummyObject) {
					return ((DummyObject) obj).getName();
				} else if (obj instanceof GroupObject) {
					return ((GroupObject) obj).getName();
				}
				return obj.toString();
			case 1:
				if (obj instanceof AgentObject) {
					AgentObject agent = (AgentObject) obj;
					if(agent.isAlive()) {
						Value value = agent.getMasterCounter();
						if (value == null) {
							return "";
						}
						Server server = ServerManager.getInstance().getServer(agent.getServerId());
						if(server == null)
							return "?";
						return getColumnTextForByte(value, server.getCounterEngine().getMasterCounterUnit(agent.getObjType()));
					}
				}
			}
			return null;
		}
		
		public String getColumnTextForByte(Value value, String unit){
			if("bytes".equals(unit)){
				double v = CastUtil.cdouble(value);
				return ScouterUtil.humanReadableByteCount(v, true);
//				if(v > 1024*1024){
//					return new Format(v/(1024*1024)).print("#,##0.0") + " MB";
//				}else if(v > 1024){
//					return new Format(v/(1024)).print("#,##0.0") + " KB";
//				}
			}
			return FormatUtil.print(value, "#,###.##") + " " + unit;
		}
	}

	public boolean addedGroup(String objType, String name) {
		boolean result = GroupManager.getInstance().addGroup(objType, name);
		if (result) {
			forceRefresh();
		}
		return result;
	}

	public void setResult(String groupName, int[] addObjHashs, 	int[] removeObjHashs) {
		GroupManager manager = GroupManager.getInstance();
		if (manager.addObject(addObjHashs, groupName)) {
			manager.removeObject(removeObjHashs, groupName);
			forceRefresh();
		}
	}
	
	public class RemoveGroupAction extends Action {
		String groupName;

		public RemoveGroupAction(String groupName) {
			this.groupName = groupName;
			setText("&Remove");
			setImageDescriptor(ImageUtil.getImageDescriptor(Images.group_delete));
		}

		public void run() {
			if (win != null) {
				if (MessageDialog.openConfirm(win.getShell(), "Remove " + groupName, groupName + " group will be removed. Continue?")) {
					GroupManager.getInstance().removeGroup(groupName);
					forceRefresh();
				}
			}
		}
	}

	public void endGroupAssignment(int objHash, String[] groups) {
		GroupManager.getInstance().assginGroups(objHash, groups);
		forceRefresh();
	}
	
	private void organizeGroup() {
		GroupManager manager = GroupManager.getInstance();
		List<String> userGroupList = manager.listGroup();
		groupMap.clear();
		// 1. Make Groups
		for (int i = 0; i < userGroupList.size(); i++) {
			String name = userGroupList.get(i);
			String objType = manager.getGroupObjType(name);
			if (objType != null) {
				groupMap.put(name, new GroupObject(objType, name));
			}
		}
		
		DummyObject othersObj = new DummyObject(GroupManager.OTHERS); 
		// 2. Make Others
		groupMap.put(GroupManager.OTHERS, othersObj);
					
		// 3. Put agent in group
		AgentObject[] objectList = AgentModelThread.getInstance().getObjectList();
		for (AgentObject agent : objectList) {
			boolean others = true;
			Set<String> objGroupList = manager.getObjGroups(agent.getObjHash());
			for (String value : objGroupList) {
				HierarchyObject hiObj = groupMap.get(value);
				if (hiObj != null && hiObj instanceof GroupObject) {
					GroupObject grpObj = (GroupObject) hiObj;
					if (grpObj.getObjType().equals(agent.getObjType())) {
						grpObj.putChild(agent.getObjName(), agent);
						others = false;
					}
				}
			}
			
			// if any group do not include this object, put in others group
			if (others) {
				String objType = agent.getObjType();
				HierarchyObject hiObj = othersObj.getChild(objType);
				if (hiObj == null) {
					hiObj = new GroupObject(objType, ServerManager.getInstance().getServer(agent.getServerId()).getCounterEngine().getDisplayNameObjectType(objType));
					othersObj.putChild(objType, hiObj);
					hiObj.setParent(othersObj);
				}
				hiObj.putChild(agent.getObjName(), agent);
			}
		}
	}
}
