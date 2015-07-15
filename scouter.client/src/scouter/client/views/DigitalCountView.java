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
package scouter.client.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.model.RefreshThread;
import scouter.client.net.TcpProxy;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;

public class DigitalCountView extends ViewPart implements RefreshThread.Refreshable {
	
	public static final String ID = DigitalCountView.class.getName();
	
	private Canvas canvas;
	protected RefreshThread thread;
	private String value = "unavailable";
	private int serverId;
	
	String title = "";
	String requestCmd;
	MapPack param;
	
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
	}

	@Override
	public void createPartControl(Composite parent) {
		canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		canvas.setLayout(layout);
		canvas.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				drawText(canvas.getClientArea(), e.gc);
			}
		});
		
		canvas.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {}
			boolean lock = false;
			public void controlResized(ControlEvent e) {
				if (!lock) {
					lock = true;
					canvas.redraw();
					lock = false;
				}
			}
		});
	}
	
    public void setInput(String title, String requestCmd, MapPack param){
    	this.title = title == null ? "" : title;
    	this.requestCmd = requestCmd;
    	this.param = param;
    	if (thread == null) {
	    	thread = new RefreshThread(this, 2000);
			thread.start();
    	}
    }
	
	private void drawText(Rectangle area, GC gc) {
		try {
			int fontSize = area.height / 2;
			Font font = new Font(null, "Arial", fontSize ,SWT.BOLD);
			gc.setFont(font);
			int stringLength = gc.stringExtent(value).x;
			if (stringLength > area.width) {
				int fontSize2 = area.width / value.length();
				if (fontSize2 < fontSize) {
					fontSize = fontSize2;
					Font oldfont = gc.getFont();
					font = new Font(null, "Arial", fontSize ,SWT.BOLD);
					gc.setFont(font);
					oldfont.dispose();
					stringLength = gc.stringExtent(value).x;
				}
			}
			int x = (area.width/2) - (stringLength/2);
			int y = (area.height/3) - (fontSize / 2);
			if (x < 1) {
				x = 1;
			}
			gc.drawText(value, x, y, true);
			font.dispose();
			
			// draw title
			fontSize = area.height / 20;
			if (fontSize < 10) {
				fontSize = 10;
			}
			font = new Font(null, "Arial", fontSize ,SWT.BOLD);
			gc.setFont(font);
			gc.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_GRAY));
			stringLength = gc.stringExtent(title).x;
			x = (area.width/2) - (stringLength/2);
			if (x < 1) {
				x = 1;
			}
			gc.drawText(title, x, 1, true);
			font.dispose();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	
	@Override
	public void dispose() {
		super.dispose();
		if (thread != null && thread.isAlive()) {
			thread.shutdown();
		}
	}


	@Override
	public void setFocus() {
	}

	public void refresh() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Value v = null;
		try {
			v = tcp.getSingleValue(requestCmd, param);
		} catch(Exception e){
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (v != null) {
			value = FormatUtil.print(CastUtil.clong(v), "#,##0");
		} else {
			value = "unavailable";
		}
		ExUtil.exec(canvas, new Runnable() {
			public void run() {
				canvas.redraw();
			}
		});
	}

}
