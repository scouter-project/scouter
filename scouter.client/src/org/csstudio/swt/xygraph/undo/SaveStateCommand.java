/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

/** Abstract base for an undo-able command that saves some state
 *  @author Kay Kasemir
 */
abstract public class SaveStateCommand implements IUndoableCommand
{
    /** Name of the command, shown in undo/redo GUI */
    final private String name;
    
    /** Initialize
     *  Derived class should save the 'original' state.
     *   */
    public SaveStateCommand(final String name)
    {
        this.name = name;
    }

    /** Derived class should implement this to save the 'final' state.
     *  Might be called multiple times to save intermediate states,
     *  and the state saved on the last call would be the 'redo' state.
     */
    abstract public void saveState();
        
    /** @return Name of the command, shown in undo/redo GUI */
    @Override
    public String toString()
    {
        return name;
    }
}
