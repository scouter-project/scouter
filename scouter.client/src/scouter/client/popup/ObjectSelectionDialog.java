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
package scouter.client.popup;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import scouter.client.Images;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.GroupObject;
import scouter.client.model.HierarchyObject;
import scouter.client.model.ServerObject;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.UIUtil;
import scouter.lang.counters.CounterEngine;

public class ObjectSelectionDialog {
	
	public CheckboxTreeViewer objTreeViewer;
	
	private Map<String, ServerObject> root = new TreeMap<String, ServerObject>();
	ObjectSelectManager objSelMgr = ObjectSelectManager.getInstance();
	
	public void show() {
		final Shell dialog = new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setText("Filter Object");
		 createPartControl(dialog);
		 makeTreeContents();
		 dialog.pack();
		 dialog.open();
	}
	

	Set<Integer> objHashSet = new HashSet<Integer>();
	
	private void createPartControl(final Shell dialog) {
		dialog.setLayout(new GridLayout(1, true));
		Composite mainComposite = new Composite(dialog, SWT.NONE);
		GridData gr = new GridData(SWT.FILL, SWT.FILL, true, true);
		gr.widthHint = 500;
		gr.heightHint = 400;
		mainComposite.setLayoutData(gr);
		objTreeViewer = new CheckboxTreeViewer(mainComposite, SWT.BORDER | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		Tree objTree = objTreeViewer.getTree();
		objTree.setHeaderVisible(true);
		objTree.setLinesVisible(false);
		TreeColumn column1 = new TreeColumn(objTree, SWT.LEFT);
		column1.setAlignment(SWT.LEFT);
		column1.setText("Type/Object");
		TreeColumnLayout layout = new TreeColumnLayout();
		mainComposite.setLayout( layout );
		layout.setColumnData(column1, new ColumnWeightData(100));
		objTreeViewer.setContentProvider(new ViewContentProvider());
		objTreeViewer.setLabelProvider(new TableLabelProvider());
		objTreeViewer.setCheckStateProvider(new TreeCheckStateProvider());
		objTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				objTreeViewer.setSubtreeChecked(event.getElement(), event.getChecked());
			}
		});
		objTreeViewer.setInput(root);
		
		Composite bottomComp = new Composite(dialog, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, true);
		bottomComp.setLayoutData(gr);
		bottomComp.setLayout(UIUtil.formLayout(0, 0));
		Button cancelBtn = new Button(bottomComp, SWT.PUSH);
		cancelBtn.setLayoutData(UIUtil.formData(null, -1, 0, 5, 100, -5, null, -1, 100));
		cancelBtn.setText("&Cancel");
		cancelBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dialog.close();
			}
		});
		
		Button okBtn = new Button(bottomComp, SWT.PUSH);
		okBtn.setLayoutData(UIUtil.formData(null, -1, 0, 5, cancelBtn, -5, null, -1, 100));
		okBtn.setText("&Apply");
		okBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] checkedElements = objTreeViewer.getCheckedElements();
				Set<Integer> checkdObjHashSet = new HashSet<Integer>(); 
				for (Object o : checkedElements) {
					if (o instanceof AgentObject) {
						AgentObject a = (AgentObject) o;
						checkdObjHashSet.add(a.getObjHash());
					}
				}
				objHashSet.removeAll(checkdObjHashSet);
				objSelMgr.addAll(objHashSet);
				dialog.close();
			}
		});
	}
	
	private void makeTreeContents() {
		Set<Integer> sIds = ServerManager.getInstance().getOpenServerList();
		for (int serverId : sIds) {
			String serverName = ServerManager.getInstance().getServer(serverId).getName();
			ServerObject serverObj = new ServerObject(serverId, serverName);
			root.put(serverName, serverObj);
		}
		AgentObject[] objectList = AgentModelThread.getInstance().getObjectList();
		for (AgentObject agent : objectList) {
			int serverId = agent.getServerId();
			String serverName = ServerManager.getInstance().getServer(serverId).getName();
			ServerObject serverObj = root.get(serverName);
			if (serverObj == null) {
				continue;
			}
			AgentObject obj = new AgentObject(agent);
			objHashSet.add(obj.getObjHash());
			HierarchyObject grpObj = serverObj.getChild(obj.getObjType());
			CounterEngine engine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
			if (grpObj == null) {
				grpObj = new GroupObject(obj.getObjType(), serverName + "/" + engine.getDisplayNameObjectType(obj.getObjType()));
				serverObj.putChild(obj.getObjType(), grpObj);
				grpObj.setParent(serverObj);
			}
			grpObj.putChild(obj.getObjName(), obj);
			obj.setParent(grpObj);
		}
		objTreeViewer.refresh();
		objTreeViewer.expandAll();
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
			// TODO Auto-generated method stub
			return true;
		}
		
	}
	
	class TableLabelProvider implements ITableLabelProvider, IColorProvider  {
		
		public Image getColumnImage(Object obj, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (obj instanceof AgentObject) {
					AgentObject a = (AgentObject) obj;
					return a.isAlive() ? Images.active : Images.dead;
				} else if (obj instanceof ServerObject) {
					ServerObject a = (ServerObject) obj;
					Server server = ServerManager.getInstance().getServer(a.getId());
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
				} else if (obj instanceof GroupObject) {
					GroupObject grpObj = (GroupObject) obj;
					HierarchyObject parent = grpObj.getParent();
					if (parent != null && parent instanceof ServerObject) {
						return Images.getObjectIcon(grpObj.getObjType(), true, ((ServerObject)parent).getId());
					} else {
						return Images.getObjectIcon(grpObj.getObjType(), true, 0);
					}
				} 
				return Images.default_context;
			}
			return null;
		}

		public String getColumnText(Object obj, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (obj instanceof AgentObject) {
					AgentObject a = (AgentObject) obj;
					String display = a.getObjName();
					return display;
				} else if (obj instanceof ServerObject) {
					ServerObject a = (ServerObject) obj;
					return a.getName();
				} else if (obj instanceof GroupObject) {
					GroupObject a = (GroupObject) obj;
					String name = a.getName();
					int index = name.indexOf("/");
					if (index > -1) {
						return name.substring(index + 1, name.length());
					} else {
						return name;
					}
				}
				return obj.toString();
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
				}
			}
			return null;
		}

		public Color getBackground(Object element) {
			return null;
		}
	}
	
	class TreeCheckStateProvider implements ICheckStateProvider {
		public boolean isChecked(Object element) {
			if (element instanceof AgentObject) {
				return !objSelMgr.isUnselectedObject(((AgentObject) element).getObjHash());
			}
			return false;
		}

		public boolean isGrayed(Object element) {
			if (element instanceof AgentObject)
				return false;
			return true;
		}
	}
}
