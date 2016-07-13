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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.util.ExUtil;
import scouter.util.DateUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;
import scouter.util.TopN;
import scouter.util.TopN.DIRECTION;

public class XLogSummaryRefererDialog extends XLogSummaryAbstractDialog{
	
	public XLogSummaryRefererDialog(Display display, LongKeyLinkedMap<XLogData> dataMap) {
		super(display, dataMap);
	}
	
	protected void calcAsync() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				final Map<Integer, RefererSummary> summaryMap = new HashMap<Integer, RefererSummary>();
				Map<Integer, List<Integer>> loadTextMap = new HashMap<Integer, List<Integer>>();
				LongEnumer longEnumer = dataMap.keys();
				while (longEnumer.hasMoreElements()) {
					XLogData d = dataMap.get(longEnumer.nextLong());
					long time = d.p.endTime;
					if (d.filter_ok && time >= stime && time <= etime && !ObjectSelectManager.getInstance().isUnselectedObject(d.p.objHash)) {
						RefererSummary summary = summaryMap.get(d.p.referer);
						if (summary == null) {
							summary = new RefererSummary(d.p.referer);
							summaryMap.put(d.p.referer, summary);
							List<Integer> loadTextList = loadTextMap.get(d.serverId);
							if (loadTextList == null) {
								loadTextList = new ArrayList<Integer>();
								loadTextMap.put(d.serverId, loadTextList);
							}
							loadTextList.add(d.p.referer);
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
				for (Integer serverId : loadTextMap.keySet()) {
					TextProxy.referer.load(DateUtil.yyyymmdd(etime), loadTextMap.get(serverId), serverId);
				}
				final TopN<RefererSummary> stn = new TopN<RefererSummary>(10000, DIRECTION.DESC);
				for (RefererSummary so : summaryMap.values()) {
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
	
	
	private static class RefererSummary extends SummaryObject implements Comparable<RefererSummary>{
		int hash;
		RefererSummary(int hash) {
			this.hash = hash;
		}
		public int compareTo(RefererSummary o) {
			return this.count - o.count;
		}
	}

	public String getTitle() {
		return "Referer Summary";
	}

	protected void createMainColumn() {
		TableViewerColumn c = createTableViewerColumn("Referer", 200, SWT.LEFT, false);
		ColumnLabelProvider labelProvider = new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof RefererSummary) {
					return TextProxy.referer.getText(((RefererSummary) element).hash);
				}
				return null;
			}
		};
		c.setLabelProvider(labelProvider);
	}

}
