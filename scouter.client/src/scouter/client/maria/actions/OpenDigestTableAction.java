/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
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
package scouter.client.maria.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.maria.views.DigestTableView;
import scouter.client.util.ConsoleProxy;

public class OpenDigestTableAction implements PropertyChangeListener{
	
	final private int serverId;
	
	public OpenDigestTableAction(int serverId) {
		this.serverId = serverId;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		Object o = evt.getNewValue();
		if (o != null && o instanceof Range) {
			double stime = ((Range) o).getLower();
			double etime = ((Range) o).getUpper();
			try {
				DigestTableView view = (DigestTableView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DigestTableView.ID, ""+serverId, IWorkbenchPage.VIEW_ACTIVATE);
				if (view != null) {
					view.setInput((long)stime, (long)etime);
				}
			} catch (PartInitException e) {
				ConsoleProxy.errorSafe(e.toString());
			}
		}
	}
}
