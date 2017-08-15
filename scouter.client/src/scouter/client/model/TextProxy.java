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
package scouter.client.model;

import scouter.lang.TextTypes;

import java.util.HashMap;
import java.util.Map;

public class TextProxy {

	public TextProxy() {
	}

	
	final public static TextModel service = new TextModel(TextTypes.SERVICE, 8192);
	final public static TextModel sql = new TextModel(TextTypes.SQL, 8192);
	final public static TextModel method = new TextModel(TextTypes.METHOD, 4096);
	final public static TextModel error = new TextModel(TextTypes.ERROR, 1024);
	final public static TextModel apicall = new TextModel(TextTypes.APICALL, 8192);
	final public static TextModel object = new TextModel(TextTypes.OBJECT, 1024);
	final public static TextModel referer = new TextModel(TextTypes.REFERER, 1024);
	final public static TextModel userAgent = new TextModel(TextTypes.USER_AGENT, 1024*20);
	final public static TextModel group = new TextModel(TextTypes.GROUP, 1024);
	final public static TextModel sql_tables = new TextModel(TextTypes.SQL_TABLES, 1024);
	final public static TextModel city = new TextModel(TextTypes.CITY, 8192);
	final public static TextModel maria = new TextModel(TextTypes.MARIA, 1024);
	final public static TextModel web = new TextModel(TextTypes.WEB, 500);
	final public static TextModel login = new TextModel(TextTypes.LOGIN, 1024*10);
	final public static TextModel desc = new TextModel(TextTypes.DESC, 1024*10);
	final public static TextModel hashMessage = new TextModel(TextTypes.HASH_MSG, 1024*10);
    final public static TextModel stackElement = new TextModel(TextTypes.STACK_ELEMENT, 8192);

	private static Map<String, TextModel> textModelMap = new HashMap<String, TextModel>();

	static {
		textModelMap.put(TextTypes.SERVICE, service);
		textModelMap.put(TextTypes.SQL, sql);
		textModelMap.put(TextTypes.METHOD, method);
		textModelMap.put(TextTypes.ERROR, error);
		textModelMap.put(TextTypes.APICALL, apicall);
		textModelMap.put(TextTypes.OBJECT, object);
		textModelMap.put(TextTypes.REFERER, referer);
		textModelMap.put(TextTypes.USER_AGENT, userAgent);
		textModelMap.put(TextTypes.GROUP, group);
		textModelMap.put(TextTypes.SQL_TABLES, sql_tables);
		textModelMap.put(TextTypes.CITY, city);
		textModelMap.put(TextTypes.MARIA, maria);
		textModelMap.put(TextTypes.WEB, web);
		textModelMap.put(TextTypes.LOGIN, login);
		textModelMap.put(TextTypes.DESC, desc);
		textModelMap.put(TextTypes.HASH_MSG, hashMessage);
        textModelMap.put(TextTypes.STACK_ELEMENT, stackElement);
	}
	
	public static TextModel getTextModel(String textType) {
		return textModelMap.get(textType);
	}
}
