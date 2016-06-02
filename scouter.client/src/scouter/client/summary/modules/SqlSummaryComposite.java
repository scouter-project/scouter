package scouter.client.summary.modules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.popup.SQLFormatDialog;
import scouter.client.util.ExUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.FormatUtil;

public class SqlSummaryComposite extends AbstractSummaryComposite {
	
	public SqlSummaryComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	protected void createColumns() {
		for (SqlColumnEnum column : SqlColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case SQL:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SummaryData) {
							return TextProxy.sql.getText(((SummaryData) element).hash);
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
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof SummaryData) {
					SummaryData data = (SummaryData) o;
					String sql = TextProxy.sql.getText(data.hash);
					new SQLFormatDialog().show(sql, null);
				}
			}
		});
	}
	
	enum SqlColumnEnum {

	    SQL("SQL", 150, SWT.CENTER, true, true, false),
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

	    private SqlColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	class LoadSqlSummaryJob extends Job {
		
		MapPack param;

		public LoadSqlSummaryJob(MapPack param) {
			super("Loading...");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			Pack p = null;
			try {
				p = tcp.getSingle(RequestCmd.LOAD_SQL_SUMMARY, param);
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
				TextProxy.sql.load(date, idLv, serverId);
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(list);
					}
				});
			}
			 
			return Status.OK_STATUS;
		}
	}

	protected void getSummaryData() {
		new LoadSqlSummaryJob(param).schedule();
	}

	protected String getTitle() {
		return "SQL";
	}
}
