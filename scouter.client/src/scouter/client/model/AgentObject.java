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

import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Color;

import scouter.lang.pack.ObjectPack;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.StringUtil;

public class AgentObject extends HierarchyObject {
	private String objType;
	protected int objHash;
	protected String objName;
	private Color color;
	
	private String displayName;

	protected ObjectPack objPack;
	
	private int activeCounterInt = 0;
	
	private int serverId;

	public AgentObject(String objType, int objHash, String objName , int serverId) {
		this.objType = objType;
		this.objHash = objHash;
		this.objName = objName;
		this.color = AgentColorManager.getInstance().assignColor(objType, objHash);
		this.serverId = serverId;
		String[] objPaths = StringUtil.tokenizer(objName, "/");		
		this.displayName = objPaths[objPaths.length - 1];
	}
	
	public AgentObject(AgentObject another) {
		this.objType = another.getObjType();
		this.objHash = another.getObjHash();
		this.objName = another.getObjName();
		this.color = another.getColor();
		this.serverId = another.getServerId();
		this.displayName = another.getDisplayName();
		this.objPack = another.getSpec();
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public String getObjType() {
		return objType;
	}

	public void setObjType(String objType) {
		this.objType = objType;
	}

	public void setObjHash(int objHash) {
		this.objHash = objHash;
	}

	public void setObjName(String objName) {
		this.objName = objName;
	}

	public int getObjHash() {
		return objHash;
	}

	public String getObjName() {
		return objName;
	}

	public boolean isAlive() {
		if (objPack == null)
			return false;
		return objPack.alive;
	}

	public String toString() {
		return getObjName();
	}

	public ObjectPack getSpec() {
		return objPack;
	}

	public Value getMasterCounter() {
		if (objPack == null)
			return null;
		return objPack.tags.get("counter");
	}
	
	public int getActiveCounterInt() {
		if (objPack == null)
			return activeCounterInt;
		Value v = objPack.tags.get("counter");
		if(v instanceof Number){
			activeCounterInt = CastUtil.cint(v);
			return activeCounterInt;
		}
		return activeCounterInt;
	}
	
	public void setSpec(ObjectPack objPack) {
		this.objPack = objPack;
	}
	
	public Color getColor() {
		return this.color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + objHash;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AgentObject other = (AgentObject) obj;
		if (objHash != other.objHash)
			return false;
		return true;
	}

	public int getServerId() {
		return this.serverId;
	}

	public void setChildMap(Map<String, HierarchyObject> childMap) {
		Set<String> keys = childMap.keySet();
		for (String key : keys) {
			HierarchyObject value = childMap.get(key);
			value.setParent(this);
			super.putChild(key, value);
		}
	}

	public String getName() {
		return getObjName();
	}
}