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

import scouter.lang.value.BooleanValue;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.DoubleValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.NullValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.util.ArrayUtil;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

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

	public static StringKeyLinkedMap<ValueType> getConfigValueTypeMap(Object o) {
		StringKeyLinkedMap<ValueType> valueTypeMap = new StringKeyLinkedMap<ValueType>();
		Field[] fields = o.getClass().getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();
			if (Modifier.isStatic(mod) == false && Modifier.isPublic(mod)) {
				try {
					ValueType valueType;
					Class type = fields[i].getType();
					if (type == Integer.TYPE || type == Long.TYPE
							|| type.isAssignableFrom(Integer.class) || type.isAssignableFrom(Long.class)) {
						valueType = ValueType.NUM;
					} else if (type == Boolean.TYPE || type == Boolean.class) {
						valueType = ValueType.BOOL;
					} else {
						valueType = ValueType.VALUE;
					}

					ConfigValueType annotation = fields[i].getAnnotation(ConfigValueType.class);
					if (annotation != null) {
						valueType = annotation.value();
					}
					String name = fields[i].getName();
					valueTypeMap.put(name, valueType);
				} catch (Exception e) {
				}
			}
		}
		return valueTypeMap;
	}

	public static StringKeyLinkedMap<ValueTypeDesc> getConfigValueTypeDescMap(Object o) {
		StringKeyLinkedMap<ValueTypeDesc> valueTypeDescMap = new StringKeyLinkedMap<ValueTypeDesc>();
		Field[] fields = o.getClass().getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();
			if (Modifier.isStatic(mod) == false && Modifier.isPublic(mod)) {
				try {
					ConfigValueType annotation = fields[i].getAnnotation(ConfigValueType.class);
					if (annotation == null || annotation.value() != ValueType.COMMA_COLON_SEPARATED_VALUE) {
						continue;
					}
					ValueTypeDesc valueTypeDesc = new ValueTypeDesc();
					valueTypeDesc.setStrings(annotation.strings());
					valueTypeDesc.setStrings1(annotation.strings1());
					valueTypeDesc.setStrings2(annotation.strings2());
					valueTypeDesc.setStrings3(annotation.strings3());
					valueTypeDesc.setBooleans(annotation.booleans());
					valueTypeDesc.setBooleans1(annotation.booleans1());
					valueTypeDesc.setInts(annotation.ints());
					valueTypeDesc.setInts1(annotation.ints1());

					valueTypeDescMap.put(fields[i].getName(), valueTypeDesc);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return valueTypeDescMap;
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
