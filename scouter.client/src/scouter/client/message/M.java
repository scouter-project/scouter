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
package scouter.client.message;

import org.eclipse.osgi.util.NLS;

public class M extends NLS {

    public static String PREFERENCE_CHARTLINE_WIDTH;
	public static String PREFERENCE_CHARTSETTING;
	public static String PREFERENCE_CHARTXLOG_IGNORE_TIME;
	public static String PREFERENCE_CHARTXLOG_MAX_COUNT;
	public static String PREFERENCE_EXPAND;
    public static String PREFERENCE_EXPAND_TEXTCACHE;
    public static String PREFERENCE_EXPAND_CHART;
    
    public static String PREFERENCE_TEXT_CACHE_APICALL;
	public static String PREFERENCE_TEXT_CACHE_ERROR;
	public static String PREFERENCE_TEXT_CACHE_METHOD;
	public static String PREFERENCE_TEXT_CACHE_OBJECT;
	public static String PREFERENCE_TEXT_CACHE_REFERER;
	public static String PREFERENCE_TEXT_CACHE_SERVICE;
	public static String PREFERENCE_TEXT_CACHE_SERVICEGROUP;
	public static String PREFERENCE_TEXT_CACHE_SIZE_SETTING;
	public static String PREFERENCE_TEXT_CACHE_SQL;
	public static String PREFERENCE_TEXT_CACHE_SQLTABLE;
	public static String PREFERENCE_TEXT_CACHE_USERAGENT;

	static {
        NLS.initializeMessages(M.class.getName(), M.class);
    }
}
