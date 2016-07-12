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
package scouter.client.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.RefreshThread;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;


public abstract class ActiveSpeedCommonView extends ViewPart implements RefreshThread.Refreshable {

	public final int BAR_WIDTH = 8;
	private static final int FETCH_INTERVAL = 2000;
	private static final int HEIGHT_MARGIN = 3;
	
	protected RefreshThread thread;
	private Canvas canvas;
	
	private Image ibuffer;
	Rectangle area;

	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
	public void createPartControl(Composite parent) {
		setTitleImage(Images.TYPE_ACTSPEED);
		canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				try {
					area = canvas.getClientArea();
					drawCanvasImage(e.gc);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
		
		window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}
	
	private void drawCanvasImage(GC gc) {
		if (ibuffer != null && ibuffer.isDisposed() == false) {
			gc.drawImage(ibuffer, 0, 0);
		}
	}
	
	public static class ActiveSpeedData {
		public int act1;
		public int act2;
		public int act3;
		public float tps;
	}

	protected ActiveSpeedData activeSpeedData = new ActiveSpeedData();
	protected int update = 0;
	protected final static int UPDATE_FACTOR = 24;
	protected long last_update = 0;

	public void refresh() {
		update++;
		if (update == UPDATE_FACTOR) {
			update = 0;
		}
		draw();
		ExUtil.syncExec(canvas, new Runnable() {
			public void run() {
				canvas.redraw();
			}
		});
		final long now = TimeUtil.getCurrentTime();
		if (now >= last_update + FETCH_INTERVAL) {
			ExUtil.asyncRun(new Runnable() {
				public void run() {
					last_update = now;
					fetch();
				}
			});
		}
	}

	abstract public void fetch(); 
	
	private boolean onGoing; 
	private long lastDrawTime;

	protected void draw() {
		long now = TimeUtil.getCurrentTime();
		if ((now - lastDrawTime) < 100L || area == null) {
			return;
		}
		if(onGoing)
			return;
		onGoing = true;

		ActiveSpeedData data = activeSpeedData;

		int work_w = area.width < 400 ? 400 : area.width;
		int work_h = area.height < 20 ? 20 : area.height;
		
		Image img = new Image(null, work_w, work_h);
		GC gc = new GC(img);
		
		try {
			lastDrawTime = now;
			int barWidth = BAR_WIDTH - 2;
			int barHehgit = work_h - (HEIGHT_MARGIN * 2);
			int idx = 0;
			for (int i = 0; i < data.act3; i++, idx++) {
				drawNemo(gc, ColorUtil.getInstance().ac3, (idx * BAR_WIDTH) + 1, HEIGHT_MARGIN, barWidth, barHehgit);
			}
			for (int i = 0; i < data.act2; i++, idx++) {
				drawNemo(gc, ColorUtil.getInstance().ac2, (idx * BAR_WIDTH) + 1, HEIGHT_MARGIN, barWidth, barHehgit);
			}
			for (int i = 0; i < data.act1; i++, idx++) {
				drawNemo(gc, ColorUtil.getInstance().ac1, (idx * BAR_WIDTH) + 1, HEIGHT_MARGIN, barWidth, barHehgit);
			}
	
			// gc.setAlpha(150);
			int mod = 0;
			if (data.tps > 5000) {
				mod = 24;
			} else if (data.tps > 1000) {
				mod = 12;
			} else if (data.tps > 200) {
				mod = 8;
			} else if (data.tps > 80) {
				mod = 6;
			} else if (data.tps > 40) {
				mod = 4;
			} else if (data.tps >= 20) {
				mod = 3;
			} else if (data.tps >= 10) {
				mod = 2;
			} else if (data.tps > 0) {
				mod = 1;
			}
			// System.out.println("mod="+mod + " data.tps="+data.tps);
	
			// gc.setAlpha(50);
			for (int i = idx; i < work_w / BAR_WIDTH; i++) {
				if (mod > 0 && (i % (UPDATE_FACTOR / mod) == update % (UPDATE_FACTOR / mod))) {
					drawNemo(gc, ColorUtil.getInstance().acm, (i * BAR_WIDTH + 1), HEIGHT_MARGIN, barWidth, barHehgit);
				} else {
					drawNemo(gc, white, (i * BAR_WIDTH + 1), HEIGHT_MARGIN, barWidth, barHehgit);
				}
			}
			
			if (work_h >= 35) {
				Font font = new Font(null, "Verdana", 14, SWT.ITALIC);
				gc.setFont(font);
				gc.drawText(Integer.toString(idx), 5, work_h / 2 - 8, true);
				font.dispose();
			}
			
		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			gc.dispose();
			Image old = ibuffer;
			ibuffer = img;
			if (old != null) {
				old.dispose();
			}
			onGoing = false;
		}
	}
	
	static Color black = ColorUtil.getInstance().getColor(SWT.COLOR_BLACK);
	static Color white = ColorUtil.getInstance().getColor(SWT.COLOR_WHITE);
	
	private void drawNemo(GC gc, Color background, int x, int y, int width, int height) {
		gc.setBackground(background);
		gc.fillRectangle(x, y, width, height);
		gc.setForeground(black);
		gc.drawRectangle(x, y, width, height);
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
		if (thread != null) {
			thread.shutdown();
		}
	}
}
