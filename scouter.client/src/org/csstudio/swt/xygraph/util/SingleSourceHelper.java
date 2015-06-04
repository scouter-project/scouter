package org.csstudio.swt.xygraph.util;

import org.csstudio.swt.xygraph.figures.XYGraph;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public abstract class SingleSourceHelper {

	private static final SingleSourceHelper IMPL;
	
	static {
		IMPL = (SingleSourceHelper)ImplementationLoader.newInstance(
				SingleSourceHelper.class);
	}
	
	public static Cursor createCursor(
			Display display, ImageData imageData, int width, int height, int backUpSWTCursorStyle){
		return IMPL.createInternalCursor(display, imageData, width, height, backUpSWTCursorStyle);
	}
	
	public static Image createVerticalTextImage(String text, Font font, RGB color, boolean upToDown){
		return IMPL.createInternalVerticalTextImage(text, font, color, upToDown);
	}
	
	public static Image getXYGraphSnapShot(XYGraph xyGraph){
		return IMPL.getInternalXYGraphSnapShot(xyGraph);
	}
	
	public static String getImageSavePath(){
		return IMPL.getInternalImageSavePath();
	}
	

	protected abstract String getInternalImageSavePath();

	protected abstract Cursor createInternalCursor(
			Display display, ImageData imageData, int width, int height,int backUpSWTCursorStyle);
	
	protected abstract Image createInternalVerticalTextImage(
			String text, Font font, RGB color, boolean upToDown);
	
	protected abstract Image getInternalXYGraphSnapShot(XYGraph xyGraph);
	
	
}
