package org.csstudio.swt.xygraph.util;

import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

public class SingleSourceHelperImpl extends SingleSourceHelper {

	@Override
	protected Cursor createInternalCursor(Display display, ImageData imageData,
			int width, int height, int style) {
		return new Cursor(display, imageData, width, height);
	}

	@Override
	protected Image createInternalVerticalTextImage(String text, Font font,
			RGB color, boolean upToDown) {
		final Dimension titleSize = FigureUtilities.getTextExtents(text, font);

		final int w = titleSize.height;
		final int h = titleSize.width + 1;
		Image image = new Image(Display.getCurrent(), w, h);

		final GC gc = new GC(image);
		final Color titleColor = new Color(Display.getCurrent(), color);
		RGB transparentRGB = new RGB(240, 240, 240);

		gc.setBackground(XYGraphMediaFactory.getInstance().getColor(
				transparentRGB));
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

	@Override
	protected Image getInternalXYGraphSnapShot(XYGraph xyGraph) {
		Rectangle bounds = xyGraph.getBounds();
		Image image = new Image(null, bounds.width + 6, bounds.height + 6);
		GC gc = new GC(image);
		SWTGraphics graphics = new SWTGraphics(gc);
		graphics.translate(-bounds.x + 3, -bounds.y + 3);
		graphics.setForegroundColor(xyGraph.getForegroundColor());
		graphics.setBackgroundColor(xyGraph.getBackgroundColor());
		xyGraph.paint(graphics);
		gc.dispose();
		return image;
	}

	@Override
	protected String getInternalImageSavePath() {
		FileDialog dialog = new FileDialog(Display.getDefault().getShells()[0],
				SWT.SAVE);
		dialog.setFilterNames(new String[] { "PNG Files", "All Files (*.*)" });
		dialog.setFilterExtensions(new String[] { "*.png", "*.*" }); // Windows
		String path = dialog.open();
		return path;
	}

}
