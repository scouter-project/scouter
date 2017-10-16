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

package scouterx.webapp.framework.client.model;

import lombok.Getter;
import scouter.lang.TextTypes;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum TextTypeEnum {
	ERROR(TextTypes.ERROR, TextProxy.error),
	APICALL(TextTypes.APICALL, TextProxy.apicall),
	METHOD(TextTypes.METHOD, TextProxy.method),
	SERVICE(TextTypes.SERVICE, TextProxy.service),
	SQL(TextTypes.SQL, TextProxy.sql),
	OBJECT(TextTypes.OBJECT, TextProxy.object),
	REFERRER(TextTypes.REFERER, TextProxy.referrer),
	USER_AGENT(TextTypes.USER_AGENT, TextProxy.userAgent),
	GROUP(TextTypes.GROUP, TextProxy.group),
	CITY(TextTypes.CITY, TextProxy.city),
	SQL_TABLES(TextTypes.SQL_TABLES, TextProxy.sql_tables),
	MARIA(TextTypes.MARIA, TextProxy.maria),
	LOGIN(TextTypes.LOGIN, TextProxy.login),
	DESC(TextTypes.DESC, TextProxy.desc),
	WEB(TextTypes.WEB, TextProxy.web),
	HASH_MSG(TextTypes.HASH_MSG, TextProxy.hashMessage),
	STACK_ELEMENT(TextTypes.STACK_ELEMENT, TextProxy.stackElement),
	;

	private final static Map<TextModel, TextTypeEnum> modelMap = new HashMap<>();
	private final static Map<String, TextTypeEnum> nameMap = new HashMap<>();

	static {
		for (TextTypeEnum textTypeEnum : TextTypeEnum.values()) {
			modelMap.put(textTypeEnum.textModel, textTypeEnum);
			nameMap.put(textTypeEnum.typeName, textTypeEnum);
		}
	}

	private final String typeName;
	private final TextModel textModel;

	TextTypeEnum(String typeName, TextModel textModel) {
		this.typeName = typeName;
		this.textModel = textModel;
	}

	public static TextTypeEnum of(TextModel textModel) {
		return modelMap.get(textModel);
	}

	public static TextTypeEnum of(String typeName) {
		return nameMap.get(typeName);
	}

}
