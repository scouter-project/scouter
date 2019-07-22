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

package scouter.agent.batch.netio.request.handle;

import scouter.agent.batch.Configure;
import scouter.agent.batch.JavaAgent;
import scouter.agent.batch.netio.request.anotation.RequestHandler;
import scouter.lang.conf.ValueType;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;
import scouter.util.ClassUtil;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringKeyLinkedMap.StringKeyLinkedEntry;

import java.lang.instrument.ClassDefinition;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

public class AgentConfigure {
	
	@RequestHandler(RequestCmd.GET_CONFIGURE_WAS)
	public Pack getAgentConfigure(Pack param) {
		MapPack p = new MapPack();
		
		p.put("configKey", Configure.getInstance().getKeyValueInfo().getList("key"));
		
		String config = Configure.getInstance().loadText();
		if (config == null) {
			//config = getEmptyConfiguration();
			config = "";
		}
		p.put("agentConfig", config);

		return p;
	}

	@RequestHandler(RequestCmd.SET_CONFIGURE_WAS)
	public Pack setAgentConfigure(Pack param) {
		final String setConfig = ((MapPack) param).getText("setConfig");
		boolean success = Configure.getInstance().saveText(setConfig);
		if (success) {
			Configure.getInstance().reload();
		}
		MapPack p = new MapPack();
		p.put("result", String.valueOf(success));
		return p;
	}
	
	@RequestHandler(RequestCmd.LIST_CONFIGURE_WAS)
	public Pack listConfigure(Pack param) {
		MapValue m = Configure.getInstance().getKeyValueInfo();
		MapPack pack = new MapPack();
		pack.put("key", m.getList("key"));
		pack.put("value", m.getList("value"));
		pack.put("default", m.getList("default"));
		return pack;
	}

	protected boolean applyConfigRunning = false;

	@RequestHandler(RequestCmd.REDEFINE_CLASSES)
	public Pack redefineClasses(Pack param) {
		final MapPack p = new MapPack();
		ListValue classLv = ((MapPack) param).getList("class");
		HashSet<String> paramSet = new HashSet<String>();
		for (int i = 0; i < classLv.size(); i++) {
			String className = classLv.getString(i);
			paramSet.add(className);
		}
		Class[] classes = JavaAgent.getInstrumentation().getAllLoadedClasses();
		ArrayList<ClassDefinition> definitionList = new ArrayList<ClassDefinition>();
		boolean allSuccess = true;
		for (int i = 0; paramSet.size() > 0 && i < classes.length; i++) {
			if (paramSet.contains(classes[i].getName())) {
				try {
					byte[] buff = ClassUtil.getByteCode(classes[i]);
					if (buff == null) {
						continue;
					}
					definitionList.add(new ClassDefinition(classes[i], buff));
					paramSet.remove(classes[i].getName());
				} catch (Exception e) {
					p.put("success", new BooleanValue(false));
					p.put("error", e.toString());
					allSuccess = false;
					break;
				}
			}
		}
		if (definitionList.size() > 0 && allSuccess) {
			try {
				JavaAgent.getInstrumentation().redefineClasses(definitionList.toArray(new ClassDefinition[definitionList.size()]));
				p.put("success", new BooleanValue(true));
			} catch (Throwable th) {
				p.put("success", new BooleanValue(false));
				p.put("error", th.toString());
			}
		}
		return p;
	}
	
	@RequestHandler(RequestCmd.CONFIGURE_DESC)
	public Pack getConfigureDesc(Pack param) {
		StringKeyLinkedMap<String> descMap = Configure.getInstance().getConfigureDesc();
		MapPack pack = new MapPack();
		Enumeration<StringKeyLinkedEntry<String>> entries = descMap.entries();
		while (entries.hasMoreElements()) {
			StringKeyLinkedEntry<String> entry = entries.nextElement();
			pack.put(entry.getKey(), entry.getValue());
		}
		return pack;
	}

	@RequestHandler(RequestCmd.CONFIGURE_VALUE_TYPE)
	public Pack getConfigureValueType(Pack param) {
		StringKeyLinkedMap<ValueType> valueTypeMap = Configure.getInstance().getConfigureValueType();
		MapPack pack = new MapPack();
		Enumeration<StringKeyLinkedEntry<ValueType>> entries = valueTypeMap.entries();
		while (entries.hasMoreElements()) {
			StringKeyLinkedEntry<ValueType> entry = entries.nextElement();
			pack.put(entry.getKey(), entry.getValue().getType());
		}
		return pack;
	}
}
