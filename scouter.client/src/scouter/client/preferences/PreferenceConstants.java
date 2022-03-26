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

public class PreferenceConstants {

	public static final String P_SVR_DEFAULT            = "svrAddrDef";
	public static final String P_SVR_LIST       = "svrAddrList";
	public static final String P_SVR_AUTOLOGIN_LIST       = "svrAddrAutoLogin";
	public static final String P_SVR_ACCOUNT_PREFIX       = "autologin_";
	public static final String P_SVR_DIVIDER         = ",";
	
	public static final String P_SOCKS_SVR_ADDR_PREFIX       = "socksServerAddr_";
	public static final String P_SOCKS_SVR_AUTOLOGIN_LIST       = "socksSvrAddrAutoLogin";
	
	
	public static final String P_PERS_WAS_SERV_DEFAULT_HOST = "wasServiceDefaultHost";
	public static final String P_PERS_WAS_SERV_DEFAULT_WAS  = "wasServiceDefaultWAS";
	public static final String P_PERS_WAS_SERV_DEFAULT_DB  = "wasServiceDefaultDB";
	
	public static final String P_MASS_PROFILE_BLOCK  = "massProfileBlock";
	
	public static final String P_UPDATE_SERVER_ADDR  = "updateServerAddr";
	
	public static final String P_CHART_LINE_WIDTH  = "lineWidth";
	public static final String P_XLOG_IGNORE_TIME  = "xlog_ignore_time";
	public static final String P_XLOG_MAX_COUNT  = "xlog_max_count";
	public static final String P_XLOG_DRAG_MAX_COUNT = "xlog_drag_max_count";
	
	public static final String P_ALERT_DIALOG_TIMEOUT = "alert_dialog_timeout";
	
	public static final String NOTIFY_FATAL_ALERT = "notify_fatal_alert";
	public static final String NOTIFY_WARN_ALERT = "notify_warn_alert";
	public static final String NOTIFY_ERROR_ALERT = "notify_error_alert";
	public static final String NOTIFY_INFO_ALERT = "notify_info_alert";
	
}
