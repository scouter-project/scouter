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
 */

package scouter.lang;

public class SummaryEnum {
	final public static byte APP = 1;
	final public static byte SQL = 2;
	final public static byte ALERT = 3;
	final public static byte IP = 4;
	final public static byte APICALL = 5;
	public static final byte USER_AGENT = 8;
	public static final byte SERVICE_ERROR = 9;
	
	public static final byte ENDUSER_NAVIGATION_TIME = 10;
	public static final byte ENDUSER_AJAX_TIME = 11;
	public static final byte ENDUSER_SCRIPT_ERROR = 12;
	
}