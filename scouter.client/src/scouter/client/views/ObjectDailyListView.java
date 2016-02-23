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
package scouter.client.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
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
import scouter.client.actions.OpenObjectDailyListAction;
import scouter.client.constants.MenuStr;
import scouter.client.context.actions.OpenCxtmenuPropertiesAction;
import scouter.client.counter.actions.OpenPastDateViewAction;
import scouter.client.counter.actions.OpenPastTimeViewAction;
import scouter.client.model.AgentDailyListProxy;
import scouter.client.model.AgentObject;
import scouter.client.model.DummyObject;
import scouter.client.model.HierarchyObject;
import scouter.client.model.ServerObject;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.MenuUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.counters.CounterConstants;
import scouter.lang.counters.CounterEngine;

public class ObjectDailyListView extends ViewPart {
	public static final String ID = ObjectDailyListView.class.getName();

	public TreeViewer viewer;
	
	private String curdate;
	private Tree addressTree;
	AgentDailyListProxy proxy = new AgentDailyListProxy();
	private Map<String, ServerObject> root = new HashMap<String, ServerObject>();
	
	int serverId;
	
	boolean showMenu = false;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(ChartUtil.gridlayout(1));
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite area2 = new Composite(parent, SWT.NONE);
		area2.setLayoutData(new GridData(GridData.FILL_BOTH));
		area2.setLayout(new FillLayout());

		addressTree = new Tree(area2, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		addressTree.setHeaderVisible(true);
		
		viewer = new TreeViewer(addressTree);
		
		TreeColumn column1 = new TreeColumn(addressTree, SWT.LEFT);
		addressTree.setLinesVisible(true);
		column1.setAlignment(SWT.LEFT);
		column1.setText("Instance");
		column1.setWidth(220);

		TreeColumnLayout layout = new TreeColumnLayout();
		area2.setLayout( layout );
		
		layout.setColumnData( column1, new ColumnWeightData( 90 ) );
		
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new TreeLabelProvider());
		viewer.setInput(root);

		createContextMenu(viewer, new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager){
            	if(showMenu){
            		fillTreeViewerContextMenu(manager);
            		showMenu = false;
            	}/*else{
            		fillAddviewContextMenu(manager);
            	}*/
            }
        });
		
		// RIGHT CLICK HANDLING
		viewer.getTree().addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				TreeItem item = viewer.getTree().getItem(point);
				if (item != null) {
					showMenu = true;
				}
			}
		});
		
		// DOUBLE CLICK HANDLING
		viewer.getTree().addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				TreeItem item = viewer.getTree().getItem(point);
				if (item != null) {
					if(item.getExpanded()){
						item.setExpanded(false);
					}else{
						item.setExpanded(true);
					}
				}
			}
		});
		
		createQuickMenus();
		
	}
	
	public void setInput(String date, int serverId){
		this.serverId = serverId;
		setDate(date) ;
	}
	
	private void createQuickMenus(){
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Expand all", ImageUtil.getImageDescriptor(Images.expand)) {
			public void run() {
				viewer.expandAll();
			}
		});
		man.add(new Action("Collapse All", ImageUtil.getImageDescriptor(Images.collapse)) {
			public void run() {
				viewer.collapseAll();
			}
		});
		man.add(new Action("Date", ImageUtil.getImageDescriptor(Images.calendar)) {
			public void run() {
				new OpenObjectDailyListAction(getSite().getWorkbenchWindow(), "Date", Images.calendar, serverId).run();
			}
		});
	}
	
	private void createContextMenu(Viewer viewer, IMenuListener listener){
        MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(listener);
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(contextMenu, viewer);
    }
	
	private void fillTreeViewerContextMenu(IMenuManager mgr){
		
		ISelection selection = viewer.getSelection();
		
		if (selection instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection)selection;
            Object[] elements = sel.toArray();
            if (elements == null || elements.length < 1) {
            	return;
            }
            Object selObject = elements[elements.length - 1];
            
            IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            
            // CREATE CONTEXT MENU - Agent Object
            if (selObject instanceof AgentObject) {
            	
            	int serverId = ((AgentObject) selObject).getServerId();
            	String objType = ((AgentObject) selObject).getObjType();
            	int objHash = ((AgentObject) selObject).getObjHash();
            	String objName = ((AgentObject) selObject).getObjName();
            	Server server = ServerManager.getInstance().getServer(serverId);  
            	CounterEngine counterEngine = server.getCounterEngine();
            	String[] counterNames = counterEngine.getSortedCounterName(objType);
            	for(int inx = 0 ; inx < counterNames.length ; inx++){
            		String counter = counterNames[inx];
            		String counterDisplay = counterEngine.getCounterDisplayName(objType, counter);
            		MenuManager counterMenu = new MenuManager(counterDisplay, Images.getCounterImageDescriptor(objType, counter, serverId), "scouter."+objType+"."+counter);
        			mgr.add(counterMenu);
        			counterMenu.add(new OpenPastTimeViewAction(win, MenuStr.TIME_COUNTER, Images.CTXMENU_RTC, objHash, objType, curdate, objName, counter, serverId));
        			counterMenu.add(new OpenPastDateViewAction(win, MenuStr.DAILY_COUNTER, Images.CTXMENU_RDC, objHash, objType, curdate, objName, counter, serverId));
        		}
            	mgr.add(new Separator());
				mgr.add(new OpenCxtmenuPropertiesAction(win, MenuStr.PROPERTIES, objHash, serverId, this.curdate));
            } else if (selObject instanceof ServerObject) {
            	
            	int serverId = ((ServerObject) selObject).getId();
//				String serverName = ((ServerObject) selObject).getName();
				Server server = ServerManager.getInstance().getServer(serverId);
				CounterEngine counterEngine = server.getCounterEngine();
				
				HashMap<String, Action> actions = MenuUtil.getPastCounterActionList(win, counterEngine, curdate, serverId);
				
				if(actions == null)
					return;
            	
				for(String objType : objTypeList){
					String objTypeDisplay = counterEngine.getDisplayNameObjectType(objType);
					ImageDescriptor objImage = Images.getObjectImageDescriptor(objType, true, serverId);
					MenuManager objTitle = new MenuManager(objTypeDisplay, objImage, "scouter.menu.id.load."+objTypeDisplay);
					mgr.add(objTitle);
					
					ArrayList<String> counters = counterEngine.getAllCounterWithDisplay(objType);
					for(int i = 0 ; counters != null && i < counters.size() ; i++){
						String[] cnt = counters.get(i).split(":");
						String display = cnt[0];
						String counter = cnt[1];
						MenuManager counterMenuManager = new MenuManager(display
								, ImageUtil.getImageDescriptor(Images.getCounterImage(objType, counter, serverId))
								, "scouter.menu.id.load."+objType+"."+counter);
						for(String menu : CounterConstants.COUNTER_MENU_ARRAY){
							if (ScouterUtil.isLiveMenu(menu)) {
								continue;
							}
							Action act = actions.get(objType + ":" + counter + ":" + menu);
							if(act != null){
								act.setText(ScouterUtil.getActionName(menu));
								act.setImageDescriptor(ScouterUtil.getActionIconName(menu));
								counterMenuManager.add(act);
							}
						}
						objTitle.add(counterMenuManager);
					}
					
					MenuUtil.addPastObjTypeSpecialMenu(win, objTitle, serverId, objType, counterEngine, curdate);
				}
            }
		}
	}
	
	
	private void setDate(String date) {
		curdate = date;
		setContentDescription(date.substring(0,4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8));
		makeHierarchyMap();
		viewer.refresh();
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	Set<String> objTypeList = new HashSet<String>();
	
	private synchronized void makeHierarchyMap() {
		Map<String, ServerObject> tempRootMap = new HashMap<String, ServerObject>();
		objTypeList.clear();
		ArrayList<AgentObject> objectList = proxy.getObjectList(curdate, serverId);
		for (AgentObject agent : objectList) {
			int serverId = agent.getServerId();
			objTypeList.add(agent.getObjType());
			String serverName = ServerManager.getInstance().getServer(serverId).getName();
			ServerObject serverObj = tempRootMap.get(serverName);
			if (serverObj == null) {
				serverObj = new ServerObject(serverId, serverName);
				tempRootMap.put(serverName, serverObj);
			}
			String objName = agent.getObjName();
			HierarchyObject parent = serverObj;
			int inx = objName.indexOf("/", 1);
			while (inx != -1) {
				String childName = objName.substring(0, inx);
				HierarchyObject child = parent.getChild(childName);
				if (child == null) {
					child = new DummyObject(childName);
					child.setParent(parent);
					parent.putChild(childName, child);
				}
				parent = child;
				inx = objName.indexOf("/", (inx+1));
			}
			HierarchyObject beforeDummyObj = parent.putChild(objName, agent);
			if (beforeDummyObj != null && beforeDummyObj instanceof DummyObject) {
				agent.setChildMap(((DummyObject)beforeDummyObj).getChildMap());
			}
			agent.setParent(parent);
		}
		root.clear();
		root.putAll(tempRootMap);
	}
	
	class ViewContentProvider implements ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		@SuppressWarnings("rawtypes")
		public Object[] getElements(Object parent) {
			if (parent instanceof Map) {
				return ((Map) parent).values().toArray();
			}
			return new Object[0];
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof HierarchyObject) {
				return ((HierarchyObject) parentElement).getSortedChildArray();
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
			if (element instanceof HierarchyObject) {
				return ((HierarchyObject) element).getChildSize() > 0;
			}
			return false;
		}

		public boolean equals(Object obj) {
			return true;
		}

	}

	public class TreeLabelProvider extends LabelProvider {

		public Image getImage(Object element) {
			if (element instanceof AgentObject) {
				AgentObject a = (AgentObject) element;
				return Images.getObjectIcon(a.getObjType(), true, serverId);
			} else if (element instanceof ServerObject) {
				return Images.SERVER_ACT;
			} else if (element instanceof DummyObject) {
				DummyObject dummyObj = (DummyObject) element;
				if (dummyObj.getChildSize() > 0) {
					return Images.folder;
				} else {
					return Images.default_context;
				}
			}
			return Images.default_context;
		}

		public String getText(Object element) {
			if (element instanceof AgentObject) {
				AgentObject a = (AgentObject) element;
				return a.getDisplayName();
			} else if (element instanceof ServerObject) {
				ServerObject a = (ServerObject) element;
				return a.getName();
			} else if (element instanceof DummyObject) {
				DummyObject a = (DummyObject) element;
				return a.getName();
			}
			return element.toString();
		}
	}
	

}