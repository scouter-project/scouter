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
package scouter.client.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import scouter.util.ThreadUtil;

public class SpinnerThread extends Thread {
	
	static ImageLoader imageLoader = new ImageLoader();
	static final ImageData[] imageDatas = imageLoader.load(SpinnerThread.class.getResourceAsStream("/icons/spinner64.gif"));
	int frameIndex = 0;
	
	private Composite parent;
	private Canvas canvas;
	private GC gc;
	
	public SpinnerThread(Composite parent, Object layoutData) {
		this.parent = parent;
		this.setName(ThreadUtil.getName(this));
		final Image image = new Image(parent.getDisplay(), imageDatas[0].width,imageDatas[0].height);
		canvas = new Canvas(parent, SWT.NULL);
		if (layoutData != null) {
			canvas.setLayoutData(layoutData);
		}
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(image, 0, 0);
			}
		});
		gc = new GC(image);
	}

	public void run() {
		while (!parent.isDisposed() && !isInterrupted()) {
			frameIndex %= imageDatas.length;
			final ImageData imageData = imageDatas[frameIndex];
			ExUtil.exec(parent, new Runnable() {
				public void run() {
					Image frame = new Image(parent.getDisplay(), imageData);
					gc.drawImage(frame, imageData.x, imageData.y);
					frame.dispose();
					canvas.redraw();
				}
			});
			try {
				Thread.sleep(imageDatas[frameIndex].delayTime * 10);
			} catch (InterruptedException e) {
				return;
			}
			frameIndex += 1;
		}
	}
}
