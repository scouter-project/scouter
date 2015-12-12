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
import org.eclipse.ui.part.ViewPart;

import scouter.client.util.ColorUtil;

public class DigitalCountView extends ViewPart {
	
	public static final String ID = DigitalCountView.class.getName();
	
	protected Canvas canvas;
	protected String value = "DigitalCount";
	protected String title = "";
	
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
			int y = (area.height/2) - (fontSize / 2);
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
			y = y -fontSize - 10;
			gc.drawText(title, x, 1 > y ? 1 : y, true);
			font.dispose();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	
	@Override
	public void setFocus() {
	}
}
