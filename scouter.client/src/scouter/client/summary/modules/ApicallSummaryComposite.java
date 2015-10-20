package scouter.client.summary.modules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCounterDialog;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class ApicallSummaryComposite extends Composite {
	
	public static final String[] HOURLY_TIMES = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"};
	public static final String[] FIVE_MIN_TIMES = {"00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60"};
	
	Composite parent;
	int serverId;
	MapPack param;
	
	Label dateLbl;
	Combo startHHCmb;
	Combo startMMCmb;
	Combo endHHCmb;
	Combo endMMCmb;
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	private Clipboard clipboard;
	
	String date = DateUtil.yyyymmdd();

	public ApicallSummaryComposite(Composite parent, int style) {
		super(parent, style);
		this.parent = parent;
	}
	
	public void setData(int serverId, MapPack param) {
		this.serverId = serverId;
		this.param = param;
		initLayout();
	}
	
	private void initLayout() {
		this.setLayout(new GridLayout(1, true));
		Composite upperComp = new Composite(this, SWT.NONE);
		upperComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		upperComp.setLayout(new GridLayout(6, false));
		
		long now = TimeUtil.getCurrentTime(serverId);
		
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		dateLbl = new Label(upperComp, SWT.CENTER | SWT.BORDER);
		dateLbl.setLayoutData(gd);
		dateLbl.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				CalendarDialog dialog = new CalendarDialog(parent.getDisplay(), new ILoadCounterDialog(){
					public void onPressedOk(long startTime, long endTime) {}
					public void onPressedCancel() {}
					public void onPressedOk(String date) {
						ApicallSummaryComposite.this.date = date;
						dateLbl.setText(DateUtil.format(DateUtil.yyyymmdd(date), "yyyy-MM-dd"));
						
					}
				});
				dialog.show(-1, -1, DateUtil.yyyymmdd(date));
			}
			
		});
		dateLbl.setText(DateUtil.format(now, "yyyy-MM-dd"));
		
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
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		getBtn.setLayoutData(gd);
		getBtn.setText("&GET");
		getBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				long stime = DateUtil.getTime(date + startHHCmb.getText() + startMMCmb.getText(), "yyyyMMddHHmm");
				long etime = DateUtil.getTime(date + endHHCmb.getText() + endMMCmb.getText(), "yyyyMMddHHmm") - 1;
				if (stime >= etime) {
					MessageDialog.openWarning(parent.getShell(), "Warning", "Wrong range");
					return;
				}
				param.put("date", date);
				param.put("stime", stime);
				param.put("etime", etime);
				new LoadApicallSummaryJob(param).schedule();
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
		
		Composite tableComp = new Composite(this, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 800;
		gd.heightHint = 400;
		tableComp.setLayoutData(gd);
		createTableViewer(tableComp);
	}
	
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
	
	private void createColumns() {
		for (ApicallColumnEnum column : ApicallColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case URL:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return TextProxy.apicall.getText(((SummaryData) element).hash);
						}
						return null;
					}
				};
				break;
			case COUNT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return FormatUtil.print(((SummaryData) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case ERROR_COUNT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return FormatUtil.print(((SummaryData) element).errorCount, "#,##0");
						}
						return null;
					}
				};
				break;
			case ELAPSED_SUM:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return FormatUtil.print(((SummaryData) element).elapsedSum, "#,##0");
						}
						return null;
					}
				};
				break;
			case ELAPSED_AVG:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							SummaryData data = (SummaryData) element;
							return FormatUtil.print(data.elapsedSum / (double) data.count , "#,##0");
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
	
	enum ApicallColumnEnum {

	    URL("URL", 150, SWT.CENTER, true, true, false),
	    COUNT("Count", 70, SWT.RIGHT, true, true, true),
	    ERROR_COUNT("Error", 70, SWT.RIGHT, true, true, true),
	    ELAPSED_SUM("Total Elapsed(ms)", 150, SWT.RIGHT, true, true, true),
	    ELAPSED_AVG("Avg Elapsed(ms)", 150, SWT.RIGHT, true, true, true);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private ApicallColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	class LoadApicallSummaryJob extends Job {
		
		MapPack param;

		public LoadApicallSummaryJob(MapPack param) {
			super("Loading...");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			Pack p = null;
			try {
				p = tcp.getSingle(RequestCmd.LOAD_APICALL_SUMMARY, param);
			} catch (Exception e) {
				e.printStackTrace();
				return Status.CANCEL_STATUS;
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			
			if (p != null) {
				final List<SummaryData> list = new ArrayList<SummaryData>();
				MapPack m = (MapPack) p;
				ListValue idLv = m.getList("id");
				ListValue countLv = m.getList("count");
				ListValue errorLv = m.getList("error");
				ListValue elapsedLv = m.getList("elapsed");
				for (int i = 0; i < idLv.size(); i++) {
					SummaryData data = new SummaryData();
					data.hash = idLv.getInt(i);
					data.count = countLv.getInt(i);
					data.errorCount = errorLv.getInt(i);
					data.elapsedSum = elapsedLv.getLong(i);
					list.add(data);
				}
				TextProxy.apicall.load(date, idLv, serverId);
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(list);
					}
				});
			}
			 
			return Status.OK_STATUS;
		}
	}
}
