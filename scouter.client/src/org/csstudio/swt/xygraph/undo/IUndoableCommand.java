/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

/**
 * @author Xihui Chen
 *
 */
public interface IUndoableCommand {
	
	/**
	 * Restore the state of the target to the state before this
	 * command has been executed.
	 */
	public void undo();
	
	/**
	 * Restore the state of the target to the state after this
	 * command has been executed.
	 */
	public void redo();
	
	// toString() is used to obtain the text that's used
	// when displaying this command in the GUI
}
