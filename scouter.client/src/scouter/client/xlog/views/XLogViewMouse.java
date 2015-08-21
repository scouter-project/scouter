/*
 *  Copyright 2015 LG CNS.
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

import java.util.ArrayList;
import java.util.Enumeration;

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
import scouter.client.util.ConsoleProxy;
import scouter.client.util.TimeUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

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

	long last_draw = TimeUtil.getCurrentTime();
	public String yyyymmdd;

	public void mouseMove(MouseEvent e) {
		if (mode != null && x1 >= 0 && y1 >= 0) {
			x2 = e.x;
			y2 = e.y;
			long now = TimeUtil.getCurrentTime();
			if (now > last_draw + 100) {
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
				if (selectedData.size() < 200) {
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
	
	public void zoominArea(final Display display, int x1, int y1, int x2, int y2) {
		int txCnt = 0;
		LongKeyLinkedMap<XLogData> zoomData = new LongKeyLinkedMap<XLogData>();
		LongEnumer enumer = data.keys();
		double max = 0;
		double min = Double.MAX_VALUE;
		while (enumer.hasMoreElements()) {
			long key = enumer.nextLong();
			XLogData item = data.get(key);
			if (inRect(x1, y1, x2, y2, item.x, item.y) 
					&& (item.filter_ok)) {
				txCnt++;
				if (item.p.elapsed > max) {
					max = item.p.elapsed;
				}
				if (item.p.elapsed < min) {
					min = item.p.elapsed;
				}
				zoomData.put(key, new XLogData(item.p, item.serverId));
			}
		}
		if (txCnt < 1) {
			ConsoleProxy.info("[XLog] no xlogs in selected area.");
			return;
		}
		ConsoleProxy.info("[XLog] "+txCnt + " selected.");
		long stime = zoomData.getFirstValue().p.endTime - 500;
		long etime = zoomData.getLastValue().p.endTime + 500;
		max *= 1.01;
		min *= 0.99;
		
		try {
			IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			XLogZoomTimeView view = (XLogZoomTimeView) win.getActivePage().showView(XLogZoomTimeView.ID,//
					objType+stime+etime+max+min, IWorkbenchPage.VIEW_ACTIVATE);
			if (view != null) {
				view.setInput(stime, etime, max / 1000, min / 1000, zoomData, objType);
			}
		} catch (Exception d) {
		}
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
