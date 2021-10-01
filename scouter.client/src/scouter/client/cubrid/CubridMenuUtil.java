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
package scouter.client.cubrid;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.cubrid.actions.MultiViewDialogAction;

public class CubridMenuUtil {

	public static void createAddViewContextMenu(IWorkbenchWindow window, int serverId, FigureCanvas canvas) {
		MenuManager manager = new MenuManager();

		manager.add(new MultiViewDialogAction(window, serverId, CubridTypePeriod.REALTIME));
		manager.add(new MultiViewDialogAction(window, serverId, CubridTypePeriod.PAST_LESS_1DAY));
		manager.add(new MultiViewDialogAction(window, serverId, CubridTypePeriod.PAST_MORE_1DAY));

		Menu menu = manager.createContextMenu(canvas);
		canvas.setMenu(menu);
	}

}
