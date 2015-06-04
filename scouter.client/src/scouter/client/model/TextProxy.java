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
package scouter.client.model;

import scouter.lang.TextTypes;

public class TextProxy {

	public TextProxy() {
	}

	
	final public static TextModel service = new TextModel(TextTypes.SERVICE, 8192);
	final public static TextModel sql = new TextModel(TextTypes.SQL, 8192);
	final public static TextModel method = new TextModel(TextTypes.METHOD, 4096);
	final public static TextModel error = new TextModel(TextTypes.ERROR, 1024);
	final public static TextModel apicall = new TextModel(TextTypes.APICALL, 4096);
	final public static TextModel object = new TextModel(TextTypes.OBJECT, 1024);
	final public static TextModel referer = new TextModel(TextTypes.REFERER, 1024);
	final public static TextModel userAgent = new TextModel(TextTypes.USER_AGENT, 1024);
	final public static TextModel group = new TextModel(TextTypes.GROUP, 1024);
	final public static TextModel sql_tables = new TextModel(TextTypes.SQL_TABLES, 1024);
	final public static TextModel city = new TextModel(TextTypes.CITY, 1024);

}
