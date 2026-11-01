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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.part.ViewPart;

import scouter.client.constants.MenuStr;
import scouter.client.context.actions.OpenCxtmenuActiveServiceListAction;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.RefreshThread;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.client.xlog.views.XLogViewPainter;

public abstract class VerticalEQCommonView extends ViewPart implements RefreshThread.Refreshable {
	private static final int MARGIN = 10;
	private static final int BAR_HEIGHT = 7;
	private static final int BAR_PADDING_WIDTH = 2;
	private static final int AXIS_PADDING = 16;
	private static final int REFRESH_INTERVAL = 200;
	private static final int FETCH_INTERVAL = 2000;
	public static double CYCLE_INTERVAL = 1000;
	private static int MINIMUM_UNIT_WIDTH = 20;

	protected RefreshThread thread;
	
	protected Canvas canvas;
	private long lastFetchedTime;
	protected Set<EqData> valueSet = new TreeSet<EqData>(new EqDataComparator());
	private int unitWidth;
	
	private Image ibuffer;
	
	private ScrolledComposite scroll;
	int winXSize;
	Rectangle area;

	// Dark mode colors (cached on UI thread in createPartControl)
	private Color dmBackground;
	private Color dmForeground;
	private Color dmSubText;
	private Color dmBorder;
	private Color dmGrid;

	public void createPartControl(final Composite parent) {
		boolean dark = ColorUtil.isDarkMode();
		if (dark) {
			dmBackground = new Color(null, 30, 30, 35);
			dmForeground = new Color(null, 200, 200, 210);
			dmSubText = new Color(null, 140, 140, 150);
			dmBorder = new Color(null, 100, 100, 120);
			dmGrid = new Color(null, 55, 55, 70);
		}
		parent.setBackground(dark ? dmBackground : ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		parent.setLayout(UIUtil.formLayout(0, 0));
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = MARGIN;
		layout.marginWidth = MARGIN;
		scroll = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scroll.setLayoutData(UIUtil.formData(0, 0, 0, 0, 100, 0, 100, 0));
		canvas = new Canvas(scroll, SWT.DOUBLE_BUFFERED);
		canvas.setLayout(layout);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				try {
					area = canvas.getClientArea();
					winXSize = parent.getSize().x;
					drawEQImage(e.gc);
				} catch (Throwable t) {}
			}
		});
		canvas.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				if (unitWidth == 0 || datas == null) {
					return;
				}
				if (e.x <= AXIS_PADDING) {
					return;
				}
				int index = (e.x - AXIS_PADDING) / unitWidth;
				if (datas.length < index + 1 || datas[index].isAlive == false) {
					return;
				}
				AgentObject agent = AgentModelThread.getInstance().getAgentObject(datas[index].objHash);
				new OpenCxtmenuActiveServiceListAction(getSite().getWorkbenchWindow(), MenuStr.ACTIVE_SERVICE_LIST, datas[index].objHash, agent.getObjType(), agent.getServerId()).run();
			}
		});
		
		scroll.setContent(canvas);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		
		scroll.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				Point origin = scroll.getOrigin();
				origin.y += (event.count * -1) * 10;
				scroll.setOrigin(origin);
			}
		});
		
		//canvas.setToolTipText("Double click to list active services");
		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
	}
	
	static Font verdana12Bold = new Font(null, "Verdana", 12, SWT.BOLD);
	static Font verdana10Bold = new Font(null, "Verdana", 10, SWT.NORMAL);
	static Font verdana10Italic = new Font(null, "Verdana", 10, SWT.ITALIC);
	static Font verdana7 = new Font(null, "Verdana", 7, SWT.NORMAL);
	
	long lastDrawTime;
	
	private void drawEQImage(GC gc) {
		if (ibuffer != null) {
			if (AXIS_PADDING + (MINIMUM_UNIT_WIDTH * size) > winXSize) {
				scroll.setMinSize(canvas.computeSize(AXIS_PADDING + (MINIMUM_UNIT_WIDTH * size), SWT.DEFAULT));	
			} else {
				scroll.setMinSize(canvas.computeSize(winXSize, SWT.DEFAULT));	
			}
			if (ibuffer.isDisposed() == false) {
				gc.drawImage(ibuffer, 0, 0);
			}
		}
	}
	
	boolean onGoing = false;
	int size;
	EqData[] datas;
	
	ObjectSelectManager objSelMgr = ObjectSelectManager.getInstance();
	
	static Color black = ColorUtil.getInstance().getColor(SWT.COLOR_BLACK);
	static Color red = ColorUtil.getInstance().getColor(SWT.COLOR_RED);
	static Color dark_gary  = ColorUtil.getInstance().getColor(SWT.COLOR_GRAY);

	private boolean isDark() { return dmBackground != null; }
	private Color fg() { return isDark() ? dmForeground : black; }
	private Color subText() { return isDark() ? dmSubText : dark_gary; }
	private Color border() { return isDark() ? dmBorder : black; }

	private Map<String, Image> objectNameImageMap = new HashMap<String, Image>();

	protected void buildBars() {
		long now = TimeUtil.getCurrentTime();
		if ((now - lastDrawTime) < REFRESH_INTERVAL || area == null) {
			return;
		}
		if(onGoing)
			return;
		onGoing = true;
		int width = area.width > 100 ? area.width : 100;
		int height = area.height > 50 ? area.height : 50;
		Image img = new Image(null, width, height);
		GC gc = new GC(img);

		try {
			if (isDark()) {
				gc.setBackground(dmBackground);
				gc.fillRectangle(0, 0, width, height);
			}
			lastDrawTime = now;
			double maxValue = 0;
			ArrayList<EqData> list = new ArrayList<EqData>();
			synchronized (valueSet) {
				for (EqData e : valueSet) {
					if (objSelMgr.isUnselectedObject(e.objHash)) {
						continue;
					}
					double max = ChartUtil.getEqMaxValue(e.asd.act1 + e.asd.act2 + e.asd.act3);
					if (max > maxValue) {
						maxValue = max;
					}
					list.add(e);
				}
			}
			size = list.size();
			if (size < 1) {
				datas = new EqData[0];
				return;
			}
			datas = list.toArray(new EqData[size]);
			unitWidth = (width - AXIS_PADDING) / size;
			
			if (unitWidth < MINIMUM_UNIT_WIDTH) {
				unitWidth = MINIMUM_UNIT_WIDTH;
			}
			
			// draw horizontal line
			gc.setForeground(isDark() ? dmGrid : new Color(null, 220, 228, 255));
			gc.setLineStyle(SWT.LINE_DOT);
			for (int i = AXIS_PADDING + unitWidth; i <= width - unitWidth; i = i + unitWidth) {
				gc.drawLine(i, 0, i, height);
			}

			// draw axis line
			gc.setForeground(fg());
			gc.setLineStyle(SWT.LINE_SOLID);
			int verticalLineX = 6;
			int verticalLineY = 6;
			
			gc.drawLine(AXIS_PADDING, verticalLineY, AXIS_PADDING, height - verticalLineY);
			gc.drawLine(AXIS_PADDING, height - verticalLineY, width, height - verticalLineY);
			
			int groundWidth = area.width - verticalLineX;
			int barSpace = height - verticalLineY - (3 * BAR_HEIGHT);
			int imgWidth = unitWidth - (BAR_PADDING_WIDTH  * 2);
			int mod = (int) (TimeUtil.getCurrentTime() % CYCLE_INTERVAL);
			for (int i = 0; i < datas.length; i++) {
				// draw objName
				String objName = datas[i].displayName;
				gc.setForeground(subText());
				gc.setFont(verdana10Italic);
				int strWidth = gc.stringExtent(objName).x;			
				while (groundWidth <= (strWidth+5)) {
					objName = objName.substring(1);
					strWidth = gc.stringExtent(objName).x;
				}

				int x = (unitWidth * (i + 1)) - 1;
				int y = verticalLineY;
				
				if (objectNameImageMap.get(objName) == null) {
					//objectNameImageMap.put(objName, SingleSourceHelper.createVerticalTextImage(objName, gc.getFont(), dark_gary.getRGB(), false));
					objectNameImageMap.put(objName, createVerticalTextImage(objName, gc.stringExtent(objName).y, gc.stringExtent(objName).x, gc.getFont(), dark_gary.getRGB(), false));
				}
				
				gc.drawImage(objectNameImageMap.get(objName), x,  y);
				
				if (datas[i].isAlive == false) {
					gc.setForeground(subText());
					gc.setLineWidth(2);
					gc.drawLine(x + (gc.stringExtent(objName).y / 2), y - 1, x + (gc.stringExtent(objName).y / 2), y + gc.stringExtent(objName).x + 1);
				}
				gc.setLineWidth(1);
				ActiveSpeedData asd = datas[i].asd;
				long total = asd.act1 + asd.act2 + asd.act3;
				double reach = barSpace * (total / maxValue);	
				int barY = height - verticalLineY - 8;
				if (total > 0) {
					try {
						// distribute bars to 3 types
						int noOfBars = (int) reach / BAR_HEIGHT;
						int noOfAct1 = (int) (noOfBars * ((double)asd.act1 / total));
						int noOfAct2 = (int) (noOfBars * ((double)asd.act2  / total));
						int noOfAct3 = (int) (noOfBars * ((double)asd.act3 / total));
						int sediments = noOfBars - (noOfAct1 + noOfAct2 + noOfAct3);
						while (sediments >= 0) {
							if (asd.act3 > 0) {
								noOfAct3++;
								sediments--;
							}
							if (sediments >= 0 && asd.act2 > 0) {
								noOfAct2++;
								sediments--;
							}
							if (sediments >= 0 && asd.act1 > 0) {
								noOfAct1++;
								sediments--;
							}
						}
						
						int barX = AXIS_PADDING + ((unitWidth * i) + BAR_PADDING_WIDTH);
						Color lastColor = null;
						
						for (int j = 0; j < noOfAct3; j++) {
							// draw red bar
							drawNemo(gc, ColorUtil.getInstance().ac3, barX + 1, barY + 1, imgWidth - 2, BAR_HEIGHT - 2);
							barY -= BAR_HEIGHT;
							lastColor = ColorUtil.getInstance().ac3; 
						}
						for (int j = 0; j < noOfAct2; j++) {
							// draw yellow bar
							drawNemo(gc, ColorUtil.getInstance().ac2, barX + 1, barY + 1, imgWidth - 2, BAR_HEIGHT - 2);
							barY -= BAR_HEIGHT;
							lastColor = ColorUtil.getInstance().ac2; 
						}
						for (int j = 0; j < noOfAct1; j++) {
							// draw blue bar
							drawNemo(gc, ColorUtil.getInstance().ac1, barX + 1, barY + 1, imgWidth - 2, BAR_HEIGHT - 2);
							barY -= BAR_HEIGHT;
							lastColor = ColorUtil.getInstance().ac1; 
						}
						
						// draw tong-tong bar
						if (lastColor != null) {
							drawNemo(gc, lastColor, barX + 1, barY + 1 - ((int) calculateReach(mod, BAR_HEIGHT * 0.7d)), imgWidth - 2, BAR_HEIGHT - 2);
						}
					} catch (Throwable th) {
						th.printStackTrace();
					}
				}
				
				// draw count text
				if (datas[i].isAlive) {
					gc.setFont(verdana10Bold);
					gc.setForeground(fg());
					String v = Long.toString(total);
					
					String all = "(" +  Long.toString(asd.act3) + " / " + Long.toString(asd.act2) + " / " + Long.toString(asd.act1) + ")";
					
					int xaxis = AXIS_PADDING + (unitWidth * i) + ((unitWidth - gc.stringExtent(v).x) / 2);
					int yaxis = barY - (BAR_HEIGHT * 2) - 4;
					
					if (total > 0 && unitWidth >= 52) {
						yaxis -= gc.stringExtent(all).y - 2; 
					}
					gc.drawString(v, xaxis, yaxis, true);
					
					if (total > 0 && unitWidth >= 52) {
						yaxis += gc.stringExtent(all).y + 2; 

						gc.setFont(verdana7);
						xaxis = AXIS_PADDING + (unitWidth * i) + ((unitWidth - gc.stringExtent(all).x) / 2) - (Long.toString(total).length() * 2);
						v = "(";
						gc.drawString(v, xaxis, yaxis, true);
	
						xaxis += gc.stringExtent(v).x + 1;
						gc.setForeground(ColorUtil.getInstance().ac3);
						v = Long.toString(asd.act3);
						gc.drawString(v, xaxis, yaxis, true);
						
						xaxis += gc.stringExtent(v).x + 1;
						gc.setForeground(fg());
						v = " / ";
						gc.drawString(v, xaxis, yaxis, true);
						
						xaxis += gc.stringExtent(v).x + 1;
						gc.setForeground(ColorUtil.getInstance().ac2);
						v = Long.toString(asd.act2);
						gc.drawString(v, xaxis, yaxis, true);
						
						xaxis += gc.stringExtent(v).x + 1;
						gc.setForeground(fg());
						v = " / ";
						gc.drawString(v, xaxis, yaxis, true);
						
						xaxis += gc.stringExtent(v).x + 1;
						gc.setForeground(ColorUtil.getInstance().ac1);
						v = Long.toString(asd.act1);
						gc.drawString(v, xaxis, yaxis, true);
						
						xaxis += gc.stringExtent(v).x + 1;
						gc.setForeground(fg());
						v = ")";
						gc.drawString(v, xaxis, yaxis, true);
					}
				}
			}
			
			// draw scale text
			gc.setForeground(fg());
			gc.setFont(verdana7);
			int max = (int) maxValue;
			String v = Integer.toString(max);
			String v2 = Integer.toString(max / 2);			
			gc.drawString(v, 2, 4, true);
			gc.drawString(v2, 2, verticalLineY + ((height - verticalLineY) / 2) - gc.stringExtent(v2).y, true);
			gc.drawString("0", 2, height - gc.stringExtent(v).y - 4, true);
		} catch (Throwable th) { th.printStackTrace(); }
		finally {
			gc.dispose();
			Image old = ibuffer;
			ibuffer = img;
			if (old != null) {
				old.dispose();
			}
			onGoing = false;
		}
	}
	
	private void drawNemo(GC gc, Color background, int x, int y, int width, int height) {
		gc.setBackground(background);
		gc.fillRectangle(x, y, width, height);
		gc.setForeground(border());
		gc.drawRectangle(x, y, width, height);
	}

	public void setFocus() {
		 scroll.setFocus();
	}
	
	
	private double calculateReach(int mod, double weight) {
		return (Math.cos(mod * (Math.PI / (CYCLE_INTERVAL / 2.0)) + Math.PI) + 1) / 2 * weight;
	}
	
	public void refresh() {
		buildBars();
		long now = TimeUtil.getCurrentTime();
		if (now >= lastFetchedTime + FETCH_INTERVAL) {
			lastFetchedTime = now;
			valueSet.clear();
			fetch();
		}
		ExUtil.syncExec(canvas, new Runnable() {
			public void run() {
				canvas.redraw();
			}
		});
	}
	
	public abstract void fetch();
	
	public void dispose() {
		super.dispose();
		if (ibuffer != null && ibuffer.isDisposed() == false) {
			ibuffer.dispose();
		}
		if (this.thread != null) {
			this.thread.shutdown();
		}
	}
	
	public static class ActiveSpeedData {
		public int act1;
		public int act2;
		public int act3;
	}
	
	public static class EqData {
		public int objHash;
		public boolean isAlive;
		public String displayName;
		public ActiveSpeedData asd;
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + objHash;
			return result;
		}
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EqData other = (EqData) obj;
			if (objHash != other.objHash)
				return false;
			return true;
		}
		
		public String toString() {
			return "EqData [objHash=" + objHash + ", displayName="
					+ displayName + "]";
		}
	}
	
	public class EqDataComparator implements Comparator<EqData> {
		public int compare(EqData o1, EqData o2) {
			int comp = o1.displayName.compareTo(o2.displayName);
			if (comp == 0) {
				return o1.objHash - o2.objHash;
			}
			return comp;
		}
	}
	
	private Image createVerticalTextImage(String text, int w, int h, Font font, RGB color, boolean upToDown) {
		Image image = new Image(Display.getCurrent(), w, h);

		final GC gc = new GC(image);
		final Color titleColor = new Color(Display.getCurrent(), color);
		RGB transparentRGB = new RGB(240, 240, 240);

		gc.setBackground(XYGraphMediaFactory.getInstance().getColor(transparentRGB));
		gc.fillRectangle(image.getBounds());
		gc.setForeground(titleColor);
		gc.setFont(font);
		final Transform tr = new Transform(Display.getCurrent());
		if (!upToDown) {
			tr.translate(0, h);
			tr.rotate(-90);
			gc.setTransform(tr);
		} else {
			tr.translate(w, 0);
			tr.rotate(90);
			gc.setTransform(tr);
		}
		gc.drawText(text, 0, 0);
		tr.dispose();
		gc.dispose();
		final ImageData imageData = image.getImageData();
		image.dispose();
		titleColor.dispose();
		imageData.transparentPixel = imageData.palette.getPixel(transparentRGB);
		image = new Image(Display.getCurrent(), imageData);
		return image;
	}
}