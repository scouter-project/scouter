package scouter.client.xlog.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ExUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

public class XLogSummaryServiceDialog extends Dialog {
	
	LongKeyLinkedMap<XLogData> dataMap;
	Label rangeLabel;
	long stime, etime;
	TableViewer viewer;
	TableColumnLayout tableColumnLayout;

	public XLogSummaryServiceDialog(Shell parentShell, LongKeyLinkedMap<XLogData> dataMap) {
		super((Shell) parentShell.getParent());
		this.dataMap = dataMap;
	}
	
	public void setRange(long stime, long etime) {
		this.stime = stime;
		this.etime = etime;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container =  (Composite) super.createDialogArea(parent);
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
		container.setLayout(new GridLayout(1, true));
		Composite upperComp = new Composite(container, SWT.NONE);
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
			}
		});
		Composite tableComp = new Composite(container, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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
					if (e.keyCode == 'c' || e.keyCode == 'C') {
						TableItem[] items = table.getSelection();
						if (items == null || items.length < 1) {
							return;
						}
					}
				}
			}
		});
		calcAsync();
		return container;
	}
	
	private void calcAsync() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				final Map<Integer, ServiceSummary> summaryMap = new HashMap<Integer, ServiceSummary>();
				Map<Integer, List<Integer>> loadTextMap = new HashMap<Integer, List<Integer>>();
				LongEnumer longEnumer = dataMap.keys();
				while (longEnumer.hasMoreElements()) {
					XLogData d = dataMap.get(longEnumer.nextLong());
					long time = d.p.endTime;
					if (d.filter_ok && time >= stime && time <= etime) {
						ServiceSummary summary = summaryMap.get(d.p.service);
						if (summary == null) {
							summary = new ServiceSummary(d.p.service);
							summaryMap.put(d.p.service, summary);
							List<Integer> loadTextList = loadTextMap.get(d.serverId);
							if (loadTextList == null) {
								loadTextList = new ArrayList<Integer>();
								loadTextMap.put(d.serverId, loadTextList);
							}
							loadTextList.add(d.p.service);
						}
						summary.count++;
						summary.sumTime += d.p.elapsed;
						if (d.p.elapsed > summary.maxTime) {
							summary.maxTime = d.p.elapsed;
						}
						if (d.p.error != 0) {
							summary.error++;
						}
					}
				}
				for (Integer serverId : loadTextMap.keySet()) {
					TextProxy.service.load(DateUtil.yyyymmdd(etime), loadTextMap.get(serverId), serverId);
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						rangeLabel.setText(DateUtil.format(stime, "yyyy-MM-dd HH:mm:ss") + " ~ " + DateUtil.format(etime, "HH:mm:ss") + " (" + summaryMap.size() +")");
						viewer.setInput(summaryMap.values());
					}
				});
			}
		});
	}
	
	
	@Override
	protected Point getInitialSize() {
		return getShell().computeSize(750, 350);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Service Summary");
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		return null;
	}
	
	private void createColumns() {
		for (ServiceColumnEnum column : ServiceColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case SERVICE:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof ServiceSummary) {
							return TextProxy.service.getText(((ServiceSummary) element).hash);
						}
						return null;
					}
				};
				break;
			case COUNT:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof ServiceSummary) {
							return FormatUtil.print(((ServiceSummary) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case TOTAL_ELAPSED:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof ServiceSummary) {
							return FormatUtil.print(((ServiceSummary) element).sumTime, "#,##0");
						}
						return null;
					}
				};
				break;
			case AVG_ELAPSED:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof ServiceSummary) {
							return FormatUtil.print(((ServiceSummary) element).sumTime / (double) ((ServiceSummary) element).count, "#,##0.##");
						}
						return null;
					}
				};
				break;
			case ERROR:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof ServiceSummary) {
							return FormatUtil.print(((ServiceSummary) element).error, "#,##0");
						}
						return null;
					}
				};
				break;
			case MAX_ELAPSED:
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof ServiceSummary) {
							return FormatUtil.print(((ServiceSummary) element).maxTime, "#,##0");
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
	
	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment, final boolean isNumber) {
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
	
	enum ServiceColumnEnum {
	    SERVICE("Service", 150, SWT.LEFT, true, true, false),
	    COUNT("Count", 100, SWT.RIGHT, true, true, true),
	    TOTAL_ELAPSED("Total Elapsed(ms)", 150, SWT.RIGHT, true, true, true),
	    AVG_ELAPSED("Avg Elapsed(ms)", 150, SWT.RIGHT, true, true, true),
	    ERROR("Error", 80, SWT.RIGHT, true, true, true),
	    MAX_ELAPSED("Max Elapsed(ms)", 150, SWT.RIGHT, true, true, true);

	    private final String title;
	    private final int weight;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private ServiceColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	private static class ServiceSummary {
		
		int hash;
		int count;
		long sumTime;
		int error;
		long maxTime;
		
		ServiceSummary(int hash) {
			this.hash = hash;
		}
	}

}
