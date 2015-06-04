/*
 *  Copyright 2015 LG CNS.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.actions;


import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.model.ICounterObjectSelector;
import scouter.client.popup.XYGraphObjectSelectionDialog;


public class ShowCounterObjectSelectionDialogAction extends Action {
	public final static String ID = ShowCounterObjectSelectionDialogAction.class.getName();

	private final IWorkbenchWindow window;
	private ICounterObjectSelector objSelector;
	private ArrayList<Integer> objHashAll;
	private ArrayList<Integer> objHashSelected;
	private int serverId;
	
	public ShowCounterObjectSelectionDialogAction(IWorkbenchWindow window, ICounterObjectSelector objSelector, ArrayList<Integer> objHashAll, ArrayList<Integer> objHashSelected, int serverId) {
		this.window = window;
		this.objSelector = objSelector;
		this.objHashAll = objHashAll;
		this.objHashSelected = objHashSelected;
		this.serverId = serverId;
		
		setText("Select Objects");
		setImageDescriptor(Images.SELECTOR);
		setId(ID);
	}

	public void run() {
		if (window != null) {
			XYGraphObjectSelectionDialog dialog = new XYGraphObjectSelectionDialog(window.getShell().getDisplay(), objSelector, serverId);
			dialog.show(objHashAll, objHashSelected);
		}
	}
}
