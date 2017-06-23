package scouter.client.summary.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.util.ExUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class ServiceSummaryComposite extends AbstractSummaryComposite {
	
	public ServiceSummaryComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	protected void createColumns() {
		for (ServiceColumnEnum column : ServiceColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case SERVICE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return TextProxy.service.getText(((SummaryData) element).hash);
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
			case CPU_SUM:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return FormatUtil.print(((SummaryData) element).cpu, "#,##0");
						}
						return null;
					}
				};
				break;
			case CPU_AVG:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							SummaryData data = (SummaryData) element;
							return FormatUtil.print(data.cpu / (double) data.count , "#,##0");
						}
						return null;
					}
				};
				break;
			case MEM_SUM:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return FormatUtil.print(((SummaryData) element).mem, "#,##0");
						}
						return null;
					}
				};
				break;
			case MEM_AVG:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							SummaryData data = (SummaryData) element;
							return FormatUtil.print(data.mem / (double) data.count , "#,##0");
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
	
	enum ServiceColumnEnum {

	    SERVICE("Service", 150, SWT.CENTER, true, true, false),
	    COUNT("Count", 70, SWT.RIGHT, true, true, true),
	    ERROR_COUNT("Error", 70, SWT.RIGHT, true, true, true),
	    ELAPSED_SUM("Total Elapsed(ms)", 150, SWT.RIGHT, true, true, true),
	    ELAPSED_AVG("Avg Elapsed(ms)", 150, SWT.RIGHT, true, true, true),
	    CPU_SUM("Total Cpu(ms)", 100, SWT.RIGHT, true, true, true),
	    CPU_AVG("Avg Cpu(ms)", 100, SWT.RIGHT, true, true, true),
	    MEM_SUM("Total Mem(bytes)", 150, SWT.RIGHT, true, true, true),
	    MEM_AVG("Avg Mem(bytes)", 150, SWT.RIGHT, true, true, true);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private ServiceColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	class LoadServiceSummaryJob extends Job {
		
		MapPack param;

		public LoadServiceSummaryJob(MapPack param) {
			super("Loading...");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			Pack p = null;
			try {
				p = tcp.getSingle(RequestCmd.LOAD_SERVICE_SUMMARY, param);
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
				ListValue cpuLv = m.getList("cpu");
				ListValue memLv = m.getList("mem");
				for (int i = 0; i < idLv.size(); i++) {
					SummaryData data = new SummaryData();
					data.hash = idLv.getInt(i);
					data.count = countLv.getInt(i);
					data.errorCount = errorLv.getInt(i);
					data.elapsedSum = elapsedLv.getLong(i);
					data.cpu = cpuLv.getLong(i);
					data.mem = memLv.getLong(i);
					list.add(data);
				}
				TextProxy.service.load(date, idLv, serverId);
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(list);
					}
				});
			}
			 
			return Status.OK_STATUS;
		}
	}

    class LoadLongdayServiceSummaryJob extends Job {

        MapPack param;
        long stime;
        long etime;

        public LoadLongdayServiceSummaryJob(MapPack param, long stime, long etime) {
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
                    packList.add(tcp.getSingle(RequestCmd.LOAD_SERVICE_SUMMARY, param));
                    stime += DateUtil.MILLIS_PER_DAY;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Status.CANCEL_STATUS;
            } finally {
                TcpProxy.putTcpProxy(tcp);
            }

            if (packList.size() > 0) {
                Map<Integer, SummaryData> summaryDataMap = new HashMap<>();
                for (Pack p : packList) {
                    MapPack m = (MapPack) p;
                    ListValue idLv = m.getList("id");
                    ListValue countLv = m.getList("count");
                    ListValue errorLv = m.getList("error");
                    ListValue elapsedLv = m.getList("elapsed");
                    ListValue cpuLv = m.getList("cpu");
                    ListValue memLv = m.getList("mem");
                    for (int i = 0; i < idLv.size(); i++) {
                        SummaryData data = new SummaryData();
                        data.hash = idLv.getInt(i);
                        data.count = countLv.getInt(i);
                        data.errorCount = errorLv.getInt(i);
                        data.elapsedSum = elapsedLv.getLong(i);
                        data.cpu = cpuLv.getLong(i);
                        data.mem = memLv.getLong(i);
                        if (summaryDataMap.containsKey(data.hash)) {
                            summaryDataMap.get(data.hash).addData(data);
                        } else {
                            summaryDataMap.put(data.hash, data);
                        }
                    }
                    TextProxy.service.load(date, idLv, serverId);
                }
                ExUtil.exec(viewer.getTable(), new Runnable() {
                    public void run() {
                        viewer.setInput(summaryDataMap.values());
                    }
                });
            }
            return Status.OK_STATUS;
        }
    }

	protected void getSummaryData() {
		new LoadLongdayServiceSummaryJob(param, param.getLong("stime"), param.getLong("etime")).schedule();
	}

	protected String getTitle() {
		return "SERVICE";
	}
}
