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
package scouter.client.maria.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.AgentDailyListProxy;
import scouter.client.model.DigestModel;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.TextProxy;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.popup.DigestDetailDialog;
import scouter.client.sorter.TreeLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.io.DataInputX;
import scouter.lang.DigestKey;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.lang.pack.StatusPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class DigestTableView extends ViewPart implements Refreshable {
	
	public final static String ID = DigestTableView.class.getName();

	double PICO = Math.pow(10, -12);
	int serverId;
	
	Composite parent;
	TreeViewer viewer;
	TreeColumnLayout columnLayout;
	
	AgentDailyListProxy agentProxy = new AgentDailyListProxy();
	
	RefreshThread thread;
	boolean isAutoRefresh = false;
	
	String date;
	long stime, etime;
	HashMap<Integer, DigestModel> root = new HashMap<Integer, DigestModel>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		this.serverId = CastUtil.cint(secId);
	}

	public void createPartControl(Composite parent) {
		this.parent = parent;
		GridLayout gridlayout = new GridLayout(1, false);
		gridlayout.marginHeight = 0;
		gridlayout.horizontalSpacing = 0;
		gridlayout.marginWidth = 0;
		parent.setLayout(gridlayout);
		columnLayout = new TreeColumnLayout();
		Composite mainComp = new Composite(parent, SWT.NONE);
		mainComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainComp.setLayout(columnLayout);
		viewer = new TreeViewer(mainComp, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		createColumns();
		final Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		viewer.setLabelProvider(new TreeLabelProvider());
	    viewer.setContentProvider(new TreeContentProvider());
	    viewer.setComparator(new TreeLabelSorter(viewer));
	    viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof DigestModel) {
					DigestModel model = (DigestModel) o;
					new DigestDetailDialog().show(model, stime, etime, serverId);
				}
			}
		});
	    viewer.setInput(root);
	    
	    IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
	    Action actAutoRefresh = new Action("Auto Refresh in 10 sec.", IAction.AS_CHECK_BOX){ 
	        public void run() {
	        	isAutoRefresh = isChecked();
	        	if (isAutoRefresh) {
	        		thread.interrupt();
	        	}
	        }
	    };
	    actAutoRefresh.setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh_auto));
	    man.add(actAutoRefresh);
	    
	    long now = TimeUtil.getCurrentTime(serverId);
	    date = DateUtil.yyyymmdd(now);
	    stime = now - DateUtil.MILLIS_PER_FIVE_MINUTE;
	    etime = now;
	    loadQueryJob.schedule(2000);
	    
	    thread = new RefreshThread(this, 10000);
		thread.start();
	}
	
	public void refresh() {
		if (isAutoRefresh) {
			root.clear();
			long now = TimeUtil.getCurrentTime(serverId);
		    date = DateUtil.yyyymmdd(now);
		    stime = now - DateUtil.MILLIS_PER_FIVE_MINUTE;
		    etime = now;
		    loadQueryJob.schedule();
		}
	}
	
	public void setInput(long stime, long etime) {
		if (loadQueryJob.getState() == Job.WAITING || loadQueryJob.getState() == Job.RUNNING) {
			MessageDialog.openInformation(null, "STOP", "Previous loading is not yet finished");
			return;
		}
		if (etime - stime < DateUtil.MILLIS_PER_MINUTE) {
			stime = etime - DateUtil.MILLIS_PER_MINUTE;
		}
		root.clear();
		this.stime = stime;
		this.etime = etime;
		this.date = DateUtil.yyyymmdd(stime);
		loadQueryJob.schedule();
	}
	
	ArrayList<DigestSchema> columnList = new ArrayList<DigestSchema>();

	private void createColumns() {
		columnList.clear();
		for (DigestSchema column : DigestSchema.values()) {
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
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TreeLabelSorter sorter = (TreeLabelSorter) viewer.getComparator();
				TreeColumn selectedColumn = (TreeColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}

	public void setFocus() {
		
	}
	
	Job loadQueryJob = new Job("Load Digest List...") {
		
		HashMap<DigestKey, MapPack> summaryMap = new HashMap<DigestKey, MapPack>();
		HashMap<Integer, StatusPack> firstStatusMap = new HashMap<Integer, StatusPack>();
		HashMap<Integer, StatusPack> lastStatusMap = new HashMap<Integer, StatusPack>();
		
		protected IStatus run(final IProgressMonitor monitor) {
			summaryMap.clear();
			firstStatusMap.clear();
			lastStatusMap.clear();
			monitor.beginTask(DateUtil.hhmmss(stime) + " ~ " + DateUtil.hhmmss(etime), 100);
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				ListValue objHashLv = agentProxy.getObjHashLv(date, serverId, CounterConstants.MARIA_PLUGIN);
				if (objHashLv.size() > 0) {
					param.put("objHash", objHashLv);
					param.put("date", date);
					param.put("time", stime);
					List<Pack> firstList = tcp.process(RequestCmd.DB_LAST_DIGEST_TABLE, param);
					for (Pack p : firstList) {
						StatusPack s = (StatusPack) p;
						firstStatusMap.put(s.objHash, s);
					}
					param.put("stime", stime);
					param.put("etime", etime);
					tcp.process(RequestCmd.DB_DIGEST_TABLE, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							Pack p = in.readPack();
							switch (p.getPackType()) {
								case PackEnum.MAP:
									MapPack m = (MapPack) p;
									if (m.containsKey("percent")) {
										monitor.worked(m.getInt("percent"));
									} else {
										int objHash = m.getInt("objHash");
										int digestHash = m.getInt("digestHash");
										summaryMap.put(new DigestKey(objHash, digestHash), m);
									}
									break;
								case PackEnum.PERF_STATUS:
									StatusPack sp = (StatusPack) p;
									lastStatusMap.put(sp.objHash, sp);
									break;
							}
						}
					});
				}
			} catch (Exception e) {
				ConsoleProxy.errorSafe(e.toString());
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			Iterator<Integer> itr = lastStatusMap.keySet().iterator();
			while (itr.hasNext()) {
				int objHash = itr.next();
				StatusPack firstStatus = firstStatusMap.get(objHash);
				StatusPack lastStatus = lastStatusMap.get(objHash);
				HashMap<Integer, MapValue> firstMap = new HashMap<Integer, MapValue>();
				if (firstStatus == null) {
					// nothing
				} else { 
					// index first values for delta
					MapValue firstData = firstStatus.data;
					ListValue firstDigestLv = firstData.getList("DIGEST_TEXT");
					for (int i = 0; i < firstDigestLv.size(); i++) {
						int digestHash = firstDigestLv.getInt(i);
						MapValue valueMap = new MapValue();
						Enumeration<String> keys = firstData.keys();
						while (keys.hasMoreElements()) {
							String key = keys.nextElement();
							valueMap.put(key, firstData.getList(key).get(i));
						}
						firstMap.put(digestHash, valueMap);
					}
				}
				MapValue data = lastStatus.data;
				ListValue digestLv = data.getList("DIGEST_TEXT");
				ListValue schemaNameLv = data.getList("SCHEMA_NAME");
				ListValue executionLv = data.getList("COUNT_STAR");
				ListValue timerWaitLv = data.getList("SUM_TIMER_WAIT");
				ListValue lockTimeLv = data.getList("SUM_LOCK_TIME");
				ListValue errorsLv = data.getList("SUM_ERRORS");
				ListValue warnsLv = data.getList("SUM_WARNINGS");
				ListValue rowsAffectedLv = data.getList("SUM_ROWS_AFFECTED");
				ListValue rowsSentLv = data.getList("SUM_ROWS_SENT");
				ListValue rowsExaminedLv = data.getList("SUM_ROWS_EXAMINED");
				ListValue createdTmpDiskTablesLv = data.getList("SUM_CREATED_TMP_DISK_TABLES");
				ListValue createdTmpTablesLv = data.getList("SUM_CREATED_TMP_TABLES");
				ListValue selectFullJoin = data.getList("SUM_SELECT_FULL_JOIN");
				ListValue selectFullRangeJoin = data.getList("SUM_SELECT_FULL_RANGE_JOIN");
				ListValue selectRangeLv = data.getList("SUM_SELECT_RANGE");
				ListValue selectRangeCheckLv = data.getList("SUM_SELECT_RANGE_CHECK");
				ListValue selectScanLv = data.getList("SUM_SELECT_SCAN");
				ListValue sortMergePassesLv = data.getList("SUM_SORT_MERGE_PASSES");
				ListValue sortRangeLv = data.getList("SUM_SORT_RANGE");
				ListValue sortRowsLv = data.getList("SUM_SORT_ROWS");
				ListValue sortScanLv = data.getList("SUM_SORT_SCAN");
				ListValue noIndexUsedLv = data.getList("SUM_NO_INDEX_USED");
				ListValue noGoodIndexUsedLv = data.getList("SUM_NO_GOOD_INDEX_USED");
				ListValue firstSeenLv = data.getList("FIRST_SEEN");
				ListValue lastSeenLv = data.getList("LAST_SEEN");
				
				for (int i = 0; i < digestLv.size(); i++) {
					if (lastSeenLv.getLong(i) < stime || lastSeenLv.getLong(i) > etime) {
						continue;
					}
					DigestModel model = new DigestModel();
					int digestHash = digestLv.getInt(i);
					MapPack m = summaryMap.get(new DigestKey(objHash, digestHash));
					if (m == null) continue;
					long maxTimerWait = m.getLong("MAX_TIMER_WAIT");
					long minTimerWait = m.getLong("MIN_TIMER_WAIT");
					long avgTimerWait = m.getLong("AVG_TIMER_WAIT");
					int count = m.getInt("count");
					model.objHash = objHash;
					model.digestHash = digestHash;
					model.name = TextProxy.object.getLoadText(date, objHash, serverId);
					model.database = TextProxy.maria.getLoadText(date, schemaNameLv.getInt(i), serverId);
					model.firstSeen = firstSeenLv.getLong(i);
					model.lastSeen = lastSeenLv.getLong(i);
					model.avgResponseTime = avgTimerWait / (double) count;
					model.minResponseTime = minTimerWait;
					model.maxResponseTime = maxTimerWait;
					
					MapValue firstValue = firstMap.get(digestHash);
					if (firstValue == null) {
						firstValue = new MapValue();
					}
					model.execution = executionLv.getInt(i) - firstValue.getInt("COUNT_STAR");
					if (model.execution < 1) {
						System.out.println("first=>" + firstStatus);
						System.out.println("last =>" + lastStatus);
					}
					model.errorCnt = errorsLv.getInt(i) - firstValue.getInt("SUM_ERRORS");
					model.warnCnt = warnsLv.getInt(i) - firstValue.getInt("SUM_WARNINGS");
					model.sumResponseTime = timerWaitLv.getLong(i) - firstValue.getLong("SUM_TIMER_WAIT");
					model.lockTime = lockTimeLv.getLong(i) - firstValue.getLong("SUM_LOCK_TIME");
					model.rowsAffected = rowsAffectedLv.getLong(i) - firstValue.getLong("SUM_ROWS_AFFECTED");
					model.rowsSent = rowsSentLv.getLong(i) - firstValue.getLong("SUM_ROWS_SENT");
					model.rowsExamined = rowsExaminedLv.getLong(i) - firstValue.getLong("SUM_ROWS_EXAMINED");
					model.createdTmpDiskTables = createdTmpDiskTablesLv.getLong(i) - firstValue.getLong("SUM_CREATED_TMP_DISK_TABLES");
					model.createdTmpTables = createdTmpTablesLv.getLong(i) - firstValue.getLong("SUM_CREATED_TMP_TABLES");
					model.selectFullJoin = selectFullJoin.getLong(i) - firstValue.getLong("SUM_SELECT_FULL_JOIN");
					model.selectFullRangeJoin = selectFullRangeJoin.getLong(i) - firstValue.getLong("SUM_SELECT_FULL_RANGE_JOIN");
					model.selectRange = selectRangeLv.getLong(i) - firstValue.getLong("SUM_SELECT_RANGE");
					model.selectRangeCheck = selectRangeCheckLv.getLong(i) - firstValue.getLong("SUM_SELECT_RANGE_CHECK");
					model.selectScan = selectScanLv.getLong(i) - firstValue.getLong("SUM_SELECT_SCAN");
					model.sortMergePasses = sortMergePassesLv.getLong(i) - firstValue.getLong("SUM_SORT_MERGE_PASSES");
					model.sortRange =sortRangeLv.getLong(i) - firstValue.getLong("SUM_SORT_RANGE");
					model.sortRows = sortRowsLv.getLong(i) - firstValue.getLong("SUM_SORT_ROWS");
					model.sortScan = sortScanLv.getLong(i) - firstValue.getLong("SUM_SORT_SCAN");
					model.noIndexUsed = noIndexUsedLv.getLong(i) - firstValue.getLong("SUM_NO_INDEX_USED");
					model.noGoodIndexUsed = noGoodIndexUsedLv.getLong(i) - firstValue.getLong("SUM_NO_GOOD_INDEX_USED");

					DigestModel parent = root.get(digestHash);
					if (parent == null) {
						parent = new DigestModel();
						parent.digestHash = digestHash;
						String digestTxt = TextProxy.maria.getLoadText(date, digestHash, serverId);
						parent.name = digestTxt == null ? "unknown hash" : digestTxt;
						root.put(digestHash, parent);
					}
					model.parent = parent;
					parent.addChild(model);
				}
			}
			Iterator<DigestModel> parents = root.values().iterator(); 
			while (parents.hasNext()) {
				DigestModel parent = parents.next();
				DigestModel[] childs = parent.getChildArray();
				if (childs != null) {
					double sumAvg = 0.0d;
					for (DigestModel child : childs) {
						sumAvg += child.avgResponseTime;
					}
					parent.avgResponseTime = sumAvg / childs.length;
				}
			}
			monitor.done();
			ExUtil.exec(viewer.getTree(), new Runnable() {
				public void run() {
					DigestTableView.this.setContentDescription(DateUtil.format(stime, "MM-dd HH:mm:ss") + " ~ " + DateUtil.format(etime, "MM-dd HH:mm:ss") + " (" + root.size() + ")");
					viewer.refresh();
				}
			});
			return Status.OK_STATUS;
		}
	};
	
	class TreeContentProvider implements ITreeContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof DigestModel) {
				DigestModel parent = (DigestModel) parentElement;
				Object[] array = parent.getChildArray();
				if(array != null) return array;
			}
			return new Object[0];
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof HashMap) {
				HashMap map = (HashMap) inputElement;
				Object[] objArray = new Object[map.size()];
				Iterator itr = map.values().iterator();
				int cnt = 0;
				while (itr.hasNext()) {
					objArray[cnt] = itr.next();
					cnt++;
				}
				
				return objArray;
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof DigestModel) {
				return ((DigestModel) element).parent;
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof DigestModel) {
				return ((DigestModel) element).getChildArray() != null;
			}
			return false;
		}
	}
	
	class TreeLabelProvider implements ITableLabelProvider {

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
			if (element instanceof DigestModel) {
				DigestModel model = (DigestModel) element;
				DigestSchema column = columnList.get(columnIndex);
				switch (column) {
					case DIGEST_TEXT:
						return model.name;
					case SCHEMA_NAME:
						return model.database;
					case COUNT_STAR:
						return FormatUtil.print(model.execution, "#,##0");
					case SUM_ERRORS:
						return FormatUtil.print(model.errorCnt, "#,##0");
					case SUM_WARNINGS:
						return FormatUtil.print(model.warnCnt, "#,##0");
					case SUM_TIMER_WAIT:
						return FormatUtil.print(model.sumResponseTime * PICO, "#,##0.00#");
					case AVG_TIMER_WAIT:
						return FormatUtil.print(model.avgResponseTime * PICO, "#,##0.00#");
					case MIN_TIMER_WAIT:
						return FormatUtil.print(model.minResponseTime * PICO, "#,##0.00#");
					case MAX_TIMER_WAIT:
						return FormatUtil.print(model.maxResponseTime * PICO, "#,##0.00#");
					case SUM_LOCK_TIME:
						return FormatUtil.print(model.lockTime  * PICO, "#,##0.00#");
					case SUM_ROWS_AFFECTED:
						return FormatUtil.print(model.rowsAffected, "#,##0");
					case SUM_ROWS_SENT:
						return FormatUtil.print(model.rowsSent, "#,##0");
					case SUM_ROWS_EXAMINED:
						return FormatUtil.print(model.rowsExamined, "#,##0");
					case SUM_CREATED_TMP_DISK_TABLES:
						return FormatUtil.print(model.createdTmpDiskTables, "#,##0");
					case SUM_CREATED_TMP_TABLES:
						return FormatUtil.print(model.createdTmpTables, "#,##0");
					case SUM_SELECT_FULL_JOIN:
						return FormatUtil.print(model.selectFullJoin, "#,##0");
					case SUM_SELECT_FULL_RANGE_JOIN:
						return FormatUtil.print(model.selectFullRangeJoin, "#,##0");
					case SUM_SELECT_RANGE:
						return FormatUtil.print(model.selectRange, "#,##0");
					case SUM_SELECT_RANGE_CHECK:
						return FormatUtil.print(model.selectRangeCheck, "#,##0");
					case SUM_SELECT_SCAN:
						return FormatUtil.print(model.selectScan, "#,##0");
					case SUM_SORT_MERGE_PASSES:
						return FormatUtil.print(model.sortMergePasses, "#,##0");
					case SUM_SORT_RANGE:
						return FormatUtil.print(model.sortRange, "#,##0");
					case SUM_SORT_ROWS:
						return FormatUtil.print(model.sortRows, "#,##0");
					case SUM_SORT_SCAN:
						return FormatUtil.print(model.sortScan, "#,##0");
					case SUM_NO_INDEX_USED:
						return FormatUtil.print(model.noIndexUsed, "#,##0");
					case SUM_NO_GOOD_INDEX_USED:
						return FormatUtil.print(model.noGoodIndexUsed, "#,##0");
					case FIRST_SEEN:
						return DateUtil.timestamp(model.firstSeen);
					case LAST_SEEN:
						return DateUtil.timestamp(model.lastSeen);
				}
			}
			return null;
		}
	}
	
	public void dispose() {
		super.dispose();
		if (loadQueryJob != null && (loadQueryJob.getState() == Job.WAITING || loadQueryJob.getState() == Job.RUNNING)) {
			loadQueryJob.cancel();
		}
	}
	
	public static void main(String[] args) {
		System.out.println(139871834183L * Math.pow(10, -12));
	}
}
