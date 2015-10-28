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
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IFigureProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.HorizontalTreeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.RadialLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import scouter.client.Images;
import scouter.client.constants.MenuStr;
import scouter.client.context.actions.OpenCxtmenuActiveServiceListAction;
import scouter.client.dashboard.figure.AgentObjectFigure;
import scouter.client.dashboard.figure.DummyObjectFigure;
import scouter.client.dashboard.figure.ServerObjectFigure;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.DummyObject;
import scouter.client.model.HierarchyObject;
import scouter.client.model.RefreshThread;
import scouter.client.model.ServerObject;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.popup.ObjectSelectionDialog;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.AlertProxyThread;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.AlertProxyThread.IAlertListener;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.DummyAction;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.MenuUtil;
import scouter.lang.ObjectType;
import scouter.lang.counters.CounterConstants;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.AlertPack;
import scouter.util.CacheTable;

public class ObjectDashboardView extends ViewPart implements Refreshable, IObjectCheckListener, IAlertListener {
	
	public final static String ID = ObjectDashboardView.class.getName();
	
	Font arial10font = new Font(null, "Arial", 10, SWT.BOLD);
	
	private GraphViewer viewer = null;
	private AgentModelThread agentThread = AgentModelThread.getInstance();
	private TreeMap<String, HierarchyObject> rootMap = new TreeMap<String, HierarchyObject>();
	private ArrayList<HierarchyObject> elementList = new ArrayList<HierarchyObject>();
	ObjectSelectManager objSelManager = ObjectSelectManager.getInstance();
	private RefreshThread thread;
	private Action actAutoRefresh = null;
	private boolean autoRefresh = true;
	Action autoResizeAct;
	Action withServerObjAct;
	
	 IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	 
	 AlertProxyThread alertThread = AlertProxyThread.getInstance();
	 CacheTable<Integer, AlertPack> alertTable = new CacheTable<Integer, AlertPack>();

	public void createPartControl(final Composite parent) {
		parent.setLayout(new FillLayout());
		parent.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent e) {
				if (autoResizeAct.isChecked()) {
					viewer.applyLayout();
				}
			}
			public void controlMoved(ControlEvent e) {
			}
		});
		viewer = new GraphViewer(parent, SWT.NONE);
		viewer.setContentProvider(new DashboardContentProvider());
		viewer.setLabelProvider(new DashboardLabelProvider());
		final int style = LayoutStyles.NO_LAYOUT_NODE_RESIZING;
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Filter Object", ImageUtil.getImageDescriptor(Images.filter)) {
			public void run() {
				new ObjectSelectionDialog().show();
			}
		});
		actAutoRefresh = new Action("AutoRefresh in 2 sec.", IAction.AS_CHECK_BOX){ 
	        public void run(){
	        	autoRefresh = actAutoRefresh.isChecked();
	        }    
	    };
	    actAutoRefresh.setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh_auto));
	    man.add(actAutoRefresh);
	    actAutoRefresh.setChecked(true);
		
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
		MenuManager layoutMenu = new MenuManager("Layout");
		menuManager.add(layoutMenu);
		
		Action treeLayout = new Action("Tree", IAction.AS_RADIO_BUTTON) {
			public void run() {
				viewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(style));
				viewer.applyLayout();
			}
		};
		layoutMenu.add(treeLayout);
		treeLayout.setChecked(true);
		
		Action horizontalTreeLayout = new Action("Horizontal Tree", IAction.AS_RADIO_BUTTON) {
			public void run() {
				viewer.setLayoutAlgorithm(new HorizontalTreeLayoutAlgorithm(style));
				viewer.applyLayout();
			}
		};
		layoutMenu.add(horizontalTreeLayout);
		
		Action radialLayout = new Action("Radial", IAction.AS_RADIO_BUTTON) {
			public void run() {
				viewer.setLayoutAlgorithm(new RadialLayoutAlgorithm(style));
				viewer.applyLayout();
			}
		};
		layoutMenu.add(radialLayout);
		
		Action springLayout = new Action("Spring", IAction.AS_RADIO_BUTTON) {
			public void run() {
				viewer.setLayoutAlgorithm(new SpringLayoutAlgorithm(style));
				viewer.applyLayout();
			}
		};
		layoutMenu.add(springLayout);
		
		layoutMenu.add(new Separator());
		autoResizeAct = new Action("Auto Resize", IAction.AS_CHECK_BOX) {
			public void run() {	
				if (autoResizeAct.isChecked()) {
					parent.notifyListeners(SWT.Resize, new Event());
				}
			}
		};
		autoResizeAct.setChecked(true);
		layoutMenu.add(autoResizeAct);
		
		withServerObjAct = new Action("With Server", IAction.AS_CHECK_BOX) {
			public void run() {	
				notifyChangeState();
			}
		};
		withServerObjAct.setChecked(true);
		layoutMenu.add(withServerObjAct);
		
		viewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(style));
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof AgentObject) {
					AgentObject ao = (AgentObject) o;
					Server server = ServerManager.getInstance().getServer(ao.getServerId());
					if (server == null) return;
					CounterEngine counterEngine = server.getCounterEngine();
					if (counterEngine.isChildOf(ao.getObjType(), CounterConstants.FAMILY_JAVAEE)) {
						new OpenCxtmenuActiveServiceListAction(getSite().getWorkbenchWindow(), MenuStr.ACTIVE_SERVICE_LIST, ao.getObjHash(), ao.getObjType(), ao.getServerId()).run();
					}
				}
			}
		});
		
		MenuManager contextMgr = new MenuManager(); 
		Menu menu = contextMgr.createContextMenu(viewer.getGraphControl());
		contextMgr.setRemoveAllWhenShown(true);
		contextMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ISelection sel = viewer.getSelection();
				if (sel instanceof IStructuredSelection) {
		            IStructuredSelection selection = (IStructuredSelection)sel;
		            Object obj = selection.getFirstElement();
		            if (obj instanceof AgentObject) {
		            	AgentObject object = (AgentObject) obj;
		            	manager.add(new DummyAction(object.getObjName(), Images.getObjectIcon(object.getObjType(), true, object.getServerId())));
		            	manager.add(new Separator());
						MenuUtil.addObjectContextMenu(manager, win, object);
						return;
		            }
				}
			}
		});
		viewer.getGraphControl().setMenu(menu);
		
		
		objSelManager.addObjectCheckStateListener(this);
		alertThread.addAlertListener(this);
		
		thread = new RefreshThread(this, 2000);
		thread.start();
	}
	
	boolean firstTime = true;
	
	public void refresh() {
		if (firstTime == false && autoRefresh == false) {
			return;
		}
		forceRefresh();
		if (firstTime) {
			firstTime = false;
			ExUtil.exec(viewer.getGraphControl(), new Runnable() {
				public void run() {
					viewer.setInput(new Object());
				}
			});
		}
	}
	
	int lastElementSize;
	
	private void forceRefresh() {
		AgentObject[] objectList = agentThread.getObjectList();
		makeHierarchyMap(objectList);
		ExUtil.exec(viewer.getGraphControl(), new Runnable() {
			public void run() {
				viewer.refresh(true);
				if (lastElementSize < elementList.size()) {
					viewer.applyLayout();
				}
				lastElementSize = elementList.size();
			}
		});
	}
	
	private synchronized void makeHierarchyMap(AgentObject[] objectList) {
		elementList.clear();
		rootMap.clear();
		Set<Integer> sIds = ServerManager.getInstance().getOpenServerList();
		for (int serverId : sIds) {
			Server server = ServerManager.getInstance().getServer(serverId);
			ServerObject serverObj = new ServerObject(serverId, server.getName());
			rootMap.put(server.getName(), serverObj);
			if (withServerObjAct.isChecked()) {
				serverObj.setTotalMemory(server.getTotalMemory());
				serverObj.setUsedMemory(server.getUsedMemory());
				elementList.add(serverObj);
			}
		}
		for (AgentObject originAgent : objectList) {
			int serverId = originAgent.getServerId();
			Server server = ServerManager.getInstance().getServer(serverId);
			if (server == null) {
				continue;
			}
			HierarchyObject serverObj = rootMap.get(server.getName());
			if (serverObj == null) { 
				continue;
			}
			if (objSelManager.isUnselectedObject(originAgent.getObjHash())) {
				continue;
			}
			CounterEngine engine = server.getCounterEngine();
			if (engine.isUnknownObjectType(originAgent.getObjType())) continue;
			ObjectType type = engine.getObjectType(originAgent.getObjType());
			if (type != null && type.isSubObject()) continue;
			AgentObject newObj = new AgentObject(originAgent);
			String objName = newObj.getObjName();
			HierarchyObject parent = serverObj;
			int inx = objName.indexOf("/", 1);
			while (inx != -1) {
				String childName = objName.substring(0, inx);
				HierarchyObject child = parent.getChild(childName);
				if (child == null) {
					child = new DummyObject(childName);
					child.setParent(parent);
					parent.putChild(childName, child);
					elementList.add(child);
				}
				parent = child;
				inx = objName.indexOf("/", (inx+1));
			}
			HierarchyObject beforeDummyObj = parent.putChild(objName, newObj);
			if (beforeDummyObj != null && beforeDummyObj instanceof DummyObject) {
				elementList.remove(beforeDummyObj);
				newObj.setChildMap(((DummyObject)beforeDummyObj).getChildMap());
			}
			newObj.setParent(parent);
			elementList.add(newObj);
		}
	}
	
	public void dispose() {
		objSelManager.removeObjectCheckStateListener(this);
		alertThread.removeAlertListener(this);
		arial10font.dispose();
		super.dispose();
	}

	class DashboardContentProvider implements IGraphEntityContentProvider {

		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			return elementList.toArray(new HierarchyObject[elementList.size()]);
		}

		public Object[] getConnectedTo(Object entity) {
			if (entity instanceof HierarchyObject) {
				return ((HierarchyObject) entity).getSortedChildArray();
			}
			return null;
		}
	}
	
	class DashboardLabelProvider extends LabelProvider implements IFigureProvider {
		public Image getImage(Object element) {
			if (element instanceof AgentObject) {
				AgentObject ao = (AgentObject) element;
				return Images.getObjectIcon(ao.getObjType(), true, ao.getServerId());
			} else if (element instanceof ServerObject) {
				return Images.SERVER_DEFAULT_ACT;
			} else if (element instanceof DummyObject) {
				return Images.folder;
			}
			return null;
		}

		public String getText(Object element) {
			if (element instanceof HierarchyObject) {
				return ((HierarchyObject)element).getDisplayName();
			}
			return null;
		}

		public IFigure getFigure(Object element) {
			if (element instanceof AgentObject) {
				AgentObject ao = (AgentObject) element;
				return new AgentObjectFigure(ao, arial10font, alertTable.get(ao.getObjHash()));
			} else if (element instanceof DummyObject) {
				return new DummyObjectFigure((DummyObject) element); 
			} else if (element instanceof ServerObject) {
				return new ServerObjectFigure((ServerObject) element, arial10font); 
			}
			return null;
		}
	}
	
	public void setFocus() {
		
	}

	public void notifyChangeState() {
		forceRefresh();
		ExUtil.exec(viewer.getGraphControl(), new Runnable() {
			public void run() {
				viewer.applyLayout();
			}
		});
	}

	public void ariseAlert(int serverId, AlertPack alert) {
		alertTable.put(alert.objHash, alert, 30000);
	}
}
