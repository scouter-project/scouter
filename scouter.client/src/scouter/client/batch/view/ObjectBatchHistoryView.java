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
package scouter.client.batch.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.popup.EditableMessageDialog;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.StringUtil;

public class ObjectBatchHistoryView extends ViewPart {
	
	public static final String ID = ObjectBatchHistoryView.class.getName();
	
	private int objHash;
	
	
	private DateTime date;
	private DateTime fromTime;
	private DateTime toTime;
	
	private Text searchText;
	private Text responseTimeText;
	
	private TableViewer tableViewer;
	private TableColumnLayout tableColumnLayout;
	
	private Clipboard clipboard;
	private int serverId;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objHash = CastUtil.cint(ids[1]);
	}

	public void createPartControl(Composite parent) {
		this.setPartName("Batch History List[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "]");
		initialLayout(parent);
		clipboard = new Clipboard(null);
		//search();
	}
	
	public void search() {
		final String search = searchText.getText();
		final String time = responseTimeText.getText();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				MapPack out = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					if (StringUtil.isNotEmpty(search)) {
						param.put("filter", search);
					}
					if (StringUtil.isNotEmpty(search)) {
						param.put("time", time);
					}					
					out = (MapPack) tcp.getSingle(RequestCmd.BATCH_HISTORY_LIST, param);
					if (out == null) {
						return;
					}
				} catch (Throwable t) {
					ConsoleProxy.errorSafe(t.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				ListValue indexLv = out.getList("index");
				if (indexLv == null) {
					return;
				}
				ListValue startTimeLv = out.getList("startTime");
				ListValue jobIdLv = out.getList("jobId");
				ListValue responseTimeLv = out.getList("reponseTime");
				ListValue sqlTimeLv = out.getList("sqlTime");
				ListValue sqlRunsLv = out.getList("sqlRuns");
				ListValue threadsLv = out.getList("threads");
				ListValue isStackLv = out.getList("isStack");
				ListValue positionLv = out.getList("position");
				
				int count = indexLv.size();
				final List<BatchData> batchDataList = new ArrayList<BatchData>(count);
				
				for (int i = 0; i < count; i++) {
					BatchData data = new BatchData();
					data.index = indexLv.getLong(i);
					data.startTime = startTimeLv.getLong(i);
					data.jobId = jobIdLv.getString(i);
					data.responseTime = responseTimeLv.getLong(i);
					data.sqlTime = sqlTimeLv.getLong(i);
					data.sqlRuns = sqlRunsLv.getLong(i);
					data.threads = threadsLv.getInt(i);
					data.isStack = isStackLv.getBoolean(i);
					data.position = positionLv.getLong(i);
					batchDataList.add(data);
				}
				ExUtil.exec(tableViewer.getTable(), new Runnable() {
					public void run() {
						tableViewer.setInput(batchDataList);
					}
				});
			}
		});
		
	}

	private void initialLayout(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		createUpperMenu(composite);
		Composite tableComposite = new Composite(composite, SWT.NONE);
		tableComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		tableComposite.setLayout(new GridLayout(1, true));
		createTableViewer(tableComposite);
	}

	private void createTableViewer(Composite composite) {
		tableViewer = new TableViewer(composite, SWT.MULTI  | SWT.FULL_SELECTION | SWT.BORDER);
		tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);
		createColumns();
		final Table table = tableViewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    createTableContextMenu();
	    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent arg0) {
				openDescription();
			}
		});
	    tableViewer.setContentProvider(new ArrayContentProvider());
	    tableViewer.setComparator(new ColumnLabelSorter(tableViewer));
	    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	    tableViewer.getControl().setLayoutData(gridData);
	}
	
	private void createTableContextMenu() {
		MenuManager manager = new MenuManager();
	    tableViewer.getControl().setMenu(manager.createContextMenu(tableViewer.getControl()));
	    manager.add(new Action("&Copy", ImageDescriptor.createFromImage(Images.copy)) {
			public void run() {
				selectionCopyToClipboard();
			}
	    }); 
	    Server server = ServerManager.getInstance().getServer(serverId);
	    if (server.isAllowAction(GroupPolicyConstants.ALLOW_EXPORTCLASS)) {
		    manager.add(new Action("&Export Class") {
		    	public void run() {
		    		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
					BatchData data = (BatchData) selection.getFirstElement();
					final String jobId = data.jobId;
					if (StringUtil.isEmpty(jobId)) {
						return;
					}
					ExUtil.asyncRun(new Runnable() {
						public void run() {
							TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
							MapPack p = null;
							try {
								MapPack param = new MapPack();
								param.put("objHash", objHash);
								param.put("class", jobId);
								p = (MapPack) tcp.getSingle(RequestCmd.OBJECT_LOAD_CLASS_BY_STREAM, param);
							} catch (Exception e) {
								ConsoleProxy.errorSafe(e.getMessage());
							} finally {
								TcpProxy.putTcpProxy(tcp);
							}
							if (p != null) {
								String error = CastUtil.cString(p.get("error"));
								if (StringUtil.isNotEmpty(error)) {
									ConsoleProxy.errorSafe(error);
								}
								Value v = p.get("class");
								if (v != null) {
									final BlobValue bv = (BlobValue) v;
									ExUtil.exec(tableViewer.getTable(), new Runnable() {
										public void run() {
											saveClassFile(jobId, bv);
										}
									});
								}
							}
						}
					});
				}
		    });
		    manager.add(new Action("&Export Jar") {
		    	public void run() {
		    		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
					final BatchData data = (BatchData) selection.getFirstElement();
					final String resource = "" + data.isStack;
					if (StringUtil.isEmpty(resource)) {
						return;
					}
					ExUtil.asyncRun(new Runnable() {
						public void run() {
							TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
							MapPack p = null;
							try {
								MapPack param = new MapPack();
								param.put("objHash", objHash);
								param.put("resource", resource);
								p = (MapPack) tcp.getSingle(RequestCmd.OBJECT_CHECK_RESOURCE_FILE, param);
							} catch (Exception e) {
								ConsoleProxy.errorSafe(e.getMessage());
							} finally {
								TcpProxy.putTcpProxy(tcp);
							}
							if (p != null) {
								String error = p.getText("error");
								if (StringUtil.isNotEmpty(error)) {
									ConsoleProxy.errorSafe(error);
								} else {
									final String name = p.getText("name");
									final long size = p.getLong("size");
									ExUtil.exec(tableViewer.getTable(), new Runnable() {
										public void run() {
											if(MessageDialog.openQuestion(tableViewer.getTable().getShell(), data.jobId
													, name + "(" + ScouterUtil.humanReadableByteCount(size, true) +") will be downloaded.\nContinue?")) {
												Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()	.getShell();
												FileDialog dialog = new FileDialog(shell, SWT.SAVE);
												dialog.setOverwrite(true);
												dialog.setFileName(name);
												dialog.setFilterExtensions(new String[] { "*.jar", "*.*" });
												dialog.setFilterNames(new String[] { "Jar File(*.jar)", "All Files" });
												String fileSelected = dialog.open();
												if (fileSelected != null) {
													new DownloadJarFileJob(name, resource, fileSelected).schedule();
												}
											}
										}
									});
								}
							}
						}
					});
				}
		    });
	    }
	    
	    manager.add(new Action("&Description") {
	    	public void run() {
	    		openDescription();
	    	}
	    });
	    manager.add(new Separator());
	    
	    if (server.isAllowAction(GroupPolicyConstants.ALLOW_REDEFINECLASS)) {
		    manager.add(new Action("&Redefine class") {
		    	public void run() {
		    		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
		    		final ListValue jobIdLv = new ListValue(); 
		    		Object[] datas = selection.toArray();
		    		for (int i = 0; i < datas.length; i++) {
		    			BatchData data = (BatchData) datas[i];
		    			jobIdLv.add(data.jobId);
		    		}
					if(MessageDialog.openQuestion(tableViewer.getTable().getShell(), jobIdLv.size() + " class(es) selected"
							, "Redefine class may affect this server.\nContinue?")) {
						new RedefineClassJob(jobIdLv).schedule();
					}
				}
		    });
	    }
	    tableViewer.getTable().addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(Event e) {
				if(e.stateMask == SWT.CTRL){
					if (e.keyCode == 'c' || e.keyCode == 'C') {
						selectionCopyToClipboard();
					}					
				}
			}
		});
	}
	
	public void saveClassFile(String className, final BlobValue bv) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()	.getShell();
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setOverwrite(true);
		dialog.setFileName(className + ".class");
		dialog.setFilterExtensions(new String[] { "*.class", "*.*" });
		dialog.setFilterNames(new String[] { "Class File(*.class)", "All Files" });
		final String fileSelected = dialog.open();
		if (fileSelected != null) {
			ExUtil.asyncRun("Decompile-" + className + TimeUtil.getCurrentTime(serverId), new Runnable() {
				public void run() {
					FileUtil.save(fileSelected, bv.value);
					ConsoleProxy.infoSafe(fileSelected + " saved.");
				}
			});
		}
	}
	
	private void createUpperMenu(Composite composite) {
		Group parentGroup = new Group(composite, SWT.NONE);
		parentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout layout = new GridLayout(9, false);
		parentGroup.setLayout(layout);
		
		date = new DateTime(parentGroup, SWT.DATE);
		fromTime = new DateTime(parentGroup, SWT.TIME);
		fromTime.setTime(0, 0, 0);
		Label label = new Label(parentGroup, SWT.CENTER);
		label.setText(" ~ ");
		toTime = new DateTime(parentGroup, SWT.TIME);
		toTime.setTime(23, 59, 59);
	
		label = new Label(parentGroup, SWT.RIGHT);
		label.setText("Job ID");
		searchText = new Text(parentGroup, SWT.LEFT | SWT.BORDER);
		GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gridData.minimumWidth = 150;
		searchText.setLayoutData(gridData);

		label = new Label(parentGroup, SWT.RIGHT);
		label.setText("Min Response Time");
		responseTimeText = new Text(parentGroup, SWT.LEFT | SWT.BORDER);
		gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gridData.minimumWidth = 80;
		responseTimeText.setLayoutData(gridData);
	
		final Button applyButton = new Button(parentGroup, SWT.PUSH);
		applyButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		applyButton.setImage(Images.filter);
		applyButton.setText("Search");
		applyButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				ExUtil.exec(new Runnable() {
					public void run() {
						search();
					}
				});
			}
		});
		
		searchText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					applyButton.notifyListeners(SWT.Selection, new Event());
				}
			}
		});
	}

	private void createColumns() {
		for (BatchColumnEnum column : BatchColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case NO:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return String.valueOf(((BatchData)element).index);
						}
						return null;
					}
				};
				break;
			case TIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return DateUtil.yyyymmdd(((BatchData) element).startTime) + " " + DateUtil.hhmmss(((BatchData) element).startTime);
						}
						return null;
					}
				};
				break;
			case JOBID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return ((BatchData) element).jobId;
						}
						return null;
					}
				};
				break;
			case RESPONSETIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return "" + ((BatchData) element).responseTime;
						}
						return null;
					}
				};
				break;
			case SQLTIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return "" + ((BatchData) element).sqlTime;
						}
						return null;
					}
				};
				break;
			case SQLRUNS:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return "" + ((BatchData) element).sqlRuns;
						}
						return null;
					}
				};
				break;
			case THREADS:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return "" + ((BatchData) element).threads;
						}
						return null;
					}
				};
				break;
			case STACK:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchData) {
							return ((BatchData) element).isStack?"O":"";
						}
						return null;
					}
				};
				break;
			}
			if (labelProvider != null) {
				c.setLabelProvider(labelProvider);
			}
		}
	}
	
	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(width, width, resizable));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) tableViewer.getComparator();
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}

	public void setFocus() {
	}
	
	private void selectionCopyToClipboard() {
		if (tableViewer != null) {
			TableItem[] items = tableViewer.getTable().getSelection();
			if (items != null && items.length > 0) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < items.length; i++) {
					BatchData data = (BatchData) items[i].getData();
					sb.append(data.toString());
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
			}
		}
	}
	
	enum BatchColumnEnum {
		NO("No", 60, SWT.RIGHT, true, true, true),
	    TIME("Time", 100, SWT.CENTER, true, true, false),
	    JOBID("Job ID", 100, SWT.LEFT, true, true, false),
	    RESPONSETIME("Response Time", 120, SWT.RIGHT, true, true, false),
	    SQLTIME("SQL Time", 120, SWT.RIGHT, true, true, false),
	    SQLRUNS("SQL Runs", 120, SWT.RIGHT, true, true, false),
	    THREADS("Threads", 100, SWT.RIGHT, true, true, false),
	    STACK("Stack", 50, SWT.CENTER, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private BatchColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
	        this.title = text;
	        this.width = width;
	        this.alignment = alignment;
	        this.resizable = resizable;
	        this.moveable = moveable;
	        this.isNumber = isNumber;
	    }
	    
	    public String getTitle(){
	        return title;
	    }

	    public int getAlignment(){
	        return alignment;
	    }

	    public boolean isResizable(){
	        return resizable;
	    }

	    public boolean isMoveable(){
	        return moveable;
	    }

		public int getWidth() {
			return width;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
	}
	
	class BatchData {
		public long index;
		public long startTime;
		public String jobId;
		public long responseTime;
		public long sqlTime;
		public long sqlRuns;
		public int threads;
		public boolean isStack;
		public long position;
		
		public String toString() {
			return new StringBuilder(100).append(index).append('\t').append(DateUtil.yyyymmdd(startTime)).append(' ').append(DateUtil.hhmmss(startTime)).append('\t').append(jobId).append('\t').append(responseTime).append('\t').append(sqlTime).append('\t').append(sqlRuns).append('\t').append(threads).append('\t').append(isStack).append('\n').toString();
		}
	}
	
	class RedefineClassJob extends Job {
		
		ListValue classLv;

		public RedefineClassJob(ListValue classLv) {
			super("Redefine.... " + classLv.size() + " classes.....");
			this.classLv = classLv;
		}

		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Redefine class", IProgressMonitor.UNKNOWN);
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			MapPack p = null;
			try {
				MapPack param = new MapPack();
				param.put("objHash", objHash);
				param.put("class", classLv);
				p = (MapPack) tcp.getSingle(RequestCmd.REDEFINE_CLASSES, param);
			} catch(Exception e) {
				ConsoleProxy.errorSafe(e.getMessage());
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			if (p != null) {
				boolean success = p.getBoolean("success");
				if (success) {
					ConsoleProxy.infoSafe("Redefine complete");
				} else {
					String error = p.getText("error");
					if (StringUtil.isNotEmpty(error)) {
						ConsoleProxy.errorSafe(error);
					} else {
						ConsoleProxy.errorSafe("Redefine failed.");
					}
				}
			}
			return Status.CANCEL_STATUS;
		}
		
	}
	
	class DownloadJarFileJob extends Job {
		
		String resource;
		String saveFile;

		public DownloadJarFileJob(String name, String resource, String saveFile) {
			super("Download...." + name);
			this.resource = resource;
			this.saveFile = saveFile;
		}

		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Downloading resource", IProgressMonitor.UNKNOWN);
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			MapPack p = null;
			try {
				MapPack param = new MapPack();
				param.put("objHash", objHash);
				param.put("resource", resource);
				p = (MapPack) tcp.getSingle(RequestCmd.OBJECT_DOWNLOAD_JAR, param);
			} catch(Exception e) {
				ConsoleProxy.errorSafe(e.getMessage());
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			if (p != null) {
				String error = p.getText("error");
				if (StringUtil.isNotEmpty(error)) {
					ConsoleProxy.errorSafe(error);
				} else {
					Value v = p.get("jar");
					if (v != null) {
						BlobValue bv = (BlobValue) v;
						FileUtil.save(saveFile, bv.value);
						return Status.OK_STATUS;
					}
				}
			}
			return Status.CANCEL_STATUS;
		}
		
	}
	
	private void openDescription() {
		StructuredSelection selection = (StructuredSelection) tableViewer.getSelection();
		BatchData data = (BatchData) selection.getFirstElement();
		final String className = data.jobId;
		if (StringUtil.isEmpty(className)) {
			return;
		}
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				MapPack p = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("class", className);
					p = (MapPack) tcp.getSingle(RequestCmd.OBJECT_CLASS_DESC, param);
				} catch (Exception e) {
					ConsoleProxy.errorSafe(e.getMessage());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (p != null) {
					final String error = CastUtil.cString(p.get("error"));
					final Value v = p.get("class");
					ExUtil.exec(tableViewer.getTable(), new Runnable() {
						public void run() {
							if (StringUtil.isNotEmpty(error)) {
								new EditableMessageDialog().show("ERROR", error);
								return;
							}
							new EditableMessageDialog().show(className, CastUtil.cString(v));
						}
					});
				}
			}
		});
	}
}
