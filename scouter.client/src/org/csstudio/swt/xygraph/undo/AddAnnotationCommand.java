/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import org.csstudio.swt.xygraph.Messages;
import org.csstudio.swt.xygraph.figures.Annotation;
import org.csstudio.swt.xygraph.figures.XYGraph;

/**The undoable command to add an annotation.
 * @author Xihui Chen
 *
 */
public class AddAnnotationCommand implements IUndoableCommand {
	
	private XYGraph xyGraph;
	private Annotation annotation;
	
	public AddAnnotationCommand(XYGraph xyGraph, Annotation annotation) {
		this.xyGraph = xyGraph;
		this.annotation = annotation;
	}

	public void redo() {
		xyGraph.addAnnotation(annotation);
	}

	public void undo() {
		xyGraph.removeAnnotation(annotation);
	}
	
	@Override
	public String toString() {
		return Messages.Annotation_Add;
	}

}
