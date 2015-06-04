/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import org.csstudio.swt.xygraph.figures.Annotation;

/**The command moving an annotation label.
 * @author Xihui Chen
 *
 */
public class MovingAnnotationLabelCommand implements IUndoableCommand {

	private Annotation annotation;

	private double beforeDx, beforeDy, afterDx, afterDy;
	
	
	public MovingAnnotationLabelCommand(Annotation annotation) {
		this.annotation = annotation;
	}

	public void redo() {
		annotation.setdxdy(afterDx, afterDy);
	}

	public void undo() {
		annotation.setdxdy(beforeDx, beforeDy);
	}
	
	public void setBeforeMovingDxDy(double dx, double dy){
		beforeDx = dx;
		beforeDy = dy;
	}
	
	public void setAfterMovingDxDy(double dx, double dy){
		afterDx = dx;
		afterDy = dy;
	}
	
	@Override
	public String toString() {
		return "Move Annotation Label";
	}
}
