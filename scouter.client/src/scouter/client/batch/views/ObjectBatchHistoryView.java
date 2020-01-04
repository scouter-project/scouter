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
package scouter.client.batch.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.batch.actions.OpenBatchDetailJob;
import scouter.client.model.TextProxy;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.BatchPack;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
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
	}
	
	public void search() {
		final String search = searchText.getText();
		final String time = responseTimeText.getText();
		final long [] period = getTimePeriod();		
		final List<BatchPack> list = new ArrayList<BatchPack>();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					if (StringUtil.isNotEmpty(search)) {
						param.put("filter", search);
					}
					if (StringUtil.isNotEmpty(time)) {
						long response = Long.parseLong(time);
						param.put("response", response);
					}
					param.put("from", period[0]);
					param.put("to", period[1]);
					
					tcp.process(RequestCmd.BATCH_HISTORY_LIST, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							BatchPack pack = new BatchPack();
							pack.readSimple(in);
							list.add(pack);
							pack.index = list.size();
						}
					});
				} catch (Throwable t) {
					ConsoleProxy.errorSafe(t.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				ExUtil.exec(tableViewer.getTable(), new Runnable() {
					public void run() {
						tableViewer.setInput(list);
					}
				});
			}
		});
		
	}
	
	private long [] getTimePeriod(){
		long [] period = new long[2];
		Calendar cal = Calendar.getInstance();
		cal.set(date.getYear(), date.getMonth(), date.getDay(), 0, 0, 0);
		long dayTime = cal.getTimeInMillis();
		dayTime = dayTime - (dayTime % 1000L);
		period[0] = dayTime + (fromTime.getHours() *3600000L) + (fromTime.getMinutes() * 60000L) + (fromTime.getSeconds() * 1000L);
		period[1] = dayTime + (toTime.getHours() *3600000L) + (toTime.getMinutes() * 60000L) + (toTime.getSeconds() * 1000L);
		return period;
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
	    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent evt) {
				StructuredSelection sel = (StructuredSelection) evt.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof BatchPack) {
					BatchPack pack = (BatchPack) o;
					Display display = ObjectBatchHistoryView.this.getViewSite().getShell().getDisplay();
					new OpenBatchDetailJob(display, pack, serverId).schedule();
				} else {
					System.out.println(o);
				}
			}
		});
	    tableViewer.setContentProvider(new ArrayContentProvider());
	    tableViewer.setComparator(new ColumnLabelSorter(tableViewer));
	    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	    tableViewer.getControl().setLayoutData(gridData);
	}
	
	
	private void createUpperMenu(Composite composite) {
		Group parentGroup = new Group(composite, SWT.NONE);
		parentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout layout = new GridLayout(10, false);
		parentGroup.setLayout(layout);
		
		Label label = new Label(parentGroup, SWT.CENTER);
		label.setText("End Time");
		date = new DateTime(parentGroup, SWT.DATE);
		fromTime = new DateTime(parentGroup, SWT.TIME);
		fromTime.setTime(0, 0, 0);
		label = new Label(parentGroup, SWT.CENTER);
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
						if (element instanceof BatchPack) {
							return "" + (((BatchPack)element).index);
						}
						return null;
					}
				};
				break;
			case TIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchPack) {
							return DateUtil.yyyymmdd(((BatchPack) element).startTime) + " " + DateUtil.hhmmss(((BatchPack) element).startTime);
						}
						return null;
					}
				};
				break;
			case JOBID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchPack) {
							return ((BatchPack) element).batchJobId;
						}
						return null;
					}
				};
				break;
			case RESPONSETIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchPack) {
							return String.format("%,13d",((BatchPack) element).elapsedTime);
						}
						return null;
					}
				};
				break;
			case SQLTIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchPack) {
							return String.format("%,13d",(((BatchPack) element).sqlTotalTime/1000000L));
						}
						return null;
					}
				};
				break;
			case SQLRUNS:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchPack) {
							return String.format("%,13d",((BatchPack) element).sqlTotalRuns);
						}
						return null;
					}
				};
				break;
			case THREADS:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchPack) {
							return String.format("%,13d",((BatchPack) element).threadCnt);
						}
						return null;
					}
				};
				break;
			case STACK:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof BatchPack) {
							return ((BatchPack) element).isStack?"O":"";
						}
						return null;
					}
				};
				break;
/*				
			case STACK:
				labelProvider = new ColumnLabelProvider() {
			            //make sure you dispose these buttons when viewer input changes
			            Map<Object, Button> buttons = new HashMap<Object, Button>();
			            
			            @Override
			            public void update(ViewerCell cell) {
			                TableItem item = (TableItem) cell.getItem();
			                Button button = null;
			            	if(((BatchPack)(cell.getViewerRow().getElement())).isStack){
				                if(buttons.containsKey(cell.getElement()))
				                {
				                    button = buttons.get(cell.getElement());
				                }
				                else
				                {
				                	button = new Button((Composite) cell.getViewerRow().getControl(),SWT.NONE);
				                    button.setText("SFA");
				                    buttons.put(cell.getElement(), button);
				                }
			            	}
			                TableEditor editor = new TableEditor(item.getParent());
			                editor.grabHorizontal  = true;
			                editor.grabVertical = true;
			                editor.setEditor(button , item, cell.getColumnIndex());
			                editor.layout();
			            }
			        };
			    break;
*/			    
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
	
	enum BatchColumnEnum {
		NO("No", 60, SWT.RIGHT, true, true, true),
	    TIME("Time", 100, SWT.CENTER, true, true, false),
	    JOBID("Job ID", 100, SWT.LEFT, true, true, false),
	    RESPONSETIME("Response Time", 120, SWT.RIGHT, true, true, true),
	    SQLTIME("SQL Time", 120, SWT.RIGHT, true, true, true),
	    SQLRUNS("SQL Runs", 120, SWT.RIGHT, true, true, true),
	    THREADS("Threads", 100, SWT.RIGHT, true, true, true),
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
}
