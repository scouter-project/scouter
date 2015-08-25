/*
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
package scouter.client.maria.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.constants.StatusConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.StatusPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class DbLockListView extends ViewPart {
	
	public static final String ID = DbLockListView.class.getName();
	
	private TreeViewer viewer;
	private Tree tree;
	private TreeColumnLayout columnLayout;
	private long time;
	
	private int serverId;
	private int objHash;
	Map<Long, LockObject> root = new HashMap<Long, LockObject>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
		objHash = CastUtil.cint(ids[1]);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		this.setPartName("Lock List[" + TextProxy.object.getText(objHash) + "]");
		columnLayout = new TreeColumnLayout();
		parent.setLayout(columnLayout);
		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		viewer = new TreeViewer(tree);
		createColumns();
		viewer.setContentProvider(new LockContentProvider());
		viewer.setLabelProvider(new LockLabelProvider());
		viewer.setInput(root);
	}

	public void setInput(long time){
		this.time = time;
		load();
	}
	
	private void load() {
		root.clear();
		tree.removeAll();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId);
				Pack p = null;
				try {
					MapPack param = new MapPack();
					param.put("key", StatusConstants.LOCK_INFO);
					param.put("objHash", objHash);
					param.put("time", time);
					p = tcpProxy.getSingle(RequestCmd.STATUS_AROUND_VALUE, param);
				} catch (Throwable th){
					th.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcpProxy);
				}
				if (p != null) {
					String date = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
					StatusPack sp = (StatusPack) p;
					ListValue waitStartedLv = sp.data.getList("WAIT_STARTED");
					ListValue waitingPidLv = sp.data.getList("WAITING_PID");
					ListValue lockedTableLv = sp.data.getList("LOCKED_TABLE");
					ListValue lockedIndexLv = sp.data.getList("LOCKED_INDEX");
					ListValue lockedTypeLv = sp.data.getList("LOCKED_TYPE");
					ListValue blockingPidLv = sp.data.getList("BLOCKING_PID");
					ListValue waitingQueryLv = sp.data.getList("WAITING_QUERY");
					ListValue waitingLockModeLv = sp.data.getList("WAITING_LOCK_MODE");
					ListValue blockingQueryLv = sp.data.getList("BLOCKING_QUERY");
					ListValue blockingTrxStartedLv = sp.data.getList("BLOCKING_TRX_STARTED");
					ListValue blockingLockModeLv = sp.data.getList("BLOCKING_LOCK_MODE");
					for (int i = 0; i < waitStartedLv.size(); i++) {
						long blockPid = blockingPidLv.getLong(i);
						LockObject blockObject = root.get(blockPid);
						if (blockObject == null) {
							blockObject = new LockObject(blockPid);
							blockObject.startTime = blockingTrxStartedLv.getLong(i);
							blockObject.sql = TextProxy.maria.getLoadText(date, blockingQueryLv.getInt(i), serverId);
							blockObject.type = lockedTypeLv.getString(i);
							blockObject.waitingLockMode = waitingLockModeLv.getString(i);
							blockObject.blockingLockMode = blockingLockModeLv.getString(i);
							blockObject.index = lockedIndexLv.getString(i);
							blockObject.table = lockedTableLv.getString(i);
							root.put(blockPid, blockObject);
						}
						
						long waitPid = waitingPidLv.getLong(i);
						LockObject waitObject = new LockObject(waitPid);
						waitObject.startTime = waitStartedLv.getLong(i);
						waitObject.sql = TextProxy.maria.getLoadText(date, waitingQueryLv.getInt(i), serverId);
						waitObject.type = lockedTypeLv.getString(i);
						waitObject.waitingLockMode = waitingLockModeLv.getString(i);
						waitObject.blockingLockMode = blockingLockModeLv.getString(i);
						waitObject.index = lockedIndexLv.getString(i);
						waitObject.table = lockedTableLv.getString(i);
						waitObject.parent = blockObject;
						blockObject.addChild(waitObject);
					}
				}
				ExUtil.exec(tree, new Runnable() {
					public void run() {
						viewer.refresh();
						viewer.expandAll();
					}
				});
			}
		});
	}
	
	public void setFocus() {
		
	}
	
	static class LockObject {
		long pid;
		long startTime;
		String sql;
		String type;
		String waitingLockMode;
		String blockingLockMode;
		String index;
		String table;
		LockObject parent;
		List<LockObject> childList;
		
		LockObject(long id) {
			this.pid = id;
		}
		
		public void addChild(LockObject child) {
			if (childList == null) {
				childList = new ArrayList<LockObject>();
			}
			childList.add(child);
		}
	}
	
	class LockContentProvider implements ITreeContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@SuppressWarnings("rawtypes")
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Map) {
				return ((Map)inputElement).values().toArray();
			}
			return new Object[0];
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof LockObject) {
				if (((LockObject)parentElement).childList != null) {
					return ((LockObject)parentElement).childList.toArray();
				}
			}
			return null;
		}

		public Object getParent(Object element) {
			if (element instanceof LockObject) {
				return ((LockObject)element).parent;
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof LockObject) {
				return ((LockObject)element).childList != null;
			}
			return false;
		}
		
	}
	
	class LockLabelProvider implements ITableLabelProvider {

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof LockObject) {
				LockObject model = (LockObject) element;
				LockTableSchema column = columnList.get(columnIndex);
				switch (column) {
					case PID:
						return CastUtil.cString(model.pid);
					case START:
						return DateUtil.format(model.startTime, "HH:mm:ss");
					case SQL:
						return model.sql;
					case TYPE:
						return model.type;
					case MODE:
						if (model.parent == null) {
							return model.blockingLockMode;
						} else {
							return model.waitingLockMode;
						}
					case INDEX:
						return model.index;
					case TABLE:
						return model.table;
				}
			}
			return null;
		}
		
	}
	
	ArrayList<LockTableSchema> columnList = new ArrayList<LockTableSchema>();

	private void createColumns() {
		columnList.clear();
		for (LockTableSchema column : LockTableSchema.values()) {
			createTreeViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), true, true, column.isNumber());
			columnList.add(column);
		}
	}
	
	private TreeViewerColumn createTreeViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
		final TreeColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		columnLayout.setColumnData(column, new ColumnPixelData(width, resizable));
		return viewerColumn;
	}
	
	enum LockTableSchema {
		PID("PID", 100, SWT.LEFT, true),
	    START("Start", 100, SWT.CENTER, false),
	    SQL("SQL", 200, SWT.LEFT, false),
	    TYPE("Type", 100, SWT.LEFT, false),
	    MODE("Mode", 100, SWT.LEFT, false),
	    INDEX("Index", 100, SWT.LEFT, true),
	    TABLE("Table", 100, SWT.LEFT,  false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean isNumber;

	    private LockTableSchema(String text, int width, int alignment, boolean isNumber) {
	        this.title = text;
	        this.width = width;
	        this.alignment = alignment;
	        this.isNumber = isNumber;
	    }
	    
	    public String getTitle(){
	        return title;
	    }

	    public int getAlignment(){
	        return alignment;
	    }

		public int getWidth() {
			return width;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
	}

}
