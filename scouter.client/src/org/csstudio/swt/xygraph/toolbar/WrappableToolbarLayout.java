/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.toolbar;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**Arrange children in multiple rows if necessary.
 * @author Xihui Chen
 *
 */
public class WrappableToolbarLayout extends AbstractLayout {

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int hint,
			int hint2) {
		int width = hint;
		int height = hint2;
		if(width > 0){
			int w =0;
			int h =0;
			int maxH=0;
			for(Object child : container.getChildren()){
				IFigure figure = (IFigure)child;
				Dimension preferSize = figure.getPreferredSize();
				if(w + preferSize.width < width){
					w +=preferSize.width;
					if(maxH < preferSize.height){
						maxH = preferSize.height;
					}
				}
				else {
					h+=maxH;
					maxH = preferSize.height;
					w = preferSize.width;
				}					
			}
			h += maxH;
			if(height < 0)
				height = h;			
			return new Dimension(width, height);
		}else{
//			int w =0;
			int maxH = 0;
			for(Object child : container.getChildren()){
				IFigure figure = (IFigure)child;
				Dimension preferSize = figure.getPreferredSize();
//				w += preferSize.width;
				if(maxH < preferSize.height){
						maxH = preferSize.height;
				}				
			}
			if(height < 0)
				height = maxH;
			return new Dimension(width, height);
		}		
	}

	public void layout(IFigure container) {
		Rectangle clientArea = container.getClientArea();
		int w = 0;
		int h = 0;
		int maxH =0;
		for(Object child : container.getChildren()){
			IFigure figure = (IFigure)child;
			Dimension preferSize = figure.getPreferredSize();
			if(w + preferSize.width < clientArea.width){
				figure.setBounds(new Rectangle(clientArea.x + w, clientArea.y + h, 
					preferSize.width, preferSize.height));
				w += preferSize.width;
				if(maxH < preferSize.height){
					maxH = preferSize.height;
				}
			}else{
				h+=maxH;
				w=0;
				figure.setBounds(new Rectangle(clientArea.x + w, clientArea.y + h,
						preferSize.width, preferSize.height));
				w = preferSize.width;
				maxH = preferSize.height;
			}			
		}
		
	}

}
