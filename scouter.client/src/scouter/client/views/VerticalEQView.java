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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.csstudio.swt.xygraph.util.SingleSourceHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.model.AgentModelThread;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.xlog.views.XLogViewPainter;
import scouter.io.DataInputX;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class VerticalEQView extends VerticalEQCommonView {
	public static final String ID = VerticalEQView.class.getName();
	
	private int serverId;
	protected String objType;	
	
	private Map<String, Image> objectNameImageMap = new HashMap<String, Image>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String[] ids = site.getSecondaryId().split("&");
		serverId = Integer.valueOf(ids[0]);
		objType = ids[1];
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server != null) {
			CounterEngine counterEngine = server.getCounterEngine();
			String displayName = counterEngine.getDisplayNameObjectType(objType);
			this.setPartName("Active Service Vertical EQ - " + displayName);
		}
		super.createPartControl(parent);
	}

	public void fetch() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objType", objType);
			tcp.process(RequestCmd.ACTIVESPEED_REAL_TIME, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					MapPack m = (MapPack) in.readPack();
					ActiveSpeedData asd = new ActiveSpeedData();
					asd.act1 = CastUtil.cint(m.get("act1"));
					asd.act2 = CastUtil.cint(m.get("act2"));
					asd.act3 = CastUtil.cint(m.get("act3"));
					int objHash = CastUtil.cint(m.get("objHash"));
					EqData data = new EqData();
					data.objHash = objHash;
					data.asd = asd;
					data.displayName = ScouterUtil.getFullObjName(objHash);
					data.isAlive = AgentModelThread.getInstance().getAgentObject(objHash).isAlive();
					valueSet.add(data);
				}
			});
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}
	
	public void buildBars() {
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
			gc.setForeground(XLogViewPainter.color_grid_narrow);
			gc.setLineStyle(SWT.LINE_DOT);

			for (int i = AXIS_PADDING + unitWidth; i <= width - unitWidth; i = i + unitWidth) {
				gc.drawLine(i, 0, i, width);
			}
			
			// draw axis line
			gc.setForeground(black);
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
				gc.setForeground(dark_gary);
				gc.setFont(verdana10Italic);
				int strWidth = gc.stringExtent(objName).x;
				
				while (groundWidth <= (strWidth+5)) {
					objName = objName.substring(1);
					strWidth = gc.stringExtent(objName).x;
				}

				int x = (unitWidth * (i + 1)) - 1;
				int y = verticalLineY;
				
				if (objectNameImageMap.get(objName) == null) {
					objectNameImageMap.put(objName, SingleSourceHelper.createVerticalTextImage(objName, gc.getFont(), dark_gary.getRGB(), false));
				}
				
				gc.drawImage(objectNameImageMap.get(objName), x,  y);
				
				if (datas[i].isAlive == false) {
					gc.setForeground(dark_gary);
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
						int noOfBars = (int) reach / BAR_WIDTH;
						int noOfAct1 = (int) (noOfBars * ((double)asd.act1 / total));
						int noOfAct2 = (int) (noOfBars * ((double)asd.act2  / total));
						int noOfAct3 = (int) (noOfBars * ((double)asd.act3 / total));
						int sediments = noOfBars - (noOfAct1 + noOfAct2 + noOfAct3);
						while (sediments > 0) {
							if (asd.act3 > 0) {
								noOfAct3++;
								sediments--;
							}
							if (sediments > 0 && asd.act2 > 0) {
								noOfAct2++;
								sediments--;
							}
							if (sediments > 0 && asd.act1 > 0) {
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
					gc.setForeground(black);
					String v = Long.toString(total);
					gc.drawString(v, AXIS_PADDING + (unitWidth * i) + ((unitWidth - gc.stringExtent(v).x) / 2), barY - (BAR_HEIGHT * 2) - 4 , true);
				}
			}
			
			// draw scale text
			gc.setForeground(black);
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
}
