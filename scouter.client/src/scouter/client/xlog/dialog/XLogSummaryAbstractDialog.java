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
package scouter.client.xlog.dialog;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import scouter.client.Images;
import scouter.client.model.XLogData;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.LongKeyLinkedMap;
import au.com.bytecode.opencsv.CSVWriter;

public abstract class XLogSummaryAbstractDialog {
	
	Display display;
	Shell dialog;
	protected LongKeyLinkedMap<XLogData> dataMap;
	protected Label rangeLabel;
	long stime, etime;
	protected TableViewer viewer;
	TableColumnLayout tableColumnLayout;
	private Clipboard clipboard;

	public XLogSummaryAbstractDialog(Display display, LongKeyLinkedMap<XLogData> dataMap) {
		this.display = display;
		this.dataMap = dataMap;
	}
	
	public void setRange(long stime, long etime) {
		this.stime = stime;
		this.etime = etime;
	}
	
	public void show() {
		dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
		dialog.setText(getTitle());
		createDialogArea();
		clipboard = new Clipboard(null);
		dialog.pack();
		dialog.open();
	}
	
	abstract public String getTitle();
	
	private void copyToClipboard(TableItem[] items) {
		if (viewer != null) {
			int colCnt = viewer.getTable().getColumnCount();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < colCnt; i++) {
				TableColumn column = viewer.getTable().getColumn(i);
				sb.append(column.getText());
				if (i == colCnt - 1) {
					sb.append("\n");
				} else {
					sb.append("\t");
				}
			}
			if (items != null && items.length > 0) {
				for (TableItem item : items) {
					for (int i = 0; i < colCnt; i++) {
						sb.append(item.getText(i));
						if (i == colCnt - 1) {
							sb.append("\n");
						} else {
							sb.append("\t");
						}
					}
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
				MessageDialog.openInformation(dialog, "Copy", "Copied to clipboard");
			}
		}
	}
	
	protected void createDialogArea() {
		dialog.setLayout(new GridLayout(1, true));
		Composite upperComp = new Composite(dialog, SWT.NONE);
		upperComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		upperComp.setLayout(new GridLayout(2, true));
		rangeLabel = new Label(upperComp, SWT.NONE);
		rangeLabel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		
		Composite btnComp = new Composite(upperComp, SWT.NONE);
		btnComp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		btnComp.setLayout(new RowLayout());
		Button copyBtn = new Button(btnComp, SWT.PUSH);
		copyBtn.setText("Copy All");
		copyBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				copyToClipboard(viewer.getTable().getItems());
			}
		});
		Button exportBtn = new Button(btnComp, SWT.PUSH);
		exportBtn.setText("CSV");
		exportBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(display.getActiveShell(), SWT.SAVE);
				dialog.setOverwrite(true);
				String filename = "[" + DateUtil.format(stime, "yyyyMMdd_HHmmss") + "-" 
				+ DateUtil.format(etime, "HHmmss") + "]" +getTitle().replaceAll(" ", "_") + ".csv";
				dialog.setFileName(filename);
				dialog.setFilterExtensions(new String[] { "*.csv", "*.*" });
				dialog.setFilterNames(new String[] { "CSV File(*.csv)", "All Files" });
				String fileSelected = dialog.open();
				if (fileSelected != null) {
					CSVWriter cw = null;
					try {
						cw = new CSVWriter(new FileWriter(fileSelected));
						int colCnt = viewer.getTable().getColumnCount();
						List<String> list = new ArrayList<String>();
						for (int i = 0; i < colCnt; i++) {
							TableColumn column = viewer.getTable().getColumn(i);
							list.add(column.getText());
						}
						cw.writeNext(list.toArray(new String[list.size()]));
						cw.flush();
						TableItem[] items = viewer.getTable().getItems();
						if (items != null && items.length > 0) {
							for (TableItem item : items) {
								list.clear();
								for (int i = 0; i < colCnt; i++) {
									list.add(item.getText(i));
								}
								cw.writeNext(list.toArray(new String[list.size()]));
								cw.flush();
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					} finally {
						try {
							if (cw != null) {
								cw.close();
							}
						} catch (Throwable th) {}
					}
				}
			}
		});
		Composite tableComp = new Composite(dialog, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 800;
		gd.heightHint = 400;
		tableComp.setLayoutData(gd);
		tableColumnLayout = new TableColumnLayout();
		tableComp.setLayout(tableColumnLayout);
		viewer = new TableViewer(tableComp, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		createColumns();
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer));
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.stateMask == SWT.CTRL || e.stateMask == SWT.COMMAND){
					switch(e.keyCode) {
					case 'c':
					case 'C':
						TableItem[] items = table.getSelection();
						if (items == null || items.length < 1) {
							return;
						}
						copyToClipboard(items);
						break;
					case 'a':
					case 'A':
						table.selectAll();
						break;
					}
				}
			}
		});
		createTableContextMenu();
		calcAsync();
	}
	
	private void createTableContextMenu() {
		MenuManager manager = new MenuManager();
	    viewer.getControl().setMenu(manager.createContextMenu(viewer.getControl()));
	    manager.add(new Action("&Copy", ImageDescriptor.createFromImage(Images.copy)) {
			public void run() {
				TableItem[] items = viewer.getTable().getSelection();
				if (items == null || items.length < 1) {
					return;
				}
				copyToClipboard(items);
			}
	    }); 
	}
	
	protected abstract void calcAsync();
	protected abstract void createMainColumn();
	
	private void createColumns() {
		createMainColumn();
		for (SummaryColumnEnum column : SummaryColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case COUNT:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case ERROR:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).error, "#,##0");
						}
						return null;
					}
				};
				break;
			case TOTAL_ELAPSED:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).sumTime, "#,##0");
						}
						return null;
					}
				};
				break;
			case AVG_ELAPSED:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).sumTime / (double) ((SummaryObject) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case TOTAL_CPU:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).cpu, "#,##0");
						}
						return null;
					}
				};
				break;
			case AVG_CPU:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).cpu / (double) ((SummaryObject) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case TOTAL_MEMORY:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).memory, "#,##0");
						}
						return null;
					}
				};
				break;
			case AVG_MEMORY:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).memory / (double) ((SummaryObject) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case TOTAL_SQLTIME:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).sqltime, "#,##0");
						}
						return null;
					}
				};
				break;
			case AVG_SQLTIME:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).sqltime / (double) ((SummaryObject) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case TOTAL_APICALLTIME:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).apicalltime, "#,##0");
						}
						return null;
					}
				};
				break;
			case AVG_APICALLTIME:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof SummaryObject) {
							return FormatUtil.print(((SummaryObject) element).apicalltime / (double) ((SummaryObject) element).count, "#,##0");
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
	
	protected TableViewerColumn createTableViewerColumn(String title, int width, int alignment, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(true);
		tableColumnLayout.setColumnData(column, new ColumnPixelData(width, true));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) viewer.getComparator();
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}
	
	enum SummaryColumnEnum {
	    COUNT("Count", 100, SWT.RIGHT, true, true, true),
	    ERROR("Error", 80, SWT.RIGHT, true, true, true),
	    TOTAL_ELAPSED("Total Elapsed(ms)", 150, SWT.RIGHT, true, true, true),
	    AVG_ELAPSED("Avg Elapsed(ms)", 150, SWT.RIGHT, true, true, true),
	    TOTAL_CPU("Total Cpu(ms)", 100, SWT.RIGHT, true, true, true),
	    AVG_CPU("Avg Cpu(ms)", 100, SWT.RIGHT, true, true, true),
	    TOTAL_MEMORY("Total Mem(KBytes)", 150, SWT.RIGHT, true, true, true),
	    AVG_MEMORY("Avg Mem(KBytes)", 150, SWT.RIGHT, true, true, true),
	    TOTAL_SQLTIME("Total SQL Time(ms)", 150, SWT.RIGHT, true, true, true),
	    AVG_SQLTIME("Avg SQL Time(ms)", 150, SWT.RIGHT, true, true, true),
	    TOTAL_APICALLTIME("Total APICall Time(ms)", 150, SWT.RIGHT, true, true, true),
	    AVG_APICALLTIME("Avg APICall Time(ms)", 150, SWT.RIGHT, true, true, true);

	    private final String title;
	    private final int weight;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private SummaryColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
	        this.title = text;
	        this.weight = width;
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
			return weight;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
	}
	
	protected abstract static class SummaryObject {
		int count;
		long sumTime;
		int error;
		long maxTime;
		long cpu;
		long memory;
		long sqltime;
		long apicalltime;
	}
}
