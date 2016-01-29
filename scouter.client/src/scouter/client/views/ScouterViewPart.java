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
package scouter.client.views;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.lang.counters.CounterEngine;

public class ScouterViewPart extends ViewPart{
	
	public String titleName;
	public String desc = "";
	public String statusMessage = "";
	
	public void setViewTab(String objType, String counter, int serverId) throws Exception {
		setViewTab(objType, counter, serverId, true);
	}
	
	public void setViewTab(String objType, String counter, int serverId, boolean showDescription) throws Exception {
		
		String counterDisplay = "";
		String objectDisplay = "";
		Server server = ServerManager.getInstance().getServer(serverId);
		CounterEngine counterEngine = null;
		if(server != null){
			counterEngine = server.getCounterEngine();
			if(counterEngine != null){
				counterDisplay = counterEngine.getCounterDisplayName(objType, counter);
				objectDisplay = counterEngine.getDisplayNameObjectType(objType);
			}
		}
		
		this.titleName = counterDisplay + " - " + objectDisplay;
		setPartName(this.titleName);
		setTitleImage(Images.getCounterImage(objType, counter, serverId));
		if(showDescription){
			setContentDescription(desc);
		}
	}

	public void setDesc(){
		setContentDescription(desc);
	}

	public void setActive(){
		setTitleImage(Images.active);		
	}
	public void setInactive(){
		setTitleImage(Images.inactive);		
	}
	
	
	public void createPartControl(Composite parent) {
	}

	public void setFocus() {
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		slManager.setMessage(statusMessage);
	}
	
}
