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
package scouter.lang.counters;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import scouter.lang.Counter;
import scouter.lang.Family;
import scouter.lang.ObjectType;
import scouter.util.FileUtil;
import scouter.util.StringKeyLinkedMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public class CounterEngine {
	
	public static final String TAG_COUNTERS = "Counters";
	public static final String TAG_FAMILYS = "Familys";
	public static final String TAG_TYPES = "Types";
	public static final String TAG_FAMILY = "Family";
	public static final String TAG_COUNTER = "Counter";
	public static final String TAG_OBJECT_TYPE = "ObjectType";

	public static final String ATTR_NAME = "name";
	public static final String ATTR_MASTER = "master";
	public static final String ATTR_DISPLAY = "disp";
	public static final String ATTR_UNIT = "unit";
	public static final String ATTR_ICON = "icon";
	public static final String ATTR_FAMILY = "family";
	public static final String ATTR_ALL = "all";
	public static final String ATTR_TOTAL = "total";
	public static final String ATTR_SUBOBJECT = "sub-object";
	
	private StringKeyLinkedMap<Family> familyMap = new StringKeyLinkedMap<Family>();
	private StringKeyLinkedMap<ObjectType> objTypeMap = new StringKeyLinkedMap<ObjectType>();
	
	public boolean parse(byte[] content) {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new InputSource(new ByteArrayInputStream(content)));
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName(TAG_FAMILY);
			for (int i = 0; nodeList != null && i <  nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element familyElement = (Element) node;
					NamedNodeMap nodeMap = familyElement.getAttributes();
					if (nodeMap == null) {
						continue;
					}
					Family family = new Family();
					for (int j = 0; j < nodeMap.getLength(); j++) {
						Node familyAttr = nodeMap.item(j);
						if (familyAttr.getNodeType() == Node.ATTRIBUTE_NODE) {
							Attr attr = (Attr) familyAttr;
							String name = attr.getName();
							String value = attr.getValue();
							if (ATTR_NAME.equals(name)) {
								family.setName(value);
							} else if (ATTR_MASTER.equals(name)) {
								family.setMaster(value);
							} else {
								family.setAttribute(name, value);
							}
						}
					}
					if (family.getName() != null) {
						familyMap.put(family.getName(), family);
					}
					NodeList counterNodes = familyElement.getElementsByTagName(TAG_COUNTER);
					for (int j = 0; counterNodes != null && j <  counterNodes.getLength(); j++) {
						Node counterNode = counterNodes.item(j);
						if (counterNode.getNodeType() == Node.ELEMENT_NODE) {
							Element counterElement = (Element) counterNode;
							NamedNodeMap counterAttrMap = counterElement.getAttributes();
							if (counterAttrMap == null) {
								continue;
							}
							scouter.lang.Counter counter = new scouter.lang.Counter();
							for (int k = 0; k < counterAttrMap.getLength(); k++) {
								Node counterAttr = counterAttrMap.item(k);
								if (counterAttr.getNodeType() == Node.ATTRIBUTE_NODE) {
									Attr attr = (Attr) counterAttr;
									String name = attr.getName();
									String value = attr.getValue();
									if (ATTR_NAME.equals(name)) {
										counter.setName(value);
									} else if (ATTR_DISPLAY.equals(name)) {
										counter.setDisplayName(value);
									} else if (ATTR_UNIT.equals(name)) {
										counter.setUnit(value);
									} else if (ATTR_ICON.equals(name)) {
										counter.setIcon(value);
									} else if (ATTR_ALL.equals(name)) {
										counter.setAll(Boolean.valueOf(value));
									} else if (ATTR_TOTAL.equals(name)) {
										counter.setTotal(Boolean.valueOf(value));
									} else {
										counter.setAttribute(name, value);
									}
								}
							}
							if (counter.getName() != null) {
								family.addCounter(counter);
							}
						}
					}
				}
			}
			nodeList = doc.getElementsByTagName(TAG_OBJECT_TYPE);
			for (int i = 0; nodeList != null && i <  nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element objElement = (Element) node;
					NamedNodeMap nodeMap = objElement.getAttributes();
					if (nodeMap == null) {
						continue;
					}
					ObjectType objType = new ObjectType();
					for (int j = 0; j < nodeMap.getLength(); j++) {
						Node objAttr = nodeMap.item(j);
						if (objAttr.getNodeType() == Node.ATTRIBUTE_NODE) {
							Attr attr = (Attr) objAttr;
							String name = attr.getName();
							String value = attr.getValue();
							if (ATTR_NAME.equals(name)) {
								objType.setName(value);
							} else if (ATTR_DISPLAY.equals(name)) {
								objType.setDisplayName(value);
							} else if (ATTR_FAMILY.equals(name)) {
								objType.setFamily(familyMap.get(value));
							} else if (ATTR_ICON.equals(name)) {
								objType.setIcon(value);
							} else if (ATTR_SUBOBJECT.equals(name)) {
								objType.setSubObject(Boolean.valueOf(value));
							} else {
								objType.setAttribute(name, value);
							}
						}
					}
					if (objType.getName() != null) {
						ObjectType existType = objTypeMap.get(objType.getName());
						if (existType == null) {
							objTypeMap.put(objType.getName(), objType);
						} else {
							objType = existType;
						}
					}
					NodeList counterNodes = objElement.getElementsByTagName(TAG_COUNTER);
					for (int j = 0; counterNodes != null && j <  counterNodes.getLength(); j++) {
						Node counterNode = counterNodes.item(j);
						if (counterNode.getNodeType() == Node.ELEMENT_NODE) {
							Element counterElement = (Element) counterNode;
							NamedNodeMap counterAttrMap = counterElement.getAttributes();
							if (counterAttrMap == null) {
								continue;
							}
							scouter.lang.Counter counter = new scouter.lang.Counter();
							for (int k = 0; k < counterAttrMap.getLength(); k++) {
								Node counterAttr = counterAttrMap.item(k);
								if (counterAttr.getNodeType() == Node.ATTRIBUTE_NODE) {
									Attr attr = (Attr) counterAttr;
									String name = attr.getName();
									String value = attr.getValue();
									if (ATTR_NAME.equals(name)) {
										counter.setName(value);
									} else if (ATTR_DISPLAY.equals(name)) {
										counter.setDisplayName(value);
									} else if (ATTR_UNIT.equals(name)) {
										counter.setUnit(value);
									} else if (ATTR_ICON.equals(name)) {
										counter.setIcon(value);
									} else if (ATTR_ALL.equals(name)) {
										counter.setAll(Boolean.valueOf(value));
									} else if (ATTR_TOTAL.equals(name)) {
										counter.setTotal(Boolean.valueOf(value));
									} else {
										counter.setAttribute(name, value);
									}
								}
							}
							if (counter.getName() != null) {
								objType.addCounter(counter);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public synchronized void clear() {
		familyMap.clear();
		objTypeMap.clear();
	}
	
	public String[] getChildren(String family) {
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<ObjectType> types = objTypeMap.values();
		while (types.hasMoreElements()) {
			ObjectType obj = types.nextElement();
			if (family.equals(obj.getFamily().getName())) {
				list.add(obj.getName());
			}
		}
		return list.toArray(new String[list.size()]);
	}
	
	public ArrayList<String> getObjTypeListWithDisplay(String attr) {
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<ObjectType> types = objTypeMap.values();
		while (types.hasMoreElements()) {
			ObjectType obj = types.nextElement();
			Family family = obj.getFamily();
			if (family.isTrueAttribute(attr)) {
				list.add(obj.getDisplayName() + ":" + obj.getName());
			}
		}
		return list;
	}
	
	public ArrayList<String> getAllObjectType() {
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<ObjectType> types = objTypeMap.values();
		while (types.hasMoreElements()) {
			ObjectType obj = types.nextElement();
			list.add(obj.getName());
		}
		Collections.sort(list);
		return list;
	}
	
	public String getDisplayNameObjectType(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return "<unknown>";
		} else {
			return obj.getDisplayName();
		}
	}

	public ArrayList<String> getAllCounterList() {
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<ObjectType> types = objTypeMap.values();
		while (types.hasMoreElements()) {
			ObjectType obj = types.nextElement();
			scouter.lang.Counter[] counters = obj.listCounters();
			if(counters == null || counters.length == 0) continue;
			for (scouter.lang.Counter counter : counters) {
				if (counter.isAll()) {
					list.add(obj.getName() + ":" + counter.getDisplayName() + ":" + counter.getName());
				}
			}
		}
		return list;
	}
	
	public ArrayList<String> getAllCounterList(String objType) {
		ArrayList<String> list = new ArrayList<String>();
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return list;
		}
		scouter.lang.Counter[] counters = obj.listCounters();
		for (scouter.lang.Counter counter : counters) {
			if (counter.isAll()) {
				list.add(counter.getName());
			}
		}
		return list;
	}
	
	public ArrayList<String> getTotalCounterList() {
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<ObjectType> types = objTypeMap.values();
		while (types.hasMoreElements()) {
			ObjectType obj = types.nextElement();
			scouter.lang.Counter[] counters = obj.listCounters();
			for (scouter.lang.Counter counter : counters) {
				if (counter.isTotal()) {
					list.add(obj.getName() + ":" + counter.getDisplayName() + ":" + counter.getName());
				}
			}
		}
		return list;
	}
	
	public ArrayList<String> getTotalCounterList(String objType) {
		ArrayList<String> list = new ArrayList<String>();
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return list;
		}
		scouter.lang.Counter[] counters = obj.listCounters();
		for (scouter.lang.Counter counter : counters) {
			if (counter.isTotal()) {
				list.add(counter.getName());
			}
		}
		return list;
	}
	
	public String getCounterDisplayName(String objType, String counter) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return "";
		}
		scouter.lang.Counter c = obj.getCounter(counter);
		if (c == null) {
			return "";
		}
		return c.getDisplayName();
	}
	
	public String getMasterCounterUnit(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return "";
		}
		String master = obj.getFamily().getMaster();
		scouter.lang.Counter counter = obj.getCounter(master);
		if (counter == null) {
			return "";
		}
		return counter.getUnit();
	}
	
	public String getMasterCounter(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return null;
		}
		Family family = obj.getFamily();
		if (family == null) {
			return null;
		}
		return family.getMaster();
	}
	
	public String[] getSortedCounterName(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return null;
		}
		ArrayList<String> list = new ArrayList<String>();
		scouter.lang.Counter[] counters = obj.listCounters();
		for (scouter.lang.Counter counter : counters) {
			list.add(counter.getName());
		}
		Collections.sort(list);
		return list.toArray(new String[list.size()]);
	}
	
	public String[] getSortedCounterDisplayName(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return null;
		}
		ArrayList<String> list = new ArrayList<String>();
		scouter.lang.Counter[] counters = obj.listCounters();
		for (scouter.lang.Counter counter : counters) {
			list.add(counter.getDisplayName());
		}
		Collections.sort(list);
		return list.toArray(new String[list.size()]);
	}
	
	public ArrayList<String> getAllCounterWithDisplay(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return null;
		}
		ArrayList<String> list = new ArrayList<String>();
		scouter.lang.Counter[] counters = obj.listCounters();
		for (scouter.lang.Counter counter : counters) {
			if (counter.isAll()) {
				list.add(counter.getDisplayName() + ":" + counter.getName());
			}
		}
		Collections.sort(list);
		return list;
	}
	
	public boolean isChildOf(String objType, String familyName) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return false;
		}
		return familyName.equalsIgnoreCase(obj.getFamily().getName());
	}
	
	public boolean isCounterOf(String counter, String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return false;
		}
		return obj.getCounter(counter) != null;
	}
	
	public Set<String> getCounterSet(String objType) {
		HashSet<String> set = new HashSet<String>();
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return set;
		}
		scouter.lang.Counter[] counters = obj.listCounters();
		for (scouter.lang.Counter counter : counters) {
			set.add(counter.getName());
		}
		return set;
	}
	
	public Set<Counter> getCounterObjectSet(String objType) {
		HashSet<Counter> set = new HashSet<Counter>();
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return set;
		}
		scouter.lang.Counter[] counters = obj.listCounters();
		for (scouter.lang.Counter counter : counters) {
			set.add(counter);
		}
		return set;
	}
	
	public String getCounterIconFileName(String objType, String counter) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return null;
		}
		scouter.lang.Counter c = obj.getCounter(counter);
		if (c == null) {
			return "";
		}
		return c.getIcon();
	}
	
	public String getCounterUnit(String objType, String counter) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return null;
		}
		scouter.lang.Counter c = obj.getCounter(counter);
		if (c == null) {
			return "";
		}
		return c.getUnit();
	}
	
	public boolean isTrueAction(String objType, String actionName) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return false;
		}
		return obj.getFamily().isTrueAttribute(actionName);
	}
	
	public Family getFamily(String familyName) {
		return familyMap.get(familyName);
	}
	
	public ObjectType getObjectType(String objType) {
		return objTypeMap.get(objType);
	}

	public String getFamilyNameFromObjType(String objType) {
		if (objTypeMap.get(objType) == null) {
			return "UNKNOWN";
		}
		return objTypeMap.get(objType).getFamily().getName();
	}
	
	public boolean isUnknownObjectType(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return true;
		}
		return false;
	}
	
	public boolean isPrimaryObject(String objType) {
		ObjectType obj = objTypeMap.get(objType);
		if (obj == null) {
			return false;
		}
		if (obj.isSubObject()) {
			return false;
		}
		return true;
	}
	
	public ObjectType addObjectType(ObjectType objType) {
		return objTypeMap.put(objType.getName(), objType);
	}
	
	public Family addFamily(Family family) {
		return familyMap.put(family.getName(), family);
	}
	
	public String[] getFamilyNames() {
		return familyMap.keyArray();
	}

	public StringKeyLinkedMap<ObjectType> getRawObjectTypeMap() {
		return objTypeMap;
	}

	public StringKeyLinkedMap<Family> getRawFamilyMap() {
		return familyMap;
	}
	
	public static void main(String[] args) {

		File f = new File("/Users/gunlee/Documents/workspace/scouter/scouter/scouter.common/src/main/resources/scouter/lang/counters/counters.xml");
		System.out.println(f.canRead());
		byte[] content = FileUtil.readAll(f);
		CounterEngine ce = new CounterEngine();
		ce.parse(content);
		System.out.println(ce.getAllObjectType());
		System.out.println(ce.getDisplayNameObjectType("tomcat"));
		System.out.println(ce.getAllCounterList());
		System.out.println(ce.getTotalCounterList());
		System.out.println(ce.getCounterDisplayName("tomcat", "visit0"));
		System.out.println(Arrays.toString(ce.getSortedCounterName("tomcat")));
		System.out.println(Arrays.toString(ce.getSortedCounterDisplayName("tomcat")));
		System.out.println(ce.getCounterIconFileName("tomcat", "visit0"));
		System.out.println(ce.getCounterUnit("tomcat", "visit0"));
	}
}
