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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
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

public class ObjectClassListView extends ViewPart {
	
	public static final String ID = ObjectClassListView.class.getName();
	
	private int objHash;
	private int currentPage = 1;
	private int totalPage = 1;
	
	private Text filterText;
	private Button leftButton;
	private Button rightButton;
	
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
		this.setPartName("Loaded Class List[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "]");
		initialLayout(parent);
		clipboard = new Clipboard(null);
		load();
	}
	
	public void load() {
		final String filter = filterText.getText();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				MapPack out = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					if (StringUtil.isNotEmpty(filter)) {
						param.put("filter", filter);
					}
					param.put("page", currentPage);
					out = (MapPack) tcp.getSingle(RequestCmd.OBJECT_CLASS_LIST, param);
					if (out == null) {
						return;
					}
				} catch (Throwable t) {
					ConsoleProxy.errorSafe(t.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				currentPage = out.getInt("page");
				totalPage = out.getInt("totalPage");
				
				ListValue indexLv = out.getList("index");
				if (indexLv == null) {
					return;
				}
				ListValue nameLv = out.getList("name");
				ListValue typeLv = out.getList("type");
				ListValue superClassLv = out.getList("superClass");
				ListValue interfaceLv = out.getList("interfaces");
				ListValue resourceLv = out.getList("resource");
				
				int count = indexLv.size();
				final List<ClassData> classDataList = new ArrayList<ClassData>(count);
				
				for (int i = 0; i < count; i++) {
					ClassData data = new ClassData();
					data.index = indexLv.getLong(i);
					data.type = typeLv.getString(i);
					data.name = nameLv.getString(i);
					data.superClass = superClassLv.getString(i);
					data.interfaces = interfaceLv.getString(i);
					data.resources = resourceLv.getString(i);
					classDataList.add(data);
				}
				ExUtil.exec(tableViewer.getTable(), new Runnable() {
					public void run() {
						validatePageButton();
						tableViewer.setInput(classDataList);
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
					ClassData data = (ClassData) selection.getFirstElement();
					final String className = data.name;
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
											saveClassFile(className, bv);
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
					final ClassData data = (ClassData) selection.getFirstElement();
					final String resource = data.resources;
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
											if(MessageDialog.openQuestion(tableViewer.getTable().getShell(), data.name
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
		    		final ListValue classLv = new ListValue(); 
		    		Object[] datas = selection.toArray();
		    		for (int i = 0; i < datas.length; i++) {
		    			ClassData data = (ClassData) datas[i];
		    			classLv.add(data.name);
		    		}
					if(MessageDialog.openQuestion(tableViewer.getTable().getShell(), classLv.size() + " class(es) selected"
							, "Redefine class may affect this server.\nContinue?")) {
						new RedefineClassJob(classLv).schedule();
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
		GridLayout layout = new GridLayout(5, false);
		parentGroup.setLayout(layout);
		
		filterText = new Text(parentGroup, SWT.BORDER);
		GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		gridData.minimumWidth = 150;
		filterText.setLayoutData(gridData);
		
		final Button applyButton = new Button(parentGroup, SWT.PUSH);
		applyButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		applyButton.setImage(Images.filter);
		applyButton.setText("Apply Filter");
		applyButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				currentPage = 1;
				ExUtil.exec(new Runnable() {
					public void run() {
						load();
					}
				});
			}
		});
		
		filterText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					applyButton.notifyListeners(SWT.Selection, new Event());
				}
			}
		});
		
		leftButton = new Button(parentGroup, SWT.PUSH);
		leftButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		leftButton.setText("<");
		leftButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (currentPage > 1) {
					currentPage--;
					ExUtil.exec(new Runnable() {
						public void run() {
							load();
						}
					});
				}
			}
		});
		
        rightButton = new Button(parentGroup, SWT.PUSH);
        rightButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        rightButton.setText(">");
        rightButton.addListener(SWT.Selection, new Listener() {

			public void handleEvent(Event event) {
				if (currentPage < totalPage) {
					currentPage++;
					ExUtil.exec(new Runnable() {
						public void run() {
							load();
						}
					});
				}
			}
			
		});
        validatePageButton();
	}

	private void createColumns() {
		for (ClassColumnEnum column : ClassColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case NO:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ClassData) {
							return String.valueOf(((ClassData)element).index);
						}
						return null;
					}
				};
				break;
			case TYPE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ClassData) {
							return ((ClassData) element).type;
						}
						return null;
					}
				};
				break;
			case NAME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ClassData) {
							return ((ClassData) element).name;
						}
						return null;
					}
				};
				break;
			case SUPERCLASS:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ClassData) {
							return ((ClassData) element).superClass;
						}
						return null;
					}
				};
				break;
			case INTERFACES:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ClassData) {
							return ((ClassData) element).interfaces;
						}
						return null;
					}
				};
				break;
			case RESOURCE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ClassData) {
							return ((ClassData) element).resources;
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

	private void validatePageButton() {
		if (this.currentPage <= 1) {
			this.leftButton.setEnabled(false);
		} else {
			this.leftButton.setEnabled(true);
		}
		
		if (this.currentPage == this.totalPage) {
			this.rightButton.setEnabled(false);
		} else {
			this.rightButton.setEnabled(true);
		}
	}
	
	private void selectionCopyToClipboard() {
		if (tableViewer != null) {
			TableItem[] items = tableViewer.getTable().getSelection();
			if (items != null && items.length > 0) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < items.length; i++) {
					ClassData data = (ClassData) items[i].getData();
					sb.append(data.toString());
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
			}
		}
	}
	
	enum ClassColumnEnum {

		NO("No", 20, SWT.RIGHT, true, true, true),
	    TYPE("Type", 30, SWT.CENTER, true, true, false),
	    NAME("Name", 250, SWT.LEFT, true, true, false),
	    SUPERCLASS("SuperClass", 150, SWT.LEFT, true, true, false),
	    INTERFACES("Interfaces", 150, SWT.LEFT, true, true, false),
	    RESOURCE("Resources", 150, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private ClassColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	class ClassData {
		public long index;
		public String type;
		public String name;
		public String superClass;
		public String interfaces;
		public String resources;
		
		public String toString() {
			return index + "\t" + type + "\t" + name + "\t" + superClass + "\t" + interfaces + "\t" + resources + "\n";
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
		ClassData data = (ClassData) selection.getFirstElement();
		final String className = data.name;
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
