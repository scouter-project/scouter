/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.linearscale;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.csstudio.swt.xygraph.linearscale.AbstractScale.LabelSide;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;


/**
 * A linear scale related marker, whose orientation, range, mark position etc. are 
 * determined by the scale. It must have the same length and bounds.x(for horizontal) 
 * or bounds.y(for vertical) with the scale.
 * 
 * @author Xihui Chen
 */
public class LinearScaledMarker extends Figure {
	
	private Map<String, MarkerProperties> markersMap= new LinkedHashMap<String, MarkerProperties>();
	
	private static final RGB DEFAULT_MARKER_COLOR = XYGraphMediaFactory.COLOR_RED;
	
	private String[] labels;
	
	private double[] markerValues;	
	private Dimension[] markerLabelDimensions;	
	private List<Color> markerColorsList = new ArrayList<Color>();
	
	private LinearScale scale;
	
	private LabelSide makerLablesPosition = LabelSide.Secondary;
	
	private boolean markerLineVisible = false;
	
	private boolean markerLableVisible = true;

	private int tickLabelMaxLength;
	
	private boolean dirty = true;

	private int[] markerPositions;
	
	private final static int TICK_LENGTH = 10;
	private final static int TICK_LINE_WIDTH = 2;
	private final static int GAP_BTW_MARK_LABEL = 3;
	
	
	public LinearScaledMarker(LinearScale scale) {
		this.scale = scale;
		setFont(XYGraphMediaFactory.getInstance().getFont(XYGraphMediaFactory.FONT_TAHOMA));
	}
	
	/**
	 * @param dirty the dirty to set
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	/**
	 * If the marker exists, set its value.
	 * @param label the label of the marker element, it must be unique. 
	 * @param value the value to be set
	 */
	public void setMarkerElementValue(String label, double value) {
		if(markersMap.containsKey(label)) {
			markersMap.get(label).value = value;
			dirty =true;
		}			
	}
	
	
	/**
	 * If the marker exists, set its color.
	 * @param label the label of the marker element, it must be unique. 
	 * @param color the color to be set
	 */
	public void setMarkerElementColor(String label, RGB color) {
		if(markersMap.containsKey(label)) {
			markersMap.get(label).color = color;
			dirty =true;
		}			
	}
	
	
	/**
	 * Add (if the marker does not exist) or change a marker element.
	 * @param label the label of the marker element, it must be unique. 
	 * @param value the value of the marker element
	 * @param color the color of the marker element
	 */
	public void addMarkerElement(String label, double value, RGB color) {
		if(markersMap.containsKey(label)) {
			markersMap.get(label).value = value;
			markersMap.get(label).color = color;
		}else
			markersMap.put(label, new MarkerProperties(value, color));		
		dirty =true;
	}	
	
	/**
	 * Add (if the marker does not exist) or change a marker element.
	 * @param label the label of the marker element, it must be unique. 
	 * @param value the value of the marker element
	 */
	public void addMarkerElement(String label, double value) {
		if(markersMap.containsKey(label))
			markersMap.get(label).value = value;
		else
			markersMap.put(label, new MarkerProperties(value, DEFAULT_MARKER_COLOR));		
		dirty =true;
	}
	
	public void removeMarkerElement(String label) {
		markersMap.remove(label);
		dirty =true;
	}	
	
	@Override
	protected void paintClientArea(Graphics graphics) {	
		//use relative coordinate
		graphics.translate(bounds.x, bounds.y);
		updateTick();
		drawMarkerTick(graphics);		
		super.paintClientArea(graphics);
	}
	
	private void drawMarkerTick(Graphics graphics) {
		graphics.setLineWidth(TICK_LINE_WIDTH);
		if(scale.isHorizontal()) {
			if(makerLablesPosition == LabelSide.Primary) {
				int i = 0;
				for(int markerPos : markerPositions) {
					graphics.setForegroundColor(markerColorsList.get(i));
					graphics.drawLine(markerPos, 0, markerPos, TICK_LENGTH);
					//draw labels
					if(isMarkerLableVisible()) {
						graphics.drawText(labels[i], 
								markerPos-markerLabelDimensions[i].width/2, 
								TICK_LENGTH + GAP_BTW_MARK_LABEL);
					}					
					i++;
				}
			} else {
				int i = 0;
				for(int markerPos : markerPositions) {
					graphics.setForegroundColor(markerColorsList.get(i));
					graphics.drawLine(markerPos, bounds.height, 
							markerPos, bounds.height - TICK_LENGTH);
					//draw labels
					if(isMarkerLableVisible()) {
						graphics.drawText(labels[i], 
								markerPos-markerLabelDimensions[i].width/2, 
								bounds.height - TICK_LENGTH - GAP_BTW_MARK_LABEL
								- markerLabelDimensions[i].height);
					}
					i++;
				}
			}		
		} else {
			if(makerLablesPosition == LabelSide.Primary) {
				
				for(int i = 0; i < markerPositions.length; i++) {
					graphics.setForegroundColor(markerColorsList.get(i));
					graphics.drawLine(bounds.width, markerPositions[i], 
							bounds.width - TICK_LENGTH, markerPositions[i]);
					//draw labels
					if(isMarkerLableVisible()) {
						graphics.drawText(labels[i], 
								bounds.width - TICK_LENGTH - GAP_BTW_MARK_LABEL
								- markerLabelDimensions[i].width,
								markerPositions[i] - markerLabelDimensions[i].height/2);
					}					
				}
			} else {
				int i = 0;
				for(int markerPos : markerPositions) {
					graphics.setForegroundColor(markerColorsList.get(i));
					graphics.drawLine(0, markerPos, 
							TICK_LENGTH, markerPos);
					
					//draw labels
					if(isMarkerLableVisible()) {
						graphics.drawText(labels[i], 
								TICK_LENGTH + GAP_BTW_MARK_LABEL,
								markerPos-markerLabelDimensions[i].height/2
								);
					}					
					i++;
				}
			}
		}			
	}	
	
	@Override
	public void setBounds(Rectangle rect) {
		if(!bounds.equals(rect))
			dirty = true;
		super.setBounds(rect);
		
	}
	
	/**
     * Updates the tick, recalculate all inner parameters
     */
	public void updateTick() {
		if(dirty == true) {
			updateMarkerElments();
			updateTickLabelMaxLength();
		}
		dirty = false;		
	}
	
    /**
     * Gets max length of tick label.
     */
    private void updateTickLabelMaxLength() {
        int maxLength = 0;
        
        for (int i = 0; i < labels.length; i++) {
                Dimension p = FigureUtilities.getTextExtents(labels[i], scale.getFont());
                if (p.width > maxLength) {
                    maxLength = p.width;
                }
            }
        
        tickLabelMaxLength = maxLength;
    }


	/**
	 * @return the labels
	 */
	public String[] getLabels() {
		String[] labels = new String[markersMap.size()];
		int i=0;
		for(String label : markersMap.keySet()) {
			labels[i] = label;
			i++;
		}
		return labels;
	}


	/**
	 * @return the markerValues
	 */
	public void updateMarkerElments() {
		labels = new String[markersMap.size()];
		markerColorsList.clear();
		markerValues = new double[markersMap.size()];
		markerLabelDimensions = new Dimension[markersMap.size()];
		markerPositions = new int[markerValues.length];
		int i = 0;
		for(String label : markersMap.keySet()) {
			labels[i] = label;
			markerValues[i] = markersMap.get(label).value;
			markerPositions[i] = scale.getValuePosition(markerValues[i], true);
			markerLabelDimensions[i] = FigureUtilities.getTextExtents(label, getFont());
			markerColorsList.add(
					XYGraphMediaFactory.getInstance().getColor(markersMap.get(label).color));
			i++;
		}
	}

	/**
	 * @param scale the scale to set
	 */
	public void setScale(LinearScale scale) {
		this.scale = scale;
		dirty =true;
	}

	/**
	 * @return the scale
	 */
	public LinearScale getScale() {
		return scale;
	}

	/**
	 * @param labelSide the makerLablesPosition to set
	 */
	public void setLabelSide(LabelSide labelSide) {
		this.makerLablesPosition = labelSide;
		dirty =true;
	}

	/**
	 * @return the makerLablesPosition
	 */
	public LabelSide getMakerLablesPosition() {
		return makerLablesPosition;
	}

	/**
	 * @param markerLineVisible the markerLineVisible to set
	 */
	public void setMarkerLineVisible(boolean markerLineVisible) {
		this.markerLineVisible = markerLineVisible;
		dirty = true;
	}

	/**
	 * @return the markerLineVisible
	 */
	public boolean isMarkerLineVisible() {
		return markerLineVisible;
	}

	/**
	 * @param markerLableVisible the markerLableVisible to set
	 */
	public void setMarkerLableVisible(boolean markerLableVisible) {
		this.markerLableVisible = markerLableVisible;
		dirty =true;
	}

	/**
	 * @return the markerLableVisible
	 */
	public boolean isMarkerLableVisible() {
		return markerLableVisible;
	}
	
	@Override
	public Dimension getPreferredSize(int wHint, int hHint) {
		updateTick();
		Dimension size = new Dimension(wHint, hHint);
		
		if(scale.isHorizontal()) {
			size.width = scale.getSize().width;
			size.height = FigureUtilities.getTextExtents("dummy", getFont()).height
							+ GAP_BTW_MARK_LABEL + TICK_LENGTH;
		} else {
			updateTickLabelMaxLength();
			size.width = (int)tickLabelMaxLength + GAP_BTW_MARK_LABEL + TICK_LENGTH;	
			size.height = scale.getSize().height;
		}			
		return size;
		
	}
	
	private static class MarkerProperties {		

		private double value;

		private RGB color;
		
		/**
		 * @param value
		 * @param color
		 */
		public MarkerProperties(double value, RGB color) {
			this.value = value;
			this.color = color;
		}		
	}
	

}
