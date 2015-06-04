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


/**The operation manager will help to manage the undoable and redoable operations. 
 * @author Xihui Chen
 *
 */
public class OperationsManager {

	private SizeLimitedStack<IUndoableCommand> undoStack;
	
	private SizeLimitedStack<IUndoableCommand> redoStack;
	
	private List<IOperationsManagerListener> listeners;
	
	/**
	 * Constructor.
	 */
	public OperationsManager() {
		undoStack = new SizeLimitedStack<IUndoableCommand>(30);
	
		redoStack = new SizeLimitedStack<IUndoableCommand>(30);
		listeners = new ArrayList<IOperationsManagerListener>();
	}
	
	/**Execute a command and push it to undo stack.
	 * @param command the command to be executed.
	 */
	public void addCommand(IUndoableCommand command){
		undoStack.push(command);
		redoStack.clear();
		fireOperationsHistoryChanged();
	}
	
	/**Undo the command. Restore the state of the target to the previous state before this
	 * command has been executed.
	 * @param command 
	 */
	public void undoCommand(IUndoableCommand command){
		IUndoableCommand temp;
		do{	temp = undoStack.pop();
			temp.undo();
			redoStack.push(temp);			
		}while(temp!= command);
		fireOperationsHistoryChanged();
	}
	
	
	/**Re-do the command. Restore the state of the target to the state after this
	 * command has been executed.
	 * @param command
	 */
	public void redoCommand(IUndoableCommand command){
		IUndoableCommand temp;
		do{	temp = redoStack.pop();
			temp.redo();
			undoStack.push(temp);			
		}while(temp!= command);
		fireOperationsHistoryChanged();
	}
	
	/**
	 * undo the last command. Do nothing if there is no last command.
	 */
	public void undo(){
		if(undoStack.size() > 0)
			undoCommand(undoStack.peek());
	}
	
	/**
	 * redo the last undone command. Do nothing if there is no last undone command.
	 */
	public void redo(){
		if(redoStack.size() > 0)
			redoCommand(redoStack.peek());
	}
	
	
	/**
	 * @return the undo commands array. The first element is the 
	 * oldest commands and the last element is the latest commands.
	 */
	public Object[] getUndoCommands(){
		return undoStack.toArray();
	}
	
	/**
	 * @return the redo commands array. The first element is the 
	 * oldest commands and the last element is the latest commands.
	 */
	public Object[] getRedoCommands(){
		return redoStack.toArray();
	}
	
	public void addListener(IOperationsManagerListener listener){
		listeners.add(listener);
	}
	
	public boolean removeListener(IOperationsManagerListener listener){
		return listeners.remove(listener);
	}
	private void fireOperationsHistoryChanged(){
		for(IOperationsManagerListener listener : listeners)
			listener.operationsHistoryChanged(this);
	}
	
	public int getUndoCommandsSize(){
		return undoStack.size();
	}
	
	public int getRedoCommandsSize(){
		return redoStack.size();
	}
	
	
}
