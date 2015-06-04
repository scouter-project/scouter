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
package scouter.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ClassUtil {
	public static <V> Map<String, V> getPublicFinalNameMap(Class<?> cls, Class v) {

		Map<String, V> map = new HashMap<String, V>();
		Field[] fields = cls.getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();

			if (fields[i].getType().equals(v) == false) {
				continue;
			}
			if (Modifier.isFinal(mod) && Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
				try {
					String name = fields[i].getName();
					Object value = fields[i].get(null);
					map.put(name, (V) value);
				} catch (Exception e) {
				}
			}
		}
		return map;
	}

	public static <V> Map<V, String> getPublicFinalValueMap(Class<?> cls, Class type) {

		Map<V, String> map = new HashMap<V, String>();
		Field[] fields = cls.getFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();

			if (fields[i].getType().equals(type) == false) {
				continue;
			}
			if (Modifier.isFinal(mod) && Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
				try {
					String name = fields[i].getName();
					Object value = fields[i].get(null);
					map.put((V) value, name);
				} catch (Exception e) {
				}
			}
		}
		return map;
	}
	public static <V> Map<V, String> getPublicFinalDeclaredValueMap(Class<?> cls, Class type) {

		Map<V, String> map = new HashMap<V, String>();
		Field[] fields = cls.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			int mod = fields[i].getModifiers();

			if (fields[i].getType().equals(type) == false) {
				continue;
			}
			if (Modifier.isFinal(mod) && Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
				try {
					String name = fields[i].getName();
					Object value = fields[i].get(null);
					map.put((V) value, name);
				} catch (Exception e) {
				}
			}
		}
		return map;
	}
}
