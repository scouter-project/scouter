package scouter.client.summary.modules;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import au.com.bytecode.opencsv.CSVWriter;
import scouter.client.Images;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.util.DateUtil;

public abstract class AbstractSummaryComposite extends Composite {
	
	public static final String[] HOURLY_TIMES = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"};
	public static final String[] FIVE_MIN_TIMES = {"00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"};
	
	Composite parent;
	int serverId;
	MapPack param;
	
	Label startDateLbl;
	Combo startHHCmb;
	Combo startMMCmb;
	Label endDateLbl;
	Combo endHHCmb;
	Combo endMMCmb;
	
	protected TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	private Clipboard clipboard;
	
	String date = DateUtil.yyyymmdd();
    String endDate = DateUtil.yyyymmdd();

	public AbstractSummaryComposite(Composite parent, int style) {
		super(parent, style);
		this.parent = parent;
	}
	
	public void setData(int serverId, MapPack param) {
		this.serverId = serverId;
		this.param = param;
		initLayout();
		clipboard = new Clipboard(null);
	}
	
	private void initLayout() {
		this.setLayout(new GridLayout(1, true));
		Composite upperComp = new Composite(this, SWT.NONE);
		upperComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		upperComp.setLayout(new GridLayout(8, false));
		
		long now = TimeUtil.getCurrentTime(serverId);
		
		startDateLbl = new Label(upperComp, SWT.CENTER | SWT.BORDER);
		startDateLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		startDateLbl.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				CalendarDialog dialog = new CalendarDialog(parent.getDisplay(), new ILoadCalendarDialog(){
					public void onPressedOk(long startTime, long endTime) {}
					public void onPressedCancel() {}
					public void onPressedOk(String date) {
						setDate(date);
						startDateLbl.setText(DateUtil.format(DateUtil.yyyymmdd(date), "yyyy-MM-dd"));
						
					}
				});
				dialog.show(-1, -1, DateUtil.yyyymmdd(date));
			}
			
		});
		startDateLbl.setText(DateUtil.format(now, "yyyy-MM-dd"));
		
		Composite startCmbComp = new Composite(upperComp, SWT.NONE);
		startCmbComp.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		startCmbComp.setLayout(new RowLayout());
		
		long start = now - DateUtil.MILLIS_PER_FIVE_MINUTE;
		
		startHHCmb = new Combo(startCmbComp, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		startHHCmb.setLayoutData(new RowData(50, SWT.DEFAULT));
		startHHCmb.setItems(HOURLY_TIMES);
		startHHCmb.setText(DateUtil.format(start, "HH"));
		
		startMMCmb = new Combo(startCmbComp, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		startMMCmb.setLayoutData(new RowData(50, SWT.DEFAULT));
		startMMCmb.setItems(FIVE_MIN_TIMES);
		startMMCmb.setText(DateUtil.format(start / DateUtil.MILLIS_PER_FIVE_MINUTE * DateUtil.MILLIS_PER_FIVE_MINUTE, "mm"));
		
		Label label = new Label(upperComp, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		label.setAlignment(SWT.CENTER);
		label.setText("to");

		endDateLbl = new Label(upperComp, SWT.CENTER | SWT.BORDER);
		endDateLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		endDateLbl.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				CalendarDialog dialog = new CalendarDialog(parent.getDisplay(), new ILoadCalendarDialog(){
					public void onPressedOk(long startTime, long endTime) {}
					public void onPressedCancel() {}
					public void onPressedOk(String date) {
						setEndDate(date);
						endDateLbl.setText(DateUtil.format(DateUtil.yyyymmdd(date), "yyyy-MM-dd"));
						
					}
				});
				dialog.show(-1, -1, DateUtil.yyyymmdd(endDate));
			}
			
		});
		endDateLbl.setText(DateUtil.format(now, "yyyy-MM-dd"));
		
		Composite endCmbComp = new Composite(upperComp, SWT.NONE);
		endCmbComp.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		endCmbComp.setLayout(new RowLayout());
		
		endHHCmb = new Combo(endCmbComp, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		endHHCmb.setLayoutData(new RowData(50, SWT.DEFAULT));
		endHHCmb.setItems(HOURLY_TIMES);
		endHHCmb.setText(DateUtil.format(now, "HH"));
		
		endMMCmb = new Combo(endCmbComp, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		endMMCmb.setLayoutData(new RowData(50, SWT.DEFAULT));
		endMMCmb.setItems(FIVE_MIN_TIMES);
		endMMCmb.setText(DateUtil.format(now / DateUtil.MILLIS_PER_FIVE_MINUTE * DateUtil.MILLIS_PER_FIVE_MINUTE, "mm"));
		
		final Button getBtn = new Button(upperComp, SWT.PUSH);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		getBtn.setLayoutData(gd);
		getBtn.setText("&GET");
		getBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				long stime = DateUtil.getTime(date + startHHCmb.getText() + startMMCmb.getText(), "yyyyMMddHHmm");
				long etime = DateUtil.getTime(endDate + endHHCmb.getText() + endMMCmb.getText(), "yyyyMMddHHmm") - 1;
				if (stime >= etime) {
					MessageDialog.openWarning(parent.getShell(), "Warning", "Wrong range");
					return;
				}
				param.put("date", date);
				param.put("stime", stime);
				param.put("etime", etime);
				getSummaryData();
			}
		});
		
		final Button clearBtn = new Button(upperComp, SWT.PUSH);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		clearBtn.setLayoutData(gd);
		clearBtn.setText("&CLEAR");
		clearBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				viewer.getTable().removeAll();
			}
		});
		
		final Button csvBtn = new Button(upperComp, SWT.PUSH);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		csvBtn.setLayoutData(gd);
		csvBtn.setText("&CSV");
		csvBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(parent.getShell(), SWT.SAVE);
				dialog.setOverwrite(true);
				String filename = "[" + getTarget() + "][" + date + startHHCmb.getText() + startMMCmb.getText() + "-" 
				+ endDate + endHHCmb.getText() + endMMCmb.getText() + "]" + getTitle() + ".csv";
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
		
		Composite tableComp = new Composite(this, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 800;
		gd.heightHint = 400;
		tableComp.setLayoutData(gd);
		createTableViewer(tableComp);
	}
	
	private void setDate(String date) {
		this.date = date;
	}
	
	private void setEndDate(String date) {
		this.endDate = date;
	}
	
	protected abstract void getSummaryData();
	
	protected abstract String getTitle();
	
	private void createTableViewer(Composite composite) {
		viewer = new TableViewer(composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    createTableContextMenu();
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer));
	    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	    viewer.getControl().setLayoutData(gridData);
	}
	
	private void createTableContextMenu() {
		MenuManager manager = new MenuManager();
		viewer.getControl().setMenu(manager.createContextMenu(viewer.getControl()));
	    manager.add(new Action("&Copy", ImageDescriptor.createFromImage(Images.copy)) {
			public void run() {
				copyToClipboard(viewer.getTable().getSelection());
			}
	    });
	    viewer.getTable().addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(Event e) {
				if(e.stateMask == SWT.CTRL || e.stateMask == SWT.COMMAND){
					switch(e.keyCode) {
					case 'c':
					case 'C':
						TableItem[] items = viewer.getTable().getSelection();
						if (items == null || items.length < 1) {
							return;
						}
						copyToClipboard(items);
						break;
					case 'a':
					case 'A':
						viewer.getTable().selectAll();
						break;
					}
				}
			}
		});
	}
	
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
			}
		}
	}
	
	
	
	protected abstract void createColumns();
	
	protected TableViewerColumn createTableViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(30, width, resizable));
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
	
	private String getTarget() {
		Server server = ServerManager.getInstance().getServer(serverId);
		int objHash = param.getInt("objHash");
		if (objHash != 0) {
			AgentObject ao = AgentModelThread.getInstance().getAgentObject(objHash);
			return ao.getDisplayName();
		}
		String objType = param.getText("objType");
		if (objType != null) {
			return objType;
		}
		return server.getName();
	}
}
