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
package scouter.client.stack.actions;

import scouter.client.stack.base.MainFrame;

public class MainFrameAction extends Thread{
	private String m_menuName = null;
	
	public MainFrameAction(String menuName){
		m_menuName = menuName;
	}
	
	public void run(){
		try {
			MainFrame.instance(true).processMenu(m_menuName, null);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
