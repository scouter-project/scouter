/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.toolbar;

import org.csstudio.swt.xygraph.util.SWTConstants;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

/**A button with gray image when disabled.
 * @author Xihui Chen
 *
 */
public class GrayableButton extends Button {
	
	Image image;
	Image grayImage;
	private static final String GRAY_IMAGE = "GrayableButton.GrayImage";
	
	public GrayableButton(Image image) {
		this(image, null);
	}
	
	public GrayableButton(Image image, Image grayImage){
		super(image);
		this.image = image;
		this.grayImage = XYGraphMediaFactory.getInstance().getRegisteredImage(GRAY_IMAGE + image.toString());
		if( this.grayImage == null){
			if(SWT.getPlatform().startsWith("rap")) { //$NON-NLS-1$
				if( grayImage != null)
					this.grayImage = grayImage;
				else
					this.grayImage = image;
			}
			else
				this.grayImage = new Image(null, image, SWTConstants.IMAGE_GRAY);
			XYGraphMediaFactory.getInstance().registerImage(GRAY_IMAGE + image.toString(), this.grayImage);
		}		
	}
	
	@Override
	public void setEnabled(boolean value) {
		super.setEnabled(value);
		if(value)			
			setContents(new ImageFigure(image));
		else
			setContents(new ImageFigure(grayImage));
	}	
	
	
}
