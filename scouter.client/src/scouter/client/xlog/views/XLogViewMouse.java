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
package scouter.client.xlog.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import scouter.client.model.XLogData;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

import java.util.ArrayList;
import java.util.Enumeration;

public class XLogViewMouse implements MouseListener, MouseMoveListener {
	
	enum SelectAreaMode {
		LIST_XLOG,
		ZOOM_AREA,
	}
	
	public SelectAreaMode mode = null;
	
	public int x1 = -1;
	public int y1 = -1;
	public int x2 = -1;
	public int y2 = -1;
	private Canvas canvas;
	private LongKeyLinkedMap<XLogData> data;
	private String objType;
	
	XLogViewPainter viewPainter;
	
	public XLogViewMouse(LongKeyLinkedMap<XLogData> data, Canvas canvas) {
		this.data = data;
		this.canvas = canvas;
	}
	
	public void setPainter(XLogViewPainter viewPainter){
		this.viewPainter = viewPainter;
	}
	
	public void setObjType(String objType){
		this.objType = objType;
	}
	
	public void mouseDoubleClick(MouseEvent e) {
		x1 = x2 = y1 = y2 = -1;
		if (viewPainter.onMouseDoubleClick(e.x, e.y)) {
			canvas.redraw();
		}
	}
	
	public void setNewYValue() {
		canvas.redraw();
	}

	public void mouseDown(MouseEvent e) {
		x1 = x2 = y1 = y2 = -1;
		switch(e.button) {
		case 1 :
			if ((e.stateMask & SWT.SHIFT) != 0) {
				mode = SelectAreaMode.ZOOM_AREA;
			} else {
				mode = SelectAreaMode.LIST_XLOG;
			}
			break;
		default :
			mode = null;
			break;
		}
		if (mode != null) {
			x1 = e.x;
			y1 = e.y;
			if (viewPainter.onMouseClick(x1, y1)) {
				canvas.redraw();
			}
		}
	}

	long last_draw = System.currentTimeMillis();
	public String yyyymmdd;

	public void mouseMove(MouseEvent e) {
		if (mode != null && x1 >= 0 && y1 >= 0) {
			x2 = e.x;
			y2 = e.y;
			long now = System.currentTimeMillis();
			if (now > last_draw + 50) {
				last_draw = now;
				canvas.redraw();
			}
		} else {
			x1 = x2 = y1 = y2 = -1;
		}

	};

	public void mouseUp(MouseEvent e) {
		if (mode != null && x1 >= 0 && x2 >= 0 && y1 >= 0 && y2 >= 0) {
			switch (mode) {
				case LIST_XLOG:
					listSelectedXLog(e.display, x1, y1, x2, y2);
					break;
				case ZOOM_AREA:
					zoominArea(e.display, x1, y1, x2, y2);
					break;
			}
		}
		x1 = x2 = y1 = y2 = -1;
		canvas.redraw();
		mode = null;
	}

	public void listSelectedXLog(final Display display, int x1, int y1, int x2, int y2) {
		int txCnt = 0;
		ArrayList<XLogData> selectedData = new ArrayList<XLogData>();
		Enumeration<XLogData> en = data.values();
		while (en.hasMoreElements()) {
			XLogData item = en.nextElement();
			if (inRect(x1, y1, x2, y2, item.x, item.y) 
					&& (item.filter_ok)) {
				txCnt++;
				if (selectedData.size() < PManager.getInstance().getInt(PreferenceConstants.P_XLOG_DRAG_MAX_COUNT)) {
					selectedData.add(item);
				}
			}
		}
		if (txCnt < 1) {
			ConsoleProxy.info("[XLog] no xlogs in selected area.");
			return;
		}
		ConsoleProxy.info("[XLog] "+txCnt + " selected.");

		try {
			IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			XLogSelectionView view = (XLogSelectionView) win.getActivePage().showView(XLogSelectionView.ID,//
					objType, IWorkbenchPage.VIEW_ACTIVATE);
			view.setInput(selectedData, objType, yyyymmdd);
		} catch (Exception d) {
		}

	}
	
	public void zoominArea(final Display display, final int x1, final int y1, final int x2, final int y2) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				final LongKeyLinkedMap<XLogData> zoomData = new LongKeyLinkedMap<XLogData>();
				LongEnumer enumer = data.keys();
				double max = 0;
				double min = Double.MAX_VALUE;
				while (enumer.hasMoreElements()) {
					long key = enumer.nextLong();
					XLogData item = data.get(key);
					if (inRect(x1, y1, x2, y2, item.x, item.y) 
							&& (item.filter_ok)) {
						switch(viewPainter.yAxisMode) {
							case ELAPSED:
								if (item.p.elapsed / 1000d > max) {
									max = item.p.elapsed / 1000d;
								}
								if (item.p.elapsed / 1000d < min) {
									min = item.p.elapsed / 1000d;
								}
								break;
							case CPU:
								if (item.p.cpu > max) {
									max = item.p.cpu;
								}
								if (item.p.cpu < min) {
									min = item.p.cpu;
								}
								break;
							case SQL_TIME:
								if (item.p.sqlTime / 1000d > max) {
									max = item.p.sqlTime / 1000d;
								}
								if (item.p.sqlTime / 1000d < min) {
									min = item.p.sqlTime / 1000d;
								}
								break;
							case SQL_COUNT:
								if (item.p.sqlCount > max) {
									max = item.p.sqlCount;
								}
								if (item.p.sqlCount < min) {
									min = item.p.sqlCount;
								}
								break;
							case APICALL_TIME:
								if (item.p.apicallTime / 1000d > max) {
									max = item.p.apicallTime / 1000d;
								}
								if (item.p.apicallTime / 1000d < min) {
									min = item.p.apicallTime / 1000d;
								}
								break;
							case APICALL_COUNT:
								if (item.p.apicallCount > max) {
									max = item.p.apicallCount;
								}
								if (item.p.apicallCount < min) {
									min = item.p.apicallCount;
								}
								break;
							case HEAP_USED:
								if (item.p.kbytes > max) {
									max = item.p.kbytes;
								}
								if (item.p.kbytes < min) {
									min = item.p.kbytes;
								}
								break;
							default:
								if (item.p.elapsed / 1000d> max) {
									max = item.p.elapsed / 1000d;
								}
								if (item.p.elapsed / 1000d< min) {
									min = item.p.elapsed / 1000d;
								}
								break;
						}
						zoomData.put(key, new XLogData(item.p, item.serverId));
					}
				}
				if (zoomData.isEmpty()) return;
				final long stime = zoomData.getFirstValue().p.endTime - 500;
				final long etime = zoomData.getLastValue().p.endTime + 500;
				final double yMax = max * 1.01;
				final double yMin = min * 0.99;
				
				ExUtil.exec(display, new Runnable() {
					public void run() {
						try {
							ConsoleProxy.info("[XLog] "+ zoomData.size() + " selected.");
							IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
							XLogZoomTimeView view = (XLogZoomTimeView) win.getActivePage().showView(XLogZoomTimeView.ID,//
									objType+stime+etime+yMax+yMin, IWorkbenchPage.VIEW_ACTIVATE);
							if (view != null) {
								view.setInput(stime, etime, yMax, yMin, zoomData, objType, viewPainter.yAxisMode);
							}
						} catch (Exception d) {
						}
					}
				});
			}
		});
	}

	public boolean inRect(int x, int y, int tx, int ty, int sx, int sy) {
		if (x > tx) {
			int ttx = x;
			x = tx;
			tx = ttx;
		}
		if (y > ty) {
			int tty = y;
			y = ty;
			ty = tty;
		}
		return (x < sx && y < sy && tx > sx && ty > sy);
	}
	
}
