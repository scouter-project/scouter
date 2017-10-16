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
import scouter.util.BitUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;

public class ErrorSummaryComposite extends AbstractSummaryComposite {
	
	public ErrorSummaryComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	protected void createColumns() {
		for (ErrorColumnEnum column : ErrorColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case ERROR:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return TextProxy.error.getText(((ErrorData) element).error);
						}
						return null;
					}
				};
				break;
			case SERVICE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return TextProxy.service.getText(((ErrorData) element).service);
						}
						return null;
					}
				};
				break;
			case MESSAGE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return TextProxy.error.getText(((ErrorData) element).message);
						}
						return null;
					}
				};
				break;
			case COUNT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return FormatUtil.print(((ErrorData) element).count, "#,##0");
						}
						return null;
					}
				};
				break;
			case TXID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return Hexa32.toString32(((ErrorData) element).txid);
						}
						return null;
					}
				};
				break;
			case SQL:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return TextProxy.sql.getText(((ErrorData) element).sql);
						}
						return null;
					}
				};
				break;
			case APICALL:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return TextProxy.apicall.getText(((ErrorData) element).apicall);
						}
						return null;
					}
				};
				break;
			case FULLSTACK:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ErrorData) {
							return TextProxy.error.getText(((ErrorData) element).fullstack);
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
	
	enum ErrorColumnEnum {

	    ERROR("Exception", 150, SWT.LEFT, true, true, false),
	    SERVICE("Service", 150, SWT.LEFT, true, true, false),
	    MESSAGE("Message", 200, SWT.LEFT, true, true, false),
	    COUNT("Count", 70, SWT.RIGHT, true, true, true),
	    TXID("TxId", 100, SWT.CENTER, true, true, false),
	    SQL("SQL", 150, SWT.LEFT, true, true, false),
	    APICALL("ApiCall", 150, SWT.LEFT, true, true, false),
	    FULLSTACK("Full Stack", 200, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private ErrorColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	class LoadErrorSummaryJob extends Job {
		
		MapPack param;

		public LoadErrorSummaryJob(MapPack param) {
			super("Loading...");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			Pack p = null;
			try {
				p = tcp.getSingle(RequestCmd.LOAD_SERVICE_ERROR_SUMMARY, param);
			} catch (Exception e) {
				e.printStackTrace();
				return Status.CANCEL_STATUS;
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			
			if (p != null) {
				final List<ErrorData> list = new ArrayList<ErrorData>();
				MapPack m = (MapPack) p;
				ListValue errorLv = m.getList("error");
				ListValue serviceLv = m.getList("service");
				ListValue messageLv = m.getList("message");
				ListValue countLv = m.getList("count");
				ListValue txidLv = m.getList("txid");
				ListValue sqlLv = m.getList("sql");
				ListValue apiLv = m.getList("apicall");
				ListValue stackLv = m.getList("fullstack");
				for (int i = 0; i < errorLv.size(); i++) {
					ErrorData data = new ErrorData();
					data.error = errorLv.getInt(i);
					data.service = serviceLv.getInt(i);
					data.message = messageLv.getInt(i);
					data.count = countLv.getInt(i);
					data.txid = txidLv.getLong(i);
					data.sql = sqlLv.getInt(i);
					data.apicall = apiLv.getInt(i);
					data.fullstack = stackLv.getInt(i);
					list.add(data);
				}
				
				TextProxy.error.load(date, errorLv, serverId);
				TextProxy.service.load(date, serviceLv, serverId);
				TextProxy.error.load(date, messageLv, serverId);
				TextProxy.sql.load(date, sqlLv, serverId);
				TextProxy.apicall.load(date, apiLv, serverId);
				TextProxy.error.load(date, stackLv, serverId);
				
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(list);
					}
				});
			}
			 
			return Status.OK_STATUS;
		}
	}
	
	class LoadLongdayErrorSummaryJob extends Job {

        MapPack param;
        long stime;
        long etime;

        public LoadLongdayErrorSummaryJob(MapPack param, long stime, long etime) {
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
                    packList.add(tcp.getSingle(RequestCmd.LOAD_SERVICE_ERROR_SUMMARY, param));
                    stime += DateUtil.MILLIS_PER_DAY;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Status.CANCEL_STATUS;
            } finally {
                TcpProxy.putTcpProxy(tcp);
            }

            if (packList.size() > 0) {
                Map<Long, ErrorData> errorDataMap = new HashMap<>();
                for (Pack p : packList) {
                    MapPack m = (MapPack) p;
                    
                    ListValue errorLv = m.getList("error");
                    ListValue serviceLv = m.getList("service");
                    ListValue messageLv = m.getList("message");
                    ListValue countLv = m.getList("count");
                    ListValue txidLv = m.getList("txid");
                    ListValue sqlLv = m.getList("sql");
                    ListValue apiLv = m.getList("apicall");
                    ListValue stackLv = m.getList("fullstack");
                    for (int i = 0; i < errorLv.size(); i++) {
                    		ErrorData data = new ErrorData();
                    		data.error = errorLv.getInt(i);
                    		data.service = serviceLv.getInt(i);
                    		data.message = messageLv.getInt(i);
                    		data.count = countLv.getInt(i);
                    		data.txid = txidLv.getLong(i);
                    		data.sql = sqlLv.getInt(i);
                    		data.apicall = apiLv.getInt(i);
                    		data.fullstack = stackLv.getInt(i);
                    		long key = BitUtil.composite(data.error, data.service);
                    		if (errorDataMap.containsKey(key)) {
                    			errorDataMap.get(key).addData(data);
                    		} else {
                    			errorDataMap.put(key, data);
                    		}
                    }
                    
                    TextProxy.error.load(date, errorLv, serverId);
    					TextProxy.service.load(date, serviceLv, serverId);
    					TextProxy.error.load(date, messageLv, serverId);
    					TextProxy.sql.load(date, sqlLv, serverId);
    					TextProxy.apicall.load(date, apiLv, serverId);
    					TextProxy.error.load(date, stackLv, serverId);
                }
                ExUtil.exec(viewer.getTable(), new Runnable() {
                    public void run() {
                        viewer.setInput(errorDataMap.values());
                    }
                });
            }
            return Status.OK_STATUS;
        }
    }
	
	private static class ErrorData {
		public int error;
		public int service;
		public int message;
		public int count;
		public long txid;
		public int sql;
		public int apicall;
		public int fullstack;
		
		public void addData(ErrorData another) {
	        this.count += another.count;
	    }
	}

	protected void getSummaryData() {
		new LoadLongdayErrorSummaryJob(param, param.getLong("stime"), param.getLong("etime")).schedule();
	}

	protected String getTitle() {
		return "EXCEPTION";
	}
}