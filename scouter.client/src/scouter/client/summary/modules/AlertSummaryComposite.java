package scouter.client.summary.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.AlertLevel;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.BitUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class AlertSummaryComposite extends AbstractSummaryComposite {
	
	public AlertSummaryComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	protected void createColumns() {
		for (AlertColumnEnum column : AlertColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case TITLE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return ((AlertData) element).title;
						}
						return null;
					}
				};
				break;
			case LEVEL:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return AlertLevel.getName(((AlertData) element).level);
						}
						return null;
					}
				};
				break;
			case COUNT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof AlertData) {
							return FormatUtil.print(((AlertData) element).count, "#,##0");
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
	
	enum AlertColumnEnum {

	    TITLE("TITLE", 150, SWT.LEFT, true, true, false),
	    LEVEL("LEVEL", 100, SWT.LEFT, true, true, false),
	    COUNT("Count", 70, SWT.RIGHT, true, true, true);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private AlertColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	class LoadAlertSummaryJob extends Job {
		
		MapPack param;

		public LoadAlertSummaryJob(MapPack param) {
			super("Loading...");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			Pack p = null;
			try {
				p = tcp.getSingle(RequestCmd.LOAD_ALERT_SUMMARY, param);
			} catch (Exception e) {
				e.printStackTrace();
				return Status.CANCEL_STATUS;
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			
			if (p != null) {
				final List<AlertData> list = new ArrayList<AlertData>();
				MapPack m = (MapPack) p;
				ListValue titleLv = m.getList("title");
				ListValue levelLv = m.getList("level");
				ListValue countLv = m.getList("count");
				for (int i = 0; i < titleLv.size(); i++) {
					AlertData data = new AlertData();
					data.title = titleLv.getString(i);
					data.level = (byte) levelLv.getInt(i);
					data.count = countLv.getInt(i);
					list.add(data);
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(list);
					}
				});
			}
			 
			return Status.OK_STATUS;
		}
	}
	
	class LoadLongdayAlertSummaryJob extends Job {

        MapPack param;
        long stime;
        long etime;

        public LoadLongdayAlertSummaryJob(MapPack param, long stime, long etime) {
            super("Loading...");
            this.param = param;
            this.stime = stime;
            this.etime = etime;
        }

        protected IStatus run(IProgressMonitor monitor) {
            TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
            List<Pack> packList = new ArrayList<>();
            try {
                while (stime <= etime) {
                    String date = DateUtil.yyyymmdd(stime);
                    long lastTimestampOfDay = DateUtil.getTime(date, "yyyyMMdd") + DateUtil.MILLIS_PER_DAY - 1;
                    param.put("date", date);
                    param.put("stime", stime);
                    param.put("etime", lastTimestampOfDay <= etime ? lastTimestampOfDay : etime);
                    packList.add(tcp.getSingle(RequestCmd.LOAD_ALERT_SUMMARY, param));
                    stime += DateUtil.MILLIS_PER_DAY;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Status.CANCEL_STATUS;
            } finally {
                TcpProxy.putTcpProxy(tcp);
            }

            if (packList.size() > 0) {
                Map<String, AlertData> alertDataMap = new HashMap<>();
                for (Pack p : packList) {
                    MapPack m = (MapPack) p;
                    
                    ListValue titleLv = m.getList("title");
    					ListValue levelLv = m.getList("level");
    					ListValue countLv = m.getList("count");
    					for (int i = 0; i < titleLv.size(); i++) {
    						AlertData data = new AlertData();
    						data.title = titleLv.getString(i);
    						data.level = (byte) levelLv.getInt(i);
    						data.count = countLv.getInt(i);
    						if (alertDataMap.containsKey(data.title)) {
    							alertDataMap.get(data.title).addData(data);
    						} else {
    							alertDataMap.put(data.title, data);
    						}
    					}                    
                }
                ExUtil.exec(viewer.getTable(), new Runnable() {
                    public void run() {
                        viewer.setInput(alertDataMap.values());
                    }
                });
            }
            return Status.OK_STATUS;
        }
    }
	
	private static class AlertData {
		public String title;
		public byte level;
		public int count;
		
		public void addData(AlertData another) {
			this.count += another.count;
		}
	}

	protected void getSummaryData() {
		new LoadLongdayAlertSummaryJob(param, param.getLong("stime"), param.getLong("etime")).schedule();
	}

	protected String getTitle() {
		return "ALERT";
	}
}
