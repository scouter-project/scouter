/*
*  Copyright 2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import scouter.client.model.XLogData;
import scouter.client.util.ExUtil;
import scouter.client.xlog.dialog.XLogSummaryAbstractDialog.SummaryObject;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;
import scouter.util.Order;
import scouter.util.OrderUtil;
import scouter.util.TopN;

public class XLogSummaryUserDialog extends XLogSummaryAbstractDialog{
	
	public XLogSummaryUserDialog(Display display, LongKeyLinkedMap<XLogData> dataMap) {
		super(display, dataMap);
	}
	
	protected void calcAsync() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				Map<Long, UserSummary> summaryMap = new HashMap<Long, UserSummary>();
				LongEnumer longEnumer = dataMap.keys();
				while (longEnumer.hasMoreElements()) {
					XLogData d = dataMap.get(longEnumer.nextLong());
					long time = d.p.endTime;
					if (d.filter_ok && time >= stime && time <= etime) {
						UserSummary summary = summaryMap.get(d.p.userid);
						if (summary == null) {
							summary = new UserSummary();
							summary.id = d.p.userid;
							summaryMap.put(d.p.userid, summary);
						}
						summary.count++;
						summary.sumTime += d.p.elapsed;
						if (d.p.elapsed > summary.maxTime) {
							summary.maxTime = d.p.elapsed;
						}
						if (d.p.error != 0) {
							summary.error++;
						}
						summary.cpu += d.p.cpu;
						summary.memory = d.p.bytes;
						summary.sqltime = d.p.sqlTime;
						summary.apicalltime = d.p.apicallTime;
					}
				}
				final TopN<SummaryObject> tn = new TopN<SummaryObject>(10000) {
					public Order order(SummaryObject o1, SummaryObject o2) {
						return OrderUtil.desc(o1.count, o2.count);
					}
				};
				for (SummaryObject so : summaryMap.values()) {
					tn.add(so);
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						rangeLabel.setText(DateUtil.format(stime, "yyyy-MM-dd HH:mm:ss") + " ~ " + DateUtil.format(etime, "HH:mm:ss") + " (" + tn.size() +")");
						viewer.setInput(tn.getList());
					}
				});
			}
		});
	}
	
	
	private static class UserSummary extends SummaryObject {
		long id;
	}

	public String getTitle() {
		return "User Summary";
	}

	protected void createMainColumn() {
		TableViewerColumn c = createTableViewerColumn("User ID", 100, SWT.LEFT, true);
		ColumnLabelProvider labelProvider = new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof UserSummary) {
					return CastUtil.cString(((UserSummary) element).id);
				}
				return null;
			}
		};
		c.setLabelProvider(labelProvider);
	}

}
