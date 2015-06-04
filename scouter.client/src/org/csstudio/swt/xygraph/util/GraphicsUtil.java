/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.util;

import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

/**
 * Utility function for graphics operations.
 * 
 * @author Xihui Chen
 * 
 */
public final class GraphicsUtil {
	
	private static boolean isRAP= SWT.getPlatform().startsWith("rap"); //$NON-NLS-1$;

	/**
	 * Draw vertical text.
	 * 
	 * @param graphics
	 *            draw2D graphics.
	 * @param text
	 *            text to be drawn.
	 * @param x
	 *            the x coordinate of the text, which is the left upper corner.
	 * @param y
	 *            the y coordinate of the text, which is the left upper corner.
	 */
	public static final void drawVerticalText(Graphics graphics, String text,
			int x, int y, boolean upToDown) {
		try {
			if(SWT.getPlatform().startsWith("rap")) //$NON-NLS-1$
				throw new Exception();
			try {
				graphics.pushState();
				graphics.translate(x, y);
				if (upToDown) {
					graphics.rotate(90);
					graphics.drawText(
							text,
							0,
							-FigureUtilities.getTextExtents(text,
									graphics.getFont()).height);
				} else {
					graphics.rotate(270);
					graphics.drawText(
							text,
							-FigureUtilities.getTextWidth(text, graphics.getFont()),
							0);
				}
			}finally{
				graphics.popState();
			}
		} catch (Exception e) {// If rotate is not supported by the graphics.
//			final Dimension titleSize = FigureUtilities.getTextExtents(text,
//					graphics.getFont());

//			final int w = titleSize.height;
//			final int h = titleSize.width + 1;
			Image image = null;
			try {
				image = SingleSourceHelper.createVerticalTextImage(text,
						graphics.getFont(), graphics.getForegroundColor().getRGB(), upToDown);
				graphics.drawImage(image, x, y);

			} finally {
				if (image != null)
					image.dispose();
			}
		}
	}

	/**
	 * Draw vertical text.
	 * 
	 * @param graphics
	 *            draw2D graphics.
	 * @param text
	 *            text to be drawn.
	 * @param location
	 *            the left upper corner coordinates of the text.
	 */
	public static final void drawVerticalText(Graphics graphics, String text,
			Point location, boolean upToDown) {
		drawVerticalText(graphics, text, location.x, location.y, upToDown);
	}
	
	public static final boolean isRAP(){
		return isRAP;
	}
}
