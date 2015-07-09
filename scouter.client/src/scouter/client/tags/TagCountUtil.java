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
 */
package scouter.client.tags;

import java.util.ArrayList;
import java.util.List;

import scouter.client.model.TextProxy;
import scouter.lang.AlertLevel;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.IP4Value;
import scouter.lang.value.ListValue;
import scouter.lang.value.TextHashValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.util.CastUtil;

public class TagCountUtil {
	
	// This should be called background thread.
	public static List<String> loadTagString(int serverId, String date, List<Value> vList, String tagName) {
		List<String> resultList = new ArrayList<String>();
		if (vList.size() == 0) return resultList;
		Value sample = vList.get(0);
		switch(sample.getValueType()) {
			case ValueEnum.TEXT_HASH:
				ListValue lv = new ListValue();
				for (Value v : vList) {
					lv.add(CastUtil.cint(v.toJavaObject()));
				}
				if (tagName.equals("object")){
					TextProxy.object.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.object.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("service")) {
					TextProxy.service.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.service.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("user-agent")) {
					TextProxy.userAgent.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.userAgent.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("group")) {
					TextProxy.group.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.group.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("city")) {
					TextProxy.city.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.city.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("referer")) {
					TextProxy.referer.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.referer.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("error")) {
					TextProxy.error.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.error.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("sql")) {
					TextProxy.sql.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.sql.getText(lv.getInt(i)));
					}
				} else if (tagName.equals("apicall")) {
					TextProxy.apicall.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.apicall.getText(lv.getInt(i)));
					}
				}
				break;
			case ValueEnum.DECIMAL:
				if (tagName.equals("level")) {
					for (Value v : vList) {
						resultList.add(AlertLevel.getName((byte)((DecimalValue)v).value));
					}
				} else {
					for (Value v : vList) {
						resultList.add(CastUtil.cString(v.toJavaObject()));
					}
				}
				break;
			case ValueEnum.IP4ADDR:
				for (Value v : vList) {
					resultList.add(v.toString());
				}
				break;
			case ValueEnum.TEXT:
				for (Value v : vList) {
					resultList.add(v.toString());
				}
				break;
			default :
				for (Value v : vList) {
					resultList.add(v.toString());
				}
				break;
		}
		return resultList;
	}
	
	public static Value convertTagToValue(String tagName, String tagValue) {
		if (tagName.equals("object")
			|| tagName.equals("service")
			|| tagName.equals("group")
			|| tagName.equals("user-agent")
			|| tagName.equals("referer")
			|| tagName.equals("city")
			|| tagName.equals("error")
			|| tagName.equals("object")
			|| tagName.equals("object")) {
			return new TextHashValue(tagValue);
		} else if (tagName.equals("visitor")
			|| tagName.equals("elapsed")
			|| tagName.equals("sqltime")
			|| tagName.equals("apitime")
			|| tagName.equals("level")) {
			return new DecimalValue(Long.valueOf(tagValue));
		} else if (tagName.equals("ip")) {
			return new IP4Value(tagValue);
		}
		return new TextValue(tagValue);
	}

}
