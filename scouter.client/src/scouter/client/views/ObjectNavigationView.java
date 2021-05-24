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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.actions.AddServerAction;
import scouter.client.actions.ClearObjectFilterAction;
import scouter.client.actions.OpenAlertDetailListAction;
import scouter.client.actions.OpenGroupNavigationAction;
import scouter.client.actions.OpenLoginListAction;
import scouter.client.actions.OpenObjectDailyListAction;
import scouter.client.actions.OpenServerFileManagementAction;
import scouter.client.actions.OpenServerLogsAction;
import scouter.client.actions.OpenServerManagerAction;
import scouter.client.actions.OpenServerThreadListAction;
import scouter.client.configuration.actions.AddAccountAction;
import scouter.client.configuration.actions.EditAccountAction;
import scouter.client.configuration.actions.ListAccountAction;
import scouter.client.configuration.actions.OpenAlertScriptingAction;
import scouter.client.configuration.actions.OpenCountersSiteFileConfigureAction;
import scouter.client.configuration.actions.OpenGroupPolicyAction;
import scouter.client.configuration.actions.OpenServerConfigureAction;
import scouter.client.configuration.actions.OpenTelegrafConfigureAction;
import scouter.client.configuration.actions.OpenTelegrafFileConfigureAction;
import scouter.client.constants.MenuStr;
import scouter.client.context.actions.CloseServerAction;
import scouter.client.context.actions.OpenAPIDebugViewAction;
import scouter.client.context.actions.OpenCxtmenuEnvAction;
import scouter.client.context.actions.OpenCxtmenuServerPropertiesAction;
import scouter.client.context.actions.OpenServerAction;
import scouter.client.context.actions.RemoveServerAction;
import scouter.client.context.actions.SetDefaultServerAction;
import scouter.client.counter.actions.OpenTotalSummaryAction;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.DummyObject;
import scouter.client.model.GroupObject;
import scouter.client.model.HierarchyObject;
import scouter.client.model.RefreshThread;
import scouter.client.model.ServerObject;
import scouter.client.net.LoginMgr;
import scouter.client.popup.ObjectSelectionDialog;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.tags.actions.OpenTagCountViewAction;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.ColorUtil;
import scouter.client.util.DummyAction;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.MenuUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.Counter;
import scouter.lang.Family;
import scouter.lang.ObjectType;
import scouter.lang.counters.CounterConstants;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;
import scouter.util.HashUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ObjectNavigationView extends ViewPart implements RefreshThread.Refreshable {
	public static final String ID = ObjectNavigationView.class.getName();

	enum PresentMode { HIERACHY_MODE, FLAT_MODE }
	
	private RefreshThread thread;
	public TreeViewer objTreeViewer;

	Tree agentTree;
	
	Composite parent;
	Composite noticeComposite;
	CLabel noticeLabel;

	boolean selectedItem = false;
	private Map<String, ServerObject> root = new TreeMap<String, ServerObject>();
	private AgentModelThread agentThread = AgentModelThread.getInstance();
	private ObjectSelectManager objSelMgr = ObjectSelectManager.getInstance();
	
	PresentMode mode = PresentMode.HIERACHY_MODE;
	boolean prevExistUnknown = false;
	
	MenuManager topMenuManager;
	IMenuManager collectorMenuManager;
	IMenuManager hostMenuManager;
	IMenuManager objectMenuManager;
	
	public void refresh() {
		if (mode == PresentMode.HIERACHY_MODE) {
			makeHierarchyMap();
		} else if (mode == PresentMode.FLAT_MODE) {
			makeFlatMap();
		}
		
		Integer[] actionSet = counterActions.keySet().toArray(new Integer[counterActions.size()]);
		Set<Integer> existServerSet = agentThread.existServerSet();
		for (int serverId : actionSet) {
			Server server = ServerManager.getInstance().getServer(serverId);
			if (existServerSet.contains(serverId) == false || server == null) {
				removeActionCache(serverId);
			} else if (server.isDirty()) {
				server.setDirty(false);
				removeActionCache(serverId);
			}
		}
		
		ExUtil.exec(agentTree, new Runnable() {
			public void run() {
				refreshViewer();
				GridData griddata = (GridData) noticeComposite.getLayoutData();
				boolean existUnknown = agentThread.existUnknownType();
				if (prevExistUnknown != existUnknown) {
					if (existUnknown) {
						griddata.exclude = false;
						noticeLabel.setImage(Images.exclamation);
						noticeLabel.setText("Unknown monitoring group id(object type) detected.\nClick for Re-Check.");
						noticeComposite.setVisible(true);
						parent.layout(false);
					} else {
						griddata.exclude = true;
						noticeComposite.setVisible(false);
						parent.layout(false);
					}
					prevExistUnknown = existUnknown;
				}
			}
		});
	}
	
	private void forceRefresh() {
		agentThread.fetchObjectList();
		refreshCounterEngine();
		refresh();
	}

	private void refreshCounterEngine() {
		Set<Integer> serverIdSet = ServerManager.getInstance().getOpenServerList();
		if (serverIdSet.size() > 0) {
			for (int serverId : serverIdSet) {
				CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
				MapPack m = LoginMgr.getCounterXmlServer(serverId);
				if (m != null) {
					counterEngine.clear();
					Value v1 = m.get("default");
					counterEngine.parse(((BlobValue)v1).value);
					v1 = m.get("custom");
					if (v1 != null) {
						counterEngine.parse(((BlobValue)v1).value);
					}
				}
			}
		}
	}
	
	public void refreshViewer() {
		objTreeViewer.refresh();
	}
	
	public void createPartControl(Composite parent) {
		this.parent = parent;
		GridLayout gridlayout = new GridLayout(1, false);
		gridlayout.marginHeight = 0;
		gridlayout.horizontalSpacing = 0;
		gridlayout.marginWidth = 0;
		parent.setLayout(gridlayout);
		noticeComposite = new Composite(parent, SWT.NONE);
		GridData griddata = new GridData(SWT.FILL, SWT.FILL, true, false);
		griddata.exclude = true;
		noticeComposite.setLayoutData(griddata);
		noticeComposite.setLayout(new FillLayout());
		noticeLabel = new CLabel(noticeComposite, SWT.MULTI | SWT.WRAP);
		noticeComposite.setVisible(false);
		
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		objTreeViewer = new TreeViewer(mainComposite, SWT.BORDER | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		agentTree = objTreeViewer.getTree();
		
		agentTree.setHeaderVisible(true);
		
		TreeColumn column1 = new TreeColumn(agentTree, SWT.LEFT);
		agentTree.setLinesVisible(true);
		column1.setAlignment(SWT.LEFT);
		column1.setText("Object");
		TreeColumn column2 = new TreeColumn(agentTree, SWT.RIGHT);
		column2.setAlignment(SWT.RIGHT);
		column2.setText("Perf");
		
		TreeColumnLayout layout = new TreeColumnLayout();
		mainComposite.setLayout( layout );
		
		layout.setColumnData( column1, new ColumnWeightData( 68 ) );
		layout.setColumnData( column2, new ColumnWeightData( 22 ) );
		
		objTreeViewer.setContentProvider(new ViewContentProvider());
		objTreeViewer.setLabelProvider(new TableLabelProvider());
		objTreeViewer.setInput(root);

		noticeLabel.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				refreshCounterEngine();
			}
		});

		createContextMenu(objTreeViewer, new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager){
            	if(selectedItem){
            		ISelection selection = objTreeViewer.getSelection();
					if (selection instanceof IStructuredSelection) {
			            IStructuredSelection sel = (IStructuredSelection)selection;
			            Object[] elements = sel.toArray();
			            if (elements == null || elements.length < 1) {
			            	return;
			            }
			            Object selObject = elements[elements.length - 1];
			            if (selObject instanceof HierarchyObject) {
			            	fillMenu(manager, (HierarchyObject) selObject);
			            }
					}
            		
            	}else{
            		backgroundContextMenu(manager);
            	}
            }
        });
		
		objTreeViewer.getTree().addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				TreeItem item = objTreeViewer.getTree().getItem(point);
				if (item != null) {
					selectedItem = true;
				} else {
					selectedItem = false;
				}
				
			}
		});
		
		// DOUBLE CLICK HANDLING
		objTreeViewer.getTree().addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				TreeItem item = objTreeViewer.getTree().getItem(point);
				if (item != null) {
					StructuredSelection sel = (StructuredSelection) objTreeViewer.getSelection();
					Object o = sel.getFirstElement();
					if (o instanceof AgentObject) {
						AgentObject ao = (AgentObject) o;
						if (objSelMgr.unselectedSize() > 0) {
							objSelMgr.selectObj(ao.getObjHash());
							if (objSelMgr.unselectedSize() >= objectList.size()) {
								objSelMgr.clear();
							}
						} else {
							Set<Integer> unselSet = new HashSet<Integer>();
							for (AgentObject a : objectList) {
								if (a.getObjHash() != ao.getObjHash()) {
									unselSet.add(a.getObjHash());
								}
							}
							objSelMgr.addAll(unselSet);
						}
						refreshViewer();
					}
				}
			}
		});
		
		createQuickMenus();
		initTopMenuMangers();
		ObjectSelectManager.getInstance().addObjectCheckStateListener(new IObjectCheckListener() {
			public void notifyChangeState() {
				if (objSelMgr.unselectedSize() > 0) {
					agentTree.setBackground(ColorUtil.getInstance().getColor("azure"));
				} else {
					agentTree.setBackground(null);
				}
				refreshViewer();
			}
		});
		
		thread = new RefreshThread(this, 3000);
		thread.start();
	}
	
	private void resetTopMenu(IMenuManager manager, String dummy) {
		manager.removeAll();
		manager.add(new DummyAction(dummy, Images.alert));
	}
	
	private HierarchyObject fillTopMenu(HierarchyObject object, IMenuManager manager) {
		manager.removeAll();
		fillMenu(manager, object);
		return object;
	}
	
	private void initTopMenuMangers() {
		WorkbenchWindow win = (WorkbenchWindow) getViewSite().getWorkbenchWindow();
		topMenuManager = win.getMenuManager();
		collectorMenuManager = (IMenuManager) topMenuManager.find("scouter.menu.collector");
		hostMenuManager = (IMenuManager) topMenuManager.find("scouter.menu.host");
		objectMenuManager = (IMenuManager) topMenuManager.find("scouter.menu.object");
		collectorMenuManager.add(new DummyAction("Choose Collector in ObjectView", Images.alert));
		hostMenuManager.add(new DummyAction("Choose Host in ObjectView", Images.alert));
		objectMenuManager.add(new DummyAction("Choose Object in ObjectView", Images.alert));
		topMenuManager.update(true);
		collectorMenuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				ISelection selection = objTreeViewer.getSelection();
				if (selection instanceof IStructuredSelection) {
		            IStructuredSelection sel = (IStructuredSelection)selection;
		            Object[] elements = sel.toArray();
		            ServerObject serverObject = null;
		            if (elements == null || elements.length < 1) {
		            	Server defServer = ServerManager.getInstance().getDefaultServer();
		            	serverObject = root.get(defServer.getName());
		            } else {
		            	Object selObject = elements[elements.length - 1];
		            	if (selObject instanceof ServerObject) {
		            		serverObject = (ServerObject) selObject;
		            	} else if (selObject instanceof AgentObject) {
		            		AgentObject ao = (AgentObject) selObject;
		            		Server server = ServerManager.getInstance().getServer(ao.getServerId());
		            		serverObject = root.get(server.getName());
		            	}
		            }
		            if (serverObject != null) {
		            	fillTopMenu(serverObject, mgr);
		            } else {
		            	resetTopMenu(collectorMenuManager, "Choose Collector in ObjectView");
		            }
		            
				}
			}
		});
		hostMenuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				ISelection selection = objTreeViewer.getSelection();
				if (selection instanceof IStructuredSelection) {
		            IStructuredSelection sel = (IStructuredSelection)selection;
		            Object[] elements = sel.toArray();
		            AgentObject hostObject = null;
		            if (elements == null || elements.length < 1) {
		            	return;
		            } else {
		            	Object selObject = elements[elements.length - 1];
		            	if (selObject instanceof AgentObject) {
		            		AgentObject agent = (AgentObject) selObject;
		            		int serverId = agent.getServerId();
			            	Server server = ServerManager.getInstance().getServer(serverId);
			            	CounterEngine engine = server.getCounterEngine();
			            	if (engine.isChildOf(agent.getObjType(), CounterConstants.FAMILY_HOST)) {
			            		hostObject = agent;
			            	} else {
			            		String objName = agent.getObjName();
		            			String host = objName.substring(0, objName.indexOf("/", 1));
		            			hostObject = agentThread.getAgentObject(HashUtil.hash(host));
			            	}
		            	}
		            }
		            if (hostObject != null) {
		            	fillTopMenu(hostObject, mgr);
		            } else {
		            	resetTopMenu(hostMenuManager, "Choose Host in ObjectView");
		            }
				}
			}
		});
		objectMenuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				ISelection selection = objTreeViewer.getSelection();
				if (selection instanceof IStructuredSelection) {
		            IStructuredSelection sel = (IStructuredSelection)selection;
		            Object[] elements = sel.toArray();
		            AgentObject object = null;
		            if (elements == null || elements.length < 1) {
		            	return;
		            } else {
		            	Object selObject = elements[elements.length - 1];
		            	if (selObject instanceof AgentObject) {
		            		AgentObject agent = (AgentObject) selObject;
		            		int serverId = agent.getServerId();
			            	Server server = ServerManager.getInstance().getServer(serverId);
			            	CounterEngine engine = server.getCounterEngine();
			            	if (!engine.isChildOf(agent.getObjType(), CounterConstants.FAMILY_HOST)) {
			            		object = agent;
			            	}
		            	}
		            }
		            if (object != null) {
		            	fillTopMenu(object, mgr);
		            } else {
		            	resetTopMenu(objectMenuManager, "Choose Object in ObjectView");
		            }
				}
			}
		});
	}

	private void createQuickMenus(){
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Expand All", ImageUtil.getImageDescriptor(Images.expand)) {
			public void run() {
				objTreeViewer.expandAll();
			}
		});
		man.add(new Action("Collapse All", ImageUtil.getImageDescriptor(Images.collapse)) {
			public void run() {
				objTreeViewer.collapseAll();
			}
		});
		man.add(new Separator());
		man.add(new OpenServerManagerAction());
		man.add(new Action("Filter Object", ImageUtil.getImageDescriptor(Images.filter)) {
			public void run() {
				new ObjectSelectionDialog().show();
			}
		});
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				forceRefresh();
			}
		});
		
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
		menuManager.add(new Action("Remove Inactive", ImageUtil.getImageDescriptor(Images.minus)) {
			public void run() {
				boolean result = MessageDialog.openConfirm(getViewSite().getShell(), "Remove Inactive",  "Inactive object(s) will be removed. Continue?");
				if (result) {
					AgentModelThread.removeInactive();
				}
			}
		});
		menuManager.add(new Separator());
		MenuManager presentMenu = new MenuManager("Object Presentation");
		menuManager.add(presentMenu);
		Action hieracyMenu = new Action("Hierarchical", IAction.AS_RADIO_BUTTON) {
			public void run() {
				mode = PresentMode.HIERACHY_MODE;
				refresh();
			}
		};
		hieracyMenu.setImageDescriptor(ImageUtil.getImageDescriptor(Images.tree_mode));
		presentMenu.add(hieracyMenu);
		Action flatMenu = new Action("Flat", IAction.AS_RADIO_BUTTON) {
			public void run() {
				mode = PresentMode.FLAT_MODE;
				refresh();
			}
		};
		flatMenu.setImageDescriptor(ImageUtil.getImageDescriptor(Images.flat_layout));
		presentMenu.add(flatMenu);
		hieracyMenu.setChecked(true);
	}
	
	private void createContextMenu(Viewer viewer, IMenuListener listener){
        MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(listener);
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(contextMenu, viewer);
    }
	
	private void backgroundContextMenu(IMenuManager mgr){
		IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (objSelMgr.unselectedSize() > 0) {
			mgr.add(new ClearObjectFilterAction());
		}
		mgr.add(new Separator());
		mgr.add(new AddServerAction(win, "Add Server", Images.add));
		mgr.add(new Separator());
		mgr.add(new OpenGroupNavigationAction(win));
	}
	
	private static HashMap<Integer, Map<String, Action>> counterActions = new HashMap<Integer, Map<String, Action>>();
	
	private void fillMenu(IMenuManager mgr, HierarchyObject selObject){
        IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        // CREATE CONTEXT MENU - Agent Object
        if (selObject instanceof AgentObject) {
        	AgentObject ao = (AgentObject) selObject;
        	CounterEngine engine = ServerManager.getInstance().getServer(ao.getServerId()).getCounterEngine();
        	if (!engine.isUnknownObjectType(ao.getObjType())) {
            	mgr.add(new DummyAction(ao.getDisplayName(), Images.getObjectIcon(ao.getObjType(), true, ao.getServerId())));
				mgr.add(new Separator());
        	}
        	MenuUtil.addObjectContextMenu(mgr, win, (AgentObject) selObject);
	    // CREATE CONTEXT MENU - Server Object	
		} else if (selObject instanceof ServerObject) {
			
			int serverId = ((ServerObject) selObject).getId();
			Server server = ServerManager.getInstance().getServer(serverId);
			boolean isOpen = server.isOpen();
			if (isOpen) {
				CounterEngine counterEngine = server.getCounterEngine();
				mgr.add(new DummyAction(server.getName(), Images.server));
				mgr.add(new Separator());
				
				//if (server.isConnected()) {
				Map<String, Action> counterActionsMap = counterActions.get(serverId);
				if (counterActionsMap == null) {
					counterActionsMap = MenuUtil.getCounterActionList(win, counterEngine, serverId);
					if(counterActionsMap == null)
						return;
					counterActions.put(serverId, counterActionsMap);
				}
				addExistObjectTypeMenus(win, mgr, counterEngine, counterActionsMap, serverId);
				if (server.isEnableMenu("tag_count")) {
					mgr.add(new Separator());
					mgr.add(new OpenTagCountViewAction(win, serverId));
				}
				mgr.add(new Separator());
				mgr.add(new OpenTotalSummaryAction(win, serverId));
				mgr.add(new OpenAlertDetailListAction(win, serverId));

				mgr.add(new Separator());
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_CONFIGURE)) {
					MenuManager configMenuManager = new MenuManager(MenuStr.SERVER_CONFIGURES, ImageUtil.getImageDescriptor(Images.config), MenuStr.SERVER_CONFIGURES_ID);
					mgr.add(configMenuManager);

					configMenuManager.add(new OpenServerConfigureAction(win, MenuStr.CONFIGURE, Images.config, serverId));

					MenuManager telegrafMenuManager = new MenuManager("Telegraf Config", ImageUtil.getImageDescriptor(Images.config), "scouter.client.contextmenu.server.telegraf.configure");
					configMenuManager.add(telegrafMenuManager);

					telegrafMenuManager.add(new OpenTelegrafConfigureAction(win, MenuStr.TELEGRAF_CONFIGURE, serverId));
					telegrafMenuManager.add(new OpenTelegrafFileConfigureAction(win, MenuStr.TELEGRAF_FILE_CONFIGURE, serverId));
					configMenuManager.add(new OpenCountersSiteFileConfigureAction(win, MenuStr.COUNTERS_SITE_FILE_CONFIGURE, serverId));
				}

				MenuManager alertMenuManager = new MenuManager(MenuStr.ALERT_SCRIPTING, ImageUtil.getImageDescriptor(Images.alert), MenuStr.ALERT_SCRIPTING_ID);
				mgr.add(alertMenuManager);
				Map<String, Map<String, String>> alertMenuMap = new TreeMap<>();
				for (AgentObject agentObject : AgentModelThread.getInstance().getObjectList()) {
					if (agentObject.getServerId() != serverId) {
						continue;
					}
					ObjectType objectType = counterEngine.getObjectType(agentObject.getObjType());
					if(objectType == null) {
						System.out.println("[Null Object Type]" + agentObject.getObjType());
						continue;
					}
					if (objectType.isSubObject()) {
						continue;
					}
					Family family = objectType.getFamily();
					if(family == null) {
						System.out.println("[Null family Type]" + agentObject.getObjType());
						continue;
					}
					List<Counter> counterList = family.listCounters();
					for (Counter counter : counterList) {
						if(!counter.isAll()) continue;
						Map<String, String> itemMap = new HashMap<>();
						itemMap.put("familyName", family.getName());
						itemMap.put("counterName", counter.getName());
						itemMap.put("counterDisplayName", counter.getDisplayName());
						alertMenuMap.put(family.getName() + ":" + counter.getName(), itemMap);
					}
				}
				for (Map.Entry<String, Map<String, String>> entry : alertMenuMap.entrySet()) {
					Map<String, String> entryValue = entry.getValue();
					alertMenuManager.add(new OpenAlertScriptingAction(win, entry.getKey(), Images.alert, serverId,
							entryValue.get("familyName"), entryValue.get("counterName"), entryValue.get("counterDisplayName")));
				}

				mgr.add(new OpenObjectDailyListAction(win, "Object Daily List", Images.GO_PAST, serverId));
				mgr.add(new Separator());
				MenuManager management = new MenuManager(MenuStr.MANAGEMENT, MenuStr.MANAGEMENT_ID);
				mgr.add(management);
				management.add(new OpenCxtmenuEnvAction(win, MenuStr.ENV, 0, serverId));
				management.add(new OpenServerThreadListAction(win, MenuStr.SERVER_THREAD_LIST, Images.thread, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_LOGINLIST))
					management.add(new OpenLoginListAction(win, MenuStr.CURRENT_LOGIN_LIST, Images.CONFIG_USER, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_DBMANAGER))
					management.add(new OpenServerFileManagementAction(win, MenuStr.FILE_MANAGEMENT, Images.explorer, serverId));
				management.add(new OpenServerLogsAction(win, serverId));
				MenuManager userMenu = new MenuManager(MenuStr.ACCOUNT, ImageUtil.getImageDescriptor(Images.CONFIG_USER), MenuStr.ACCOUNT_ID);
				management.add(userMenu);
				userMenu.add(new ListAccountAction(win, serverId));
				userMenu.add(new OpenGroupPolicyAction(win, serverId));
				userMenu.add(new Separator());
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_ADDACCOUNT))
					userMenu.add(new AddAccountAction(win, serverId));
				if (server.isAllowAction(GroupPolicyConstants.ALLOW_EDITACCOUNT))
					userMenu.add(new EditAccountAction(win, serverId));
				//}
				mgr.add(new Separator());
				if(ServerManager.getInstance().getOpenServerList().size() > 1) {
					if(server != ServerManager.getInstance().getDefaultServer() && server.isConnected()){
						mgr.add(new SetDefaultServerAction(win, server));
					}
					mgr.add(new CloseServerAction(win, serverId));
					mgr.add(new RemoveServerAction(win, serverId));
				}
				mgr.add(new Separator());
				mgr.add(new OpenCxtmenuServerPropertiesAction(win, MenuStr.PROPERTIES, serverId));
				if (false) {
					mgr.add(new Separator());
					mgr.add(new OpenAPIDebugViewAction(win, 0, serverId));
				}
			} else {
				mgr.add(new OpenServerAction(serverId));
				mgr.add(new RemoveServerAction(win, serverId));
			}
		} else if (selObject instanceof GroupObject) {
			GroupObject obj = (GroupObject) selObject;
			HierarchyObject parent = obj.getParent();
			if (parent == null || parent instanceof ServerObject == false) {
				return;
			}
			int serverId = ((ServerObject) parent).getId();
			CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
			Map<String, Action> counterActionsMap = counterActions.get(serverId);
			if (counterActionsMap == null) {
				counterActionsMap = MenuUtil.getCounterActionList(win, counterEngine, serverId);
				if(counterActionsMap == null)
					return;
				counterActions.put(serverId, counterActionsMap);
			}
			addObjectTypeMenu(mgr, counterEngine, counterActionsMap, serverId, obj.getObjType());
		} else {
			mgr.removeAll();
		}
    }
	
	private void addExistObjectTypeMenus(IWorkbenchWindow win, IMenuManager mgr, CounterEngine counterEngine, Map<String, Action> actionMap, int serverId) {
		Set<String> agentTypeList = agentThread.getCurrentObjectTypeList(serverId);
		List<ObjectType> objTypeList = new ArrayList<>(); 
		for(String objType : agentTypeList){
			ObjectType type = counterEngine.getObjectType(objType);
			if (type != null) {
				objTypeList.add(type);
			}
		}
		Collections.sort(objTypeList, new Comparator<ObjectType>() {

			@Override
			public int compare(ObjectType o1, ObjectType o2) {
				return o1.getDisplayName().compareTo(o2.getDisplayName());
			}
			
		});

		for(ObjectType type : objTypeList){
			String objType = type.getName();
			String displayName = type.getDisplayName();
			ImageDescriptor objImage = Images.getObjectImageDescriptor(objType, true, serverId);
			MenuManager objTitle = new MenuManager(displayName, objImage, "scouter.menu.id."+displayName);
			mgr.add(objTitle);
			addObjectTypeMenu(objTitle, counterEngine, actionMap, serverId, objType);
			MenuUtil.addObjTypeSpecialMenu(win, objTitle, serverId, objType, counterEngine);
		}
	}

	private void addObjectTypeMenu(IMenuManager objTitle, CounterEngine counterEngine, Map<String, Action> actionMap, int serverId, String objType) {
		ArrayList<String> counters = counterEngine.getAllCounterWithDisplay(objType);
		for(int i = 0 ; counters != null && i < counters.size() ; i++){
			String[] cnt = counters.get(i).split(":");
			String counter = cnt[1];
			Action realtimeAllAct = actionMap.get(objType + ":" + counter);
			if (realtimeAllAct != null) {
				objTitle.add(realtimeAllAct);
			}
		}
	}

	private static void removeActionCache(int serverId) {
		counterActions.remove(serverId);
	}
	
	public void setFocus() {
		objTreeViewer.getControl().setFocus();
	}

	public void dispose() {
		super.dispose();
		if(thread != null && thread.isAlive()){
			thread.shutdown();
			thread = null;
		}
	}
	
	ArrayList<AgentObject> objectList = new ArrayList<AgentObject>();
	
	private void getObjectList() {
		AgentObject[] agentList = agentThread.getObjectList();
		objectList = new ArrayList<AgentObject>(Arrays.asList(agentList));
	}
	
	private synchronized void makeHierarchyMap() {
		Map<String, ServerObject> tempRootMap = new TreeMap<String, ServerObject>();
		Enumeration<Integer> sIds = ServerManager.getInstance().getAllServerList();
		while(sIds.hasMoreElements()) {
			int serverId =sIds.nextElement();
			Server server = ServerManager.getInstance().getServer(serverId);
			ServerObject serverObj = new ServerObject(serverId, server.getName());
			if (server.isOpen()) {
				serverObj.setTotalMemory(server.getTotalMemory());
				serverObj.setUsedMemory(server.getUsedMemory());
			}
			tempRootMap.put(server.getName(), serverObj);
		}
		getObjectList();
		for (AgentObject agent : objectList) {
			if (agent.getObjType().startsWith("z$")) {
				System.out.println("agent: " + agent);
			}
			int serverId = agent.getServerId();
			Server server = ServerManager.getInstance().getServer(serverId);
			if (server == null) {
				continue;
			}
			ServerObject serverObj = tempRootMap.get(server.getName());
			if (serverObj == null) { 
				continue;
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
	
	private synchronized void makeFlatMap() {
		Map<String, ServerObject> tempRootMap = new HashMap<String, ServerObject>();
		Enumeration<Integer> sIds = ServerManager.getInstance().getAllServerList();
		while(sIds.hasMoreElements()) {
			int serverId =sIds.nextElement();
			
			Server server = ServerManager.getInstance().getServer(serverId);
			ServerObject serverObj = new ServerObject(serverId, server.getName());
			if (server.isOpen()) {
				serverObj.setTotalMemory(server.getTotalMemory());
				serverObj.setUsedMemory(server.getUsedMemory());
			}
			tempRootMap.put(server.getName(), serverObj);
		}
		getObjectList();
		for (AgentObject agent : objectList) {
			int serverId = agent.getServerId();
			Server server = ServerManager.getInstance().getServer(serverId);
			if (server == null) {
				continue;
			}
			ServerObject serverObj = tempRootMap.get(server.getName());
			if (serverObj == null) {
				continue;
			}
			String objType = agent.getObjType();
			HierarchyObject grpObj = serverObj.getChild(objType);
			if (grpObj == null) {
				GroupObject objTypeObj = new GroupObject(objType, server.getName() + "/" + server.getCounterEngine().getDisplayNameObjectType(objType));
				serverObj.putChild(objType, objTypeObj);
				objTypeObj.setParent(serverObj);
				grpObj = objTypeObj;
			}
			grpObj.putChild(agent.getObjName(), agent);
			agent.setParent(grpObj);
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
			if (parentElement instanceof HierarchyObject){
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
			if (element instanceof HierarchyObject){
				return ((HierarchyObject) element).getChildSize() > 0;
			}
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			return true;
		}
		
	}
	
	class TableLabelProvider implements ITableLabelProvider, IColorProvider  {
		
		public Image getColumnImage(Object obj, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (obj instanceof AgentObject) {
					AgentObject a = (AgentObject) obj;
					if (mode == PresentMode.HIERACHY_MODE) {
						return Images.getObjectIcon(a.getObjType(), a.isAlive(), a.getServerId());
					} else if (mode == PresentMode.FLAT_MODE) {
						return a.isAlive() ? Images.active : Images.dead;
					}
				} else if (obj instanceof ServerObject) {
					ServerObject a = (ServerObject) obj;
					Server server = ServerManager.getInstance().getServer(a.getId());
					boolean isOpen = server.isOpen();
					if (isOpen) {
						boolean isConnected = server.isConnected();
						if (ServerManager.getInstance().getDefaultServer() == server) {
							if (isConnected) {
								return Images.SERVER_DEFAULT_ACT;
							} else {
								return Images.SERVER_DEFAULT_INACT;
							}
						} else {
							if (isConnected) {
								return Images.SERVER_ACT;
							} else {
								return Images.SERVER_INACT;
							}
						}
					} else {
						return Images.close_folder;
					}
				} else if (obj instanceof GroupObject) {
					GroupObject grpObj = (GroupObject) obj;
					HierarchyObject parent = grpObj.getParent();
					if (parent != null && parent instanceof ServerObject) {
						return Images.getObjectIcon(grpObj.getObjType(), true, ((ServerObject)parent).getId());
					} else {
						return Images.getObjectIcon(grpObj.getObjType(), true, 0);
					}
				} else if (obj instanceof DummyObject) {
					DummyObject dummyObj = (DummyObject) obj;
					if (dummyObj.getChildSize() > 0) {
						return Images.folder;
					} else {
						return Images.default_context;
					}
				}
				return Images.default_context;
			case 1:
				if (obj instanceof AgentObject) {
					AgentObject a = (AgentObject) obj;
					if (a.isAlive()) {
						boolean activeStack = a.getSpec().tags.getBoolean("ActiveStack");
						if (activeStack) {
							return Images.active;
						}
					}
				}
			}
			return null;
		}

		public String getColumnText(Object obj, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (obj instanceof AgentObject) {
					AgentObject a = (AgentObject) obj;
					String display = "";
					if (mode == PresentMode.HIERACHY_MODE) {
						display += a.getDisplayName();
					} else if (mode == PresentMode.FLAT_MODE) {
						display += a.getObjName();
					}
					return display;
				} else if (obj instanceof ServerObject) {
					ServerObject a = (ServerObject) obj;
					return a.getDisplayName();
				} else if (obj instanceof GroupObject) {
					GroupObject a = (GroupObject) obj;
					String name = a.getName();
					int index = name.indexOf("/");
					if (index > -1) {
						return name.substring(index + 1, name.length());
					} else {
						return name;
					}
				} else if (obj instanceof DummyObject) {
					DummyObject a = (DummyObject) obj;
					return a.getDisplayName();
				}
				return obj.toString();
			case 1:
				if (obj instanceof AgentObject) {
					AgentObject a = (AgentObject) obj;
					if(a.isAlive()){
						Value value = a.getMasterCounter();
						if (value == null) {
							return null;
						}
						Server server = ServerManager.getInstance().getServer(a.getServerId());
						if(server == null)
							return "?";
						return getColumnTextForByte(value, server.getCounterEngine().getMasterCounterUnit(a.getObjType()));
					}
				} else if (obj instanceof ServerObject) {
					ServerObject o = (ServerObject) obj;
					if (o.getUsedMemory() > 0) {
						return ScouterUtil.humanReadableByteCount(o.getUsedMemory(), true);
					}
				}
			}
			return null;
		}

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		public void removeListener(ILabelProviderListener listener) {
		}

		public Color getForeground(Object obj) {
			if (obj instanceof AgentObject) {
				AgentObject a = (AgentObject) obj;
				if(!a.isAlive()){
					return ColorUtil.getInstance().getColor("gray");
				} else if (objSelMgr.isUnselectedObject(a.getObjHash())) {
					return ColorUtil.getInstance().getColor(SWT.COLOR_GRAY);
				}
			}
			return null;
		}

		public Color getBackground(Object element) {
			return null;
		}
		
		public String getColumnTextForByte(Value value, String unit){
			if(unit != null && "bytes".equals(unit)){
				double v = CastUtil.cdouble(value);
				return ScouterUtil.humanReadableByteCount(v, true);
			}
			return FormatUtil.print(value, "#,###.##") + " " + unit;
		}
	}
	
	public static void main(String[] args) {
		String s = "/ab/bc/cd";
		int inx = s.indexOf("/", 1);
		while (inx != -1) {
			System.out.println(s.substring(0, inx));
			inx = s.indexOf("/", (inx+1));
		}
		System.out.println(s);
	}
}
