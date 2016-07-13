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
package scouter.client.tags;

import java.util.ArrayList;
import java.util.List;

import scouter.client.model.TextProxy;
import scouter.lang.AlertLevel;
import scouter.lang.constants.TagConstants;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.IP4Value;
import scouter.lang.value.ListValue;
import scouter.lang.value.TextHashValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.util.CastUtil;
import scouter.util.IntSet;
import scouter.util.StringSet;

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
				if (tagName.equals(TagConstants.NAME_OBJECT)){
					TextProxy.object.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.object.getText(lv.getInt(i)));
					}
				} else if (TagConstants.serviceHashGroup.hasKey(tagName)) {
					TextProxy.service.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.service.getText(lv.getInt(i)));
					}
				} else if (tagName.equals(TagConstants.NAME_USER_AGENT)) {
					TextProxy.userAgent.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.userAgent.getText(lv.getInt(i)));
					}
				} else if (tagName.equals(TagConstants.NAME_GROUP)) {
					TextProxy.group.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.group.getText(lv.getInt(i)));
					}
				} else if (tagName.equals(TagConstants.NAME_CITY)) {
					TextProxy.city.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.city.getText(lv.getInt(i)));
					}
				} else if (tagName.equals(TagConstants.NAME_REFERER)) {
					TextProxy.referer.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.referer.getText(lv.getInt(i)));
					}
				} else if (tagName.equals(TagConstants.NAME_ERROR)) {
					TextProxy.error.load(date, lv, serverId);
					for (int i = 0; i < lv.size(); i++) {
						resultList.add(TextProxy.error.getText(lv.getInt(i)));
					}
				}
				break;
			case ValueEnum.DECIMAL:
				if (tagName.equals(TagConstants.NAME_LEVEL)) {
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
		if (tagName.equals(TagConstants.NAME_OBJECT)
			|| tagName.equals(TagConstants.NAME_SERVICE)
			|| tagName.equals(TagConstants.NAME_SERVICE_TIME_SUM)
			|| tagName.equals(TagConstants.NAME_SERVICE_KBYTE_SUM)
			|| tagName.equals(TagConstants.NAME_SERVICE_ERROR_SUM)
			|| tagName.equals(TagConstants.NAME_GROUP)
			|| tagName.equals(TagConstants.NAME_USER_AGENT)
			|| tagName.equals(TagConstants.NAME_REFERER)
			|| tagName.equals(TagConstants.NAME_CITY)
			|| tagName.equals(TagConstants.NAME_ERROR)
			||TagConstants.serviceHashGroup.hasKey(tagName)
			) {
			return new TextHashValue(tagValue);
//		}		else if (tagName.equals(TagConstants.NAME_USERID)
//			|| tagName.equals(TagConstants.NAME_ELAPSED)
//			|| tagName.equals(TagConstants.NAME_SQLTIME)
//			|| tagName.equals(TagConstants.NAME_APITIME)){
//			return new DecimalValue(Long.valueOf(tagValue));
		} else if(tagName.equals(TagConstants.NAME_LEVEL)) {
			return new DecimalValue(AlertLevel.getValue(tagValue));
//		} else if (tagName.equals(TagConstants.NAME_IP)) {
//			return new IP4Value(tagValue);
		}
		return new TextValue(tagValue);
	}

}
