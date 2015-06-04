/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.figures;

import org.csstudio.swt.xygraph.toolbar.XYGraphToolbar;
import org.eclipse.draw2d.Clickable;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**An graph which consists of a toolbar and an XYGraph. 
 * @author Xihui Chen
 * @author Kay Kasemir added flags
 */
public class ToolbarArmedXYGraph extends Figure {
	final private XYGraph xyGraph;

	final private XYGraphToolbar toolbar;

	private boolean transparent;
	private final static int MARGIN = 3;
	
	/** Construct default graph */
	public ToolbarArmedXYGraph() {
		this(new XYGraph());
	}

	/** Construct default toolbar around existing graph
	 *  @param xyGraph XYGraph
	 */
	public ToolbarArmedXYGraph(final XYGraph xyGraph) {
	    this(xyGraph, XYGraphFlags.COMBINED_ZOOM);
    }

	/** Construct toolbar around existing graph
	 *  @param xyGraph XYGraph
	 *  @param flags Bitwise 'or' of flags
     *  @see XYGraphFlags#COMBINED_ZOOM
	 *  @see XYGraphFlags#SEPARATE_ZOOM
	 */
	public ToolbarArmedXYGraph(final XYGraph xyGraph, final int flags) {
		this.xyGraph = xyGraph;
		toolbar = new XYGraphToolbar(this.xyGraph, flags);
		xyGraph.setOpaque(false);
		toolbar.setOpaque(false);
		add(toolbar);		
		add(xyGraph);		
	}
	
	@Override
	protected void layout() {
		Rectangle clientArea = getClientArea().getCopy();
		if(toolbar.isVisible()){
			toolbar.invalidate();
			Dimension size = toolbar.getPreferredSize(clientArea.width - MARGIN, -1);
			toolbar.setBounds(new Rectangle(clientArea.x + MARGIN, clientArea.y + MARGIN, 
					size.width, size.height));			
			clientArea.y += size.height + 2*MARGIN;
			clientArea.height -= size.height + 2*MARGIN;
		}
		xyGraph.setBounds(new Rectangle(clientArea));
			
		super.layout();
	}

	/**
	 * @param showToolbar the showToolbar to set
	 */
	public void setShowToolbar(boolean showToolbar) {
			toolbar.setVisible(showToolbar);
			revalidate();
	}

	/**
	 * @return the showToolbar
	 */
	public boolean isShowToolbar() {
		return toolbar.isVisible();
	}
	
	/**
	 * @return the xyGraph
	 */
	public XYGraph getXYGraph() {
		return xyGraph;
	}
	

	@Override
	public boolean isOpaque() {
		return false;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
    public void paintFigure(final Graphics graphics) {		
		if (!transparent)		
			graphics.fillRectangle(getClientArea());		
		super.paintFigure(graphics);
	}
	/**
	 * @return the transparent
	 */
	public boolean isTransparent() {
		return transparent;
	}
	/**
	 * @param transparent the transparent to set
	 */
	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
		xyGraph.setTransparent(transparent);
	}
	
	/** Add a button to the tool bar.
	 *  New button will be added to the 'end' of the tool bar.
	 *  @param button New button
	 */
    public void addToolbarButton(final Clickable button)
    {
        toolbar.addButton(button);
    }
}
