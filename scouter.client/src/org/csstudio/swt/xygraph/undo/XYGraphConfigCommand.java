/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import org.csstudio.swt.xygraph.figures.XYGraph;

/**The command for graph configuration.
 * @author Xihui Chen
 *
 */
public class XYGraphConfigCommand implements IUndoableCommand {

	private XYGraph xyGraph;
	private XYGraphMemento previousXYGraphMem, afterXYGraphMem;
	
	public XYGraphConfigCommand(XYGraph xyGraph) {
		this.xyGraph = xyGraph;
		previousXYGraphMem = new XYGraphMemento();		
		afterXYGraphMem = new XYGraphMemento();
		
		for(int i =0; i< xyGraph.getPlotArea().getAnnotationList().size(); i++){
			previousXYGraphMem.addAnnotationMemento(new AnnotationMemento());
			afterXYGraphMem.addAnnotationMemento(new AnnotationMemento());
		}	
		
		for(int i=0; i<xyGraph.getAxisList().size(); i++){
			previousXYGraphMem.addAxisMemento(new AxisMemento());
			afterXYGraphMem.addAxisMemento(new AxisMemento());
		}
		
		for(int i=0; i<xyGraph.getPlotArea().getTraceList().size(); i++){
			previousXYGraphMem.addTraceMemento(new TraceMemento());
			afterXYGraphMem.addTraceMemento(new TraceMemento());
		}
		
	}

	public void redo() {
		XYGraphMementoUtil.restoreXYGraphPropsFromMemento(xyGraph, afterXYGraphMem);
	}

	public void undo() {
		XYGraphMementoUtil.restoreXYGraphPropsFromMemento(xyGraph, previousXYGraphMem);
	}
	
	public void savePreviousStates(){
		XYGraphMementoUtil.saveXYGraphPropsToMemento(xyGraph, previousXYGraphMem);
	}
	
	public void saveAfterStates(){
		XYGraphMementoUtil.saveXYGraphPropsToMemento(xyGraph, afterXYGraphMem);
	}

	@Override
	public String toString() {
		return "Configure XYGraph Settings";
	}
	
}
