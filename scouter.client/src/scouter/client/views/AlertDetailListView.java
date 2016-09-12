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

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import au.com.bytecode.opencsv.CSVWriter;
import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.popup.AlertNotifierDialog;
import scouter.client.popup.CalendarDialog;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.AlertLevel;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.HashUtil;
import scouter.util.StringEnumer;
import scouter.util.StringUtil;

public class AlertDetailListView extends ViewPart implements CalendarDialog.ILoadCalendarDialog {
	
	public static final String ID = AlertDetailListView.class.getName();

	private int serverId;
	
	private Text dateText;
	private DateTime fromTime;
	private DateTime toTime;
	private Text maxCountText;
	private Combo levelCombo;
	private Text objText;
	private Text keyText;
	private TableViewer viewer;
	private String yyyymmdd;

	private TableColumnLayout tableColumnLayout;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		serverId = Integer.valueOf(secId);
		yyyymmdd = DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId));
	}

	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		Group parentGroup = new Group(parent, SWT.NONE);
		parentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout layout = new GridLayout(10, false);
		parentGroup.setLayout(layout);
		
		Label label = new Label(parentGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText("Date");

        dateText = new Text(parentGroup, SWT.READ_ONLY | SWT.BORDER);
		dateText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		dateText.setBackground(ColorUtil.getInstance().getColor("white"));
		dateText.setText(DateUtil.format(TimeUtil.getCurrentTime(serverId), "yyyy-MM-dd"));
		
		Button button = new Button(parentGroup, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		button.setImage(Images.calendar);
		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					Display display = Display.getCurrent();
					if (display == null) {
						display = Display.getDefault();
					}
					CalendarDialog dialog = new CalendarDialog(display, AlertDetailListView.this);
					dialog.show(dateText.getLocation().x, dateText.getLocation().y, DateUtil.yyyymmdd(yyyymmdd));
					break;
				}
			}
		});
		
		long now = TimeUtil.getCurrentTime(serverId);
		label = new Label(parentGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText("From");
        
		fromTime = new DateTime(parentGroup, SWT.TIME | SWT.SHORT);
		fromTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fromTime.setHours(DateUtil.getHour(now) - 1);
		fromTime.setMinutes(DateUtil.getMin(now));
		
		label = new Label(parentGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText("To");
        
        toTime = new DateTime(parentGroup, SWT.TIME | SWT.SHORT);
        toTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        toTime.setHours(DateUtil.getHour(now));
        toTime.setMinutes(DateUtil.getMin(now));
        
        label = new Label(parentGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText("Count(Max:1000)");
        
        maxCountText = new Text(parentGroup, SWT.BORDER);
        maxCountText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        maxCountText.setText("500");
        maxCountText.setToolTipText("1~1000");
        maxCountText.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
				String count = maxCountText.getText();
				if (StringUtil.isEmpty(count)) {
					return;
				}
				try {
					Integer.valueOf(count);
				} catch(Exception ex) {
					MessageDialog.openError(maxCountText.getShell(), "Invalid Count", "Count is allowed only digits");
					maxCountText.setText("");
				}
			}
			public void keyPressed(KeyEvent e) {
				
			}
		});
        
        button = new Button(parentGroup, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.setText("&Search");
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				load();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		label = new Label(parentGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText("Level");
        
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 2;
        levelCombo = new Combo (parentGroup, SWT.READ_ONLY);
        levelCombo.setLayoutData(gridData);
        levelCombo.add("ALL");
        StringEnumer itr = AlertLevel.names();
        while (itr.hasMoreElements()) {
        	levelCombo.add(itr.nextString());
        }
        levelCombo.select(0);
        
        label = new Label(parentGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText("Object");
        
        objText = new Text(parentGroup, SWT.BORDER);
        objText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        label = new Label(parentGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        label.setText("Key");
        
        keyText = new Text(parentGroup, SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 3;
        keyText.setLayoutData(gridData);
        
        button = new Button(parentGroup, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.setText("&Clear");
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				levelCombo.select(0);
				objText.setText("");
				keyText.setText("");
				viewer.getTable().removeAll();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		Composite tableComposite = new Composite(parent, SWT.NONE);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);
		viewer = new TableViewer(tableComposite, SWT.MULTI  | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer));
	    viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof AlertData) {
					AlertData data = (AlertData) o;
					Display display = Display.getCurrent();
					if (display == null) {
						display = Display.getDefault();
					}
					AlertNotifierDialog alertDialog = new AlertNotifierDialog(display, serverId);
					alertDialog.setObjName(data.object);
					alertDialog.setPack(data.toPack());
					alertDialog.show(getViewSite().getShell().getBounds());
				} else {
					System.out.println(o);
				}
			}
		});
	    
	    IToolBarManager man =  getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Export CSV", ImageUtil.getImageDescriptor(Images.csv)) {
			public void run() {
				Server server = ServerManager.getInstance().getServer(serverId);
				FileDialog dialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
				dialog.setOverwrite(true);
				String filename = "[" + server.getName() + "]" + yyyymmdd + "_" + fromTime.getHours() + fromTime.getMinutes() + "_" + toTime.getHours() + toTime.getMinutes() + ".csv";
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
	}
	
	private void createColumns() {
		for (AlertColumnEnum column : AlertColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWeight(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case TIME :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return DateUtil.format(((AlertData) element).time, "yyyy-MM-dd HH:mm:ss");
						}
						return null;
					}
				};
				break;
			case LEVEL :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return ((AlertData) element).level;
						}
						return null;
					}
				};
				break;
			case OBJECT :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return ((AlertData) element).object;
						}
						return null;
					}
				};
				break;
			case TITLE :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return ((AlertData) element).title;
						}
						return null;
					}
				};
				break;
			case MESSAGE :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return ((AlertData) element).message;
						}
						return null;
					}
				};
				break;
			case TAGS :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return ((AlertData) element).tags.toString();
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
	
	private TableViewerColumn createTableViewerColumn(String title, int weight, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(weight, 10, resizable));
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
	
	public void setFocus() {
		
	}
	
	public void setInput(long stime, long etime) {
		onPressedOk(DateUtil.yyyymmdd(stime));
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(stime));
		int hours = calendar.get(Calendar.HOUR_OF_DAY);
		int minutes = calendar.get(Calendar.MINUTE);
		int seconds = calendar.get(Calendar.SECOND);
		fromTime.setTime(hours, minutes, seconds);
		calendar = Calendar.getInstance();
		calendar.setTime(new Date(etime));
		hours = calendar.get(Calendar.HOUR_OF_DAY);
		minutes = calendar.get(Calendar.MINUTE);
		seconds = calendar.get(Calendar.SECOND);
		toTime.setTime(hours, minutes, seconds);
	}
	
	private void load() {
		int fromHour = fromTime.getHours();
		int fromMin = fromTime.getMinutes();
		int toHour = toTime.getHours();
		int toMin = toTime.getMinutes();
		final long stime = DateUtil.getTime(yyyymmdd + (fromHour < 10 ? "0" + fromHour : fromHour) +  (fromMin < 10 ? "0" + fromMin : fromMin), "yyyyMMddHHmm");
		final long etime = DateUtil.getTime(yyyymmdd + (toHour < 10 ? "0" + toHour : toHour) +  (toMin < 10 ? "0" + toMin : toMin), "yyyyMMddHHmm");
		final String count = maxCountText.getText();
		final String level = levelCombo.getText();
		final String object = objText.getText();
		final String key = keyText.getText();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				final List<AlertData> dataList = new ArrayList<AlertData>();
				try {
					MapPack param = new MapPack();
					param.put("date", yyyymmdd);
					param.put("stime", stime);
					param.put("etime", etime);
					if (StringUtil.isNotEmpty(count)) {
						param.put("count", Long.valueOf(count));
					}
					if (StringUtil.isNotEmpty(level)  && "ALL".equalsIgnoreCase(level) == false) {
						param.put("level", level);
					}
					if (StringUtil.isNotEmpty(object)) {
						param.put("object", object);
					}
					if (StringUtil.isNotEmpty(key)) {
						param.put("key", key);
					}
					List<Pack> packList = tcp.process(RequestCmd.ALERT_LOAD_TIME, param);
					for (Pack pack : packList) {
						if (pack instanceof AlertPack) {
							AlertPack alertPack = (AlertPack) pack;
							AlertData data = new AlertData();
							data.time = alertPack.time;
							data.level = AlertLevel.getName(alertPack.level);
							data.object = TextProxy.object.getLoadText(yyyymmdd, alertPack.objHash, serverId);
							data.title = alertPack.title;
							data.message = alertPack.message;
							data.objType = alertPack.objType;
							data.tags = alertPack.tags;
							dataList.add(data);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					ConsoleProxy.errorSafe(e.getMessage());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(dataList);
					}
				});
			}
		});
	}

	public void onPressedOk(long startTime, long endTime) {	}

	public void onPressedOk(String date) {
		this.yyyymmdd = date;
		dateText.setText(date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8));
	}

	public void onPressedCancel() { };
	
	enum AlertColumnEnum {
	    TIME("TIME", 6, SWT.RIGHT, true, true, false), //
	    LEVEL("LEVEL", 3, SWT.CENTER, true, true, false), //
	    OBJECT("OBJECT", 9, SWT.LEFT, true, true, false),
	    TITLE("TITLE", 10, SWT.LEFT, true, true, false),
	    MESSAGE("MESSAGE", 25, SWT.LEFT, true, true, false),
	    TAGS("TAGs", 7, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int weight;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private AlertColumnEnum(String text, int weight, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
	        this.title = text;
	        this.weight = weight;
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

		public int getWeight() {
			return weight;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
	}
	
	static class AlertData {
		public long time;
		public String level;
		public String object;
		public String title;
		public String message;
		public MapValue tags;
		public String objType;
		
		public AlertPack toPack() {
			AlertPack pack = new AlertPack();
			pack.level = AlertLevel.getValue(level);
			pack.message = message;
			pack.objHash = object == null ? 0 : HashUtil.hash(object);
			pack.objType = objType;
			pack.tags = tags;
			pack.time = time;
			pack.title = title;
			return pack;
		}

		public String toString() {
			return "AlertData [time=" + time + ", level=" + level + ", object="
					+ object + ", title=" + title + ", message=" + message
					+ ", tags=" + tags + ", objType=" + objType + "]";
		}
	}
	
}
