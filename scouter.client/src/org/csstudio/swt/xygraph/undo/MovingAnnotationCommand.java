/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.figures.Annotation;
import org.eclipse.draw2d.geometry.Point;

/**The command of moving an annotation on the graph.
 * @author Xihui Chen
 *
 */
public class MovingAnnotationCommand implements IUndoableCommand {
	
	private Annotation annotation;
	private Point beforeMovePosition;
	private Point afterMovePosition;
	private ISample beforeMoveSnappedSample;
	private ISample afterMoveSnappedSample;
	private double beforeDx, beforeDy, afterDx, afterDy;
	
	
	public MovingAnnotationCommand(Annotation annotation) {
		this.annotation = annotation;
	}
	
	public void redo() {
		if(annotation.isFree())
			annotation.setCurrentPosition(afterMovePosition, false);
		else
			annotation.setCurrentSnappedSample(afterMoveSnappedSample, false);
		annotation.setdxdy(afterDx, afterDy);
	}

	public void undo() {
		if(annotation.isFree())
			annotation.setCurrentPosition(beforeMovePosition, false);
		else
			annotation.setCurrentSnappedSample(beforeMoveSnappedSample, false);
		annotation.setdxdy(beforeDx, beforeDy);
	}
	
	public void setBeforeDxDy(double dx, double dy){
		beforeDx = dx;
		beforeDy = dy;
	}
	
	public void setAfterDxDy(double dx, double dy){
		afterDx = dx;
		afterDy = dy;
	}
	
	/**
	 * @param beforeMovePosition the beforeMovePosition to set
	 */
	public void setBeforeMovePosition(Point beforeMovePosition) {
		this.beforeMovePosition = beforeMovePosition;
	}

	/**
	 * @param afterMovePosition the afterMovePosition to set
	 */
	public void setAfterMovePosition(Point afterMovePosition) {
		this.afterMovePosition = afterMovePosition;
	}

	/**
	 * @param beforeMoveSnappedSample the beforeMoveSnappedSample to set
	 */
	public void setBeforeMoveSnappedSample(ISample beforeMoveSnappedSample) {
		this.beforeMoveSnappedSample = beforeMoveSnappedSample;
	}

	/**
	 * @param afterMoveSnappedSample the afterMoveSnappedSample to set
	 */
	public void setAfterMoveSnappedSample(ISample afterMoveSnappedSample) {
		this.afterMoveSnappedSample = afterMoveSnappedSample;
	}
	
	@Override
	public String toString() {
		return "Move Annotation";
	}


}
