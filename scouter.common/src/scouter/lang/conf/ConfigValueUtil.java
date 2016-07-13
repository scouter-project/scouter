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

package scouter.lang.conf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import scouter.lang.value.BooleanValue;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.DoubleValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.NullValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.util.ArrayUtil;
import scouter.util.ParamText;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;

public class ConfigValueUtil {
	public static Properties replaceSysProp(Properties temp) {
		Properties p = new Properties();

		Map<Object, Object> args = new HashMap<Object, Object>();
		args.putAll(System.getenv());
		args.putAll(System.getProperties());

		p.putAll(args);

		Iterator<Object> itr = temp.keySet().iterator();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			String value = (String) temp.get(key);
			p.put(key, new scouter.util.ParamText(StringUtil.trim(value)).getText(args));
		}
		return p;
	}
	
	public static StringKeyLinkedMap<Object> getConfigDefault(Object o) {
		StringKeyLinkedMap<Object> map = new StringKeyLinkedMap<Object>();
		Field[] fields = o.getClass().getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();

			if (Modifier.isStatic(mod) == false && Modifier.isPublic(mod)) {
				try {
					String name = fields[i].getName();
					Object value = fields[i].get(o);
					map.put(name, value);
				} catch (Exception e) {
				}
			}
		}
		return map;
	}
	
	public static StringKeyLinkedMap<String> getConfigDescMap(Object o) {
		StringKeyLinkedMap<String> descMap = new StringKeyLinkedMap<String>();
		Field[] fields = o.getClass().getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();
			if (Modifier.isStatic(mod) == false && Modifier.isPublic(mod)) {
				try {
					ConfigDesc desc = fields[i].getAnnotation(ConfigDesc.class);
					if (desc != null && StringUtil.isNotEmpty(desc.value())) {
						String name = fields[i].getName();
						descMap.put(name, desc.value());
					}
				} catch (Exception e) {
				}
			}
		}
		return descMap;
	}

	public static Value toValue(Object o) {
		if (o == null)
			return new NullValue();
		if (o instanceof Float) {
			return new FloatValue(((Float) o).floatValue());
		}
		if (o instanceof Double) {
			return new DoubleValue(((Double) o).doubleValue());
		}
		if (o instanceof Number) {
			return new DecimalValue(((Number) o).longValue());
		}
		if (o instanceof Boolean) {
			return new BooleanValue(((Boolean) o).booleanValue());
		}
		if (o.getClass().isArray()) {
			String s = ArrayUtil.toString(o);
			return new TextValue(s.substring(1, s.length() - 1));
		}
		return new TextValue(o.toString());
	}

}