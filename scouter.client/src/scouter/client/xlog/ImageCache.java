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
package scouter.client.xlog;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import scouter.client.model.AgentColorManager;
import scouter.client.util.ColorUtil;
import scouter.lang.pack.XLogTypes;

import java.util.HashMap;
import java.util.Map;

public class ImageCache {

	private static ImageCache instance;
	private Map<RGB, Image> xLogDotMap = new HashMap<RGB, Image>();
	private Image errorXpDot = null;
	private Image errorXpDotLight = null;

	public synchronized static ImageCache getInstance() {
		if (instance == null) {
			instance = new ImageCache();
		}
		return instance;
	}

	public synchronized Image getXPImage(int objHash, byte xtype) {
		Color agentColor = AgentColorManager.getInstance().getColor(objHash);
		if (agentColor == null) {
			agentColor = ColorUtil.getInstance().getColor("blue");
		}
		if(xtype == XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE || xtype == XLogTypes.BACK_THREAD2) {
			agentColor = ColorUtil.getInstance().getColor("light2 gray");
		}

		RGB rgb = agentColor.getRGB();
		Image xp = xLogDotMap.get(rgb);
		if (xp == null) {
			xp = createXPImage(rgb);
			xLogDotMap.put(rgb, xp);
		}
		return xp;
	}

	private Image createXPImage(RGB rgb) {
		return createXPImage5(rgb);
	}

	private Image createXPImage4(RGB rgb) {
		Image xp;
		xp = new Image(null, 4, 4);
		GC gcc = new GC(xp);
		gcc.setForeground(new Color(null, rgb));
		for (int i = 0; i < 4; i++) {
			gcc.drawLine(i, 0, i, 3);
		}
		gcc.setForeground(ColorUtil.getInstance().getColor("white"));
		gcc.drawPoint(1, 0);
		gcc.drawPoint(3, 1);
		gcc.drawPoint(0, 2);
		gcc.drawPoint(2, 3);
		gcc.dispose();
		return xp;
	}

	private Image createXPImage5(RGB rgb) {
		Image xp;
		xp = new Image(null, 5, 5);
		GC gcc = new GC(xp);
		gcc.setForeground(new Color(null, rgb));
		for (int i = 0; i < 5; i++) {
			gcc.drawLine(i, 0, i, 4);
		}
		gcc.setForeground(ColorUtil.getInstance().getColor("white"));
		gcc.drawPoint(1, 0);
		gcc.drawPoint(4, 1);
		gcc.drawPoint(0, 3);
		gcc.drawPoint(3, 4);
		gcc.dispose();
		return xp;
	}

	public synchronized Image getXPErrorImage(byte xtype) {
		if (errorXpDot == null) {
			errorXpDot = createXPImage(ColorUtil.getInstance().getColor("red").getRGB());
		}
		if(errorXpDotLight == null) {
			errorXpDotLight = createXPImage(ColorUtil.getInstance().getColor("light2 red").getRGB());
		}
		if(xtype == XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE || xtype == XLogTypes.BACK_THREAD2) {
			return errorXpDotLight;
		} else {
			return errorXpDot;
		}
	}

	private Image createObjectImage(RGB rgb) {
		Image xp = new Image(null, 3, 3);
		GC gcc = new GC(xp);
		gcc.setForeground(new Color(null, rgb));
		gcc.drawPoint(1, 1);
		gcc.dispose();
		return xp;
	}
}
