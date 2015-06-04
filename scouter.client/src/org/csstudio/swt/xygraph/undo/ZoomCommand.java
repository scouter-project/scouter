/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.linearscale.Range;

/**The command for graph zooming and panning.
 * @author Xihui Chen
 * @author Kay Kasemir (allow <code>null</code> for x axis)
 */
public class ZoomCommand extends SaveStateCommand
{
	final private List<Axis> xAxisList;
	final private List<Axis> yAxisList;
	
	final private List<Range> beforeXRangeList = new ArrayList<Range>();
	final private List<Range> beforeYRangeList = new ArrayList<Range>();
	final private List<Range> afterXRangeList = new ArrayList<Range>();
	final private List<Range> afterYRangeList = new ArrayList<Range>();

	/** Initialize
	 *  @param name Name of operation for undo/redo GUI
	 *  @param xAxisList X Axes to save or <code>null</code>
	 *  @param yAxisList Y Axes to save
	 */
	public ZoomCommand(final String name, final List<Axis> xAxisList,
	        final List<Axis> yAxisList)
	{
	    super(name);
		this.xAxisList = xAxisList;
		this.yAxisList = yAxisList;
		saveOriginalState();
	}

	private void saveOriginalState(){
        if (xAxisList != null)
            for(Axis axis : xAxisList)
                beforeXRangeList.add(axis.getRange());
        for(Axis axis : yAxisList)
            beforeYRangeList.add(axis.getRange());
    }
    
	public void redo() {
		int i=0;
		if (xAxisList != null) {
    		for(Axis axis : xAxisList){
    			axis.setRange(afterXRangeList.get(i));
    			i++;
    		}
		}
		i=0;
		for(Axis axis : yAxisList){
			axis.setRange(afterYRangeList.get(i));
			i++;
		}
	}

	public void undo() {
		int i=0;
        if (xAxisList != null) {
    		for(Axis axis : xAxisList){
    			axis.setRange(beforeXRangeList.get(i));
    			i++;
    		}
        }
		i=0;
		for(Axis axis : yAxisList){
			axis.setRange(beforeYRangeList.get(i));
			i++;
		}
	}
	
    @Override
	public void saveState(){
        if (xAxisList != null)
    		for(Axis axis : xAxisList)
    			afterXRangeList.add(axis.getRange());
		for(Axis axis : yAxisList)
			afterYRangeList.add(axis.getRange());
	}
}
