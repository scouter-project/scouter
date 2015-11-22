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

package scouter.agent.asm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import scouter.util.StrMatch;
import scouter.util.StringUtil;

public class HookingSet {
	public byte xType = 0;
	public StrMatch classMatch = null;
	protected Map<String,Object> inner = new HashMap<String,Object>();

	private boolean all_flag = false;
	private int all_flag_value;

	public boolean isA(String method, String desc) {
		if (all_flag)
			return true;
		else if (this.contains(method))
			return true;
		else if (this.contains(method + desc))
			return true;
		return false;
	}

	public boolean contains(String name) {
		return inner.containsKey(name);
	}

	public void add(String mname) {
		if ("*".equals(mname)) {
			this.all_flag = true;
		} else {
			inner.put(mname, "");
		}
	}

	public void add(String mname, int idx) {
		if ("*".equals(mname)) {
			this.all_flag = true;
			this.all_flag_value = idx;
		} else {
			this.inner.put(mname, idx);
		}
	}

	public int get(String method, String desc) {
		if (all_flag)
			return all_flag_value;

		Integer i = (Integer) this.inner.get(method);
		if (i != null) {
			return i.intValue();
		}
		i = (Integer) this.inner.get(method);
		if (i != null) {
			return i.intValue();
		}
		return -1;
	}

	public static Map<String, HookingSet> getHookingSet(String arg) {
		String[] c = StringUtil.split(arg, ',');
		Map<String, HookingSet> classSet = new HashMap<String, HookingSet>();
		for (int i = 0; i < c.length; i++) {
			String s = c[i];
			int x = s.lastIndexOf(".");
			if (x <= 0)
				continue;
			String cname = s.substring(0, x).replace('.', '/');
			String mname = s.substring(x + 1);

			HookingSet methodSet = classSet.get(cname);
			if (methodSet == null) {
				methodSet = new HookingSet();
				classSet.put(cname, methodSet);
			}
			methodSet.add(mname);
		}
		return classSet;
	}

	public static List<HookingSet> getHookingMethodSet(String arg) {
		String[] c = StringUtil.split(arg, ',');

		Map<String, HookingSet> classSet = new HashMap<String, HookingSet>();
		for (int i = 0; i < c.length; i++) {
			String s = c[i];
			int x = s.lastIndexOf(".");
			if (x <= 0)
				continue;
			String cname = s.substring(0, x).replace('.', '/').trim();
			String mname = s.substring(x + 1).trim();

			HookingSet methodSet = classSet.get(cname);
			if (methodSet == null) {
				methodSet = new HookingSet();
				classSet.put(cname, methodSet);
			}

			methodSet.add(mname);
		}

		List<HookingSet> list = new ArrayList<HookingSet>();
		Iterator<Entry<String, HookingSet>> itr = classSet.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<String, HookingSet> e = itr.next();
			e.getValue().classMatch = new StrMatch(e.getKey());
			list.add(e.getValue());
		}

		return list;
	}

	public static void setHookingMethod(Map<String, HookingSet> classSet, String cname, String mname) {

		HookingSet methodSet = classSet.get(cname);
		if (methodSet == null) {
			methodSet = new HookingSet();
			classSet.put(cname, methodSet);
		}
		methodSet.add(mname);
	}

	public static void add(List<HookingSet> list, String classname, String method) {
		add(list, classname, method, (byte) 0);
	}

	public static void add(List<HookingSet> list, String classname, String method, byte serviceType) {
		for (int i = 0; i < list.size(); i++) {
			HookingSet m = list.get(i);
			if (m.classMatch.include(classname)) {
				m.add(method);
				return;
			}
		}
		HookingSet m = new HookingSet();
		m.xType = serviceType;
		m.classMatch = new StrMatch(classname);
		m.add(method);
		list.add(m);
	}

	public static HashSet<String> getHookingClassSet(String arg) {
		String[] c = StringUtil.tokenizer(arg, ",");
		HashSet<String> classSet = new HashSet<String>();
		if(c ==null)
			return classSet;
		for (int i = 0; i < c.length; i++) {
			classSet.add(c[i]);
		}
		return classSet;
	}
	public static Map<String, String> getClassFieldSet(String arg) {
		String[] c = StringUtil.split(arg, ',');

		Map<String, String> m = new HashMap<String, String>();
		for (int i = 0; i < c.length; i++) {
			String s = c[i];
			int x = s.lastIndexOf(".");
			if (x <= 0)
				continue;
			String cname = s.substring(0, x).replace('.', '/').trim();
			String mname = s.substring(x + 1).trim();

			m.put(cname, mname);
		}

		return m;
	}

	public static Set<String> getClassSet(String arg) {
		String[] c = StringUtil.split(arg, ',');

		Set<String> m = new HashSet<String>();
		for (int i = 0; i < c.length; i++) {
			String s = c[i];
			m.add(s.replace('.', '/'));
		}

		return m;
	}
}