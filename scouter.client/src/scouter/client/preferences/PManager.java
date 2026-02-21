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
package scouter.client.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import scouter.client.Activator;
import scouter.client.xlog.views.XLogColumnEnum;
import scouter.lang.counters.CounterConstants;

public class PManager {
	
	private static volatile PManager instance;
	private IPreferenceStore store = Activator.getDefault().getPreferenceStore();
	
	public static PManager getInstance() {
		if (instance == null) {
			synchronized (PManager.class) {
				if (instance == null) {
					instance = new PManager();
				}
			}
		}
		return instance;
	}
	
	private PManager() {
		store.setDefault(PreferenceConstants.P_CHART_LINE_WIDTH, 1);
		store.setDefault(PreferenceConstants.P_XLOG_IGNORE_TIME, 0);
		store.setDefault(PreferenceConstants.P_XLOG_MAX_COUNT, 1000000);
		store.setDefault(PreferenceConstants.P_XLOG_DRAG_MAX_COUNT, 200);

		store.setDefault(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_HOST, CounterConstants.LINUX);
		store.setDefault(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_WAS, CounterConstants.TOMCAT);
		store.setDefault(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_DB, CounterConstants.MARIA_DB);
		
		store.setDefault(PreferenceConstants.P_MASS_PROFILE_BLOCK, 10);

		for (XLogColumnEnum xLogColumnEnum : XLogColumnEnum.values()) {
			store.setDefault(xLogColumnEnum.getInternalID(), xLogColumnEnum.isDefaultVisible());
		}

		store.setDefault(PreferenceConstants.P_DARK_MODE, false);

//		store.setDefault(PreferenceConstants.P_UPDATE_SERVER_ADDR, PORT_AND_REPOSITORY_FOLDER);
//		store.setDefault(PreferenceConstants.P_ALERT_DIALOG_TIMEOUT, -1);
//		store.setDefault(PreferenceConstants.NOTIFY_FATAL_ALERT, true);
//		store.setDefault(PreferenceConstants.NOTIFY_WARN_ALERT, false);
//		store.setDefault(PreferenceConstants.NOTIFY_ERROR_ALERT, false);
//		store.setDefault(PreferenceConstants.NOTIFY_INFO_ALERT, true);
	}
	

	public boolean getBoolean(String key){
		return store.getBoolean(key);
	}
	public double getDouble(String key){
		return store.getDouble(key);
	}
	public float getFloat(String key){
		return store.getFloat(key);
	}
	public int getInt(String key){
		return store.getInt(key);
	}
	public long getLong(String key){
		return store.getLong(key);
	}
	public String getString(String key){
		return store.getString(key);
	}
	
	public void setDefault(String key, String value){
		store.setDefault(key, value);
	}
	
	public void setDefault(String key, boolean value){
		store.setDefault(key, value);
	}
	
	public void setValue(String key, boolean value){
		store.setValue(key, value);
	}
	public void setValue(String key, double value){
		store.setValue(key, value);
	}
	public void setValue(String key, float value){
		store.setValue(key, value);
	}
	public void setValue(String key, int value){
		store.setValue(key, value);
	}
	public void setValue(String key, long value){
		store.setValue(key, value);
	}
	public void setValue(String key, String value){
		store.setValue(key, value);
	}
	
}
