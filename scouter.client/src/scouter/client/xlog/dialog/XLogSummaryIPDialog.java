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
package scouter.client.xlog.dialog;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import scouter.client.model.XLogData;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.util.ExUtil;
import scouter.util.DateUtil;
import scouter.util.IPUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;
import scouter.util.TopN;
import scouter.util.TopN.DIRECTION;

public class XLogSummaryIPDialog extends XLogSummaryAbstractDialog{
	
	public XLogSummaryIPDialog(Display display, LongKeyLinkedMap<XLogData> dataMap) {
		super(display, dataMap);
	}
	
	protected void calcAsync() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				Map<String, IpSummary> summaryMap = new HashMap<String, IpSummary>();
				LongEnumer longEnumer = dataMap.keys();
				while (longEnumer.hasMoreElements()) {
					XLogData d = dataMap.get(longEnumer.nextLong());
					long time = d.p.endTime;
					if (d.filter_ok && time >= stime && time <= etime && !ObjectSelectManager.getInstance().isUnselectedObject(d.p.objHash)) {
						String ip = IPUtil.toString(d.p.ipaddr);
						IpSummary summary = summaryMap.get(ip);
						if (summary == null) {
							summary = new IpSummary();
							summary.ip = ip;
							summaryMap.put(ip, summary);
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
						summary.memory += d.p.kbytes;
						summary.sqltime += d.p.sqlTime;
						summary.apicalltime += d.p.apicallTime;
					}
				}
				
				final TopN<IpSummary> stn = new TopN<IpSummary>(10000, DIRECTION.DESC);
				for (IpSummary so : summaryMap.values()) {
					stn.add(so);
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						rangeLabel.setText(DateUtil.format(stime, "yyyy-MM-dd HH:mm:ss") + " ~ " + DateUtil.format(etime, "HH:mm:ss") + " (" + stn.size() +")");
						viewer.setInput(stn.getList());
					}
				});
			}
		});
	}
	
	
	private static class IpSummary extends SummaryObject implements Comparable<IpSummary> {
		String ip;

		public int compareTo(IpSummary o) {
			return this.count - o.count;
		}
	}

	public String getTitle() {
		return "IP Summary";
	}

	protected void createMainColumn() {
		TableViewerColumn c = createTableViewerColumn("IP", 150, SWT.CENTER, false);
		ColumnLabelProvider labelProvider = new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof IpSummary) {
					return ((IpSummary) element).ip;
				}
				return null;
			}
		};
		c.setLabelProvider(labelProvider);
	}

}
