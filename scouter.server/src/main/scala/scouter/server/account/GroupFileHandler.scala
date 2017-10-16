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

package scouter.server.account;

import java.io.File

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

import javax.xml.parsers.DocumentBuilderFactory
import scouter.lang.pack.MapPack
import scouter.lang.value.BooleanValue
import scouter.lang.value.MapValue
import scouter.server.Logger
import scouter.server.util.EnumerScala
import scouter.server.util.XmlUtil
import scouter.util.ArrayUtil
import scouter.util.CastUtil
import scouter.util.StringKeyLinkedMap

object GroupFileHandler {

    val TAG_GROUPS = "Groups";
    val TAG_GROUP = "Group";
    val TAG_POLICY = "Policy";
    val ATTR_NAME = "name";

    def parse(file: File): StringKeyLinkedMap[MapValue] = {
        val groupMap = new StringKeyLinkedMap[MapValue]();
        val docBuilderFactory = DocumentBuilderFactory.newInstance();
        val docBuilder = docBuilderFactory.newDocumentBuilder();
        val doc = docBuilder.parse(file);
        doc.getDocumentElement().normalize();
        val groupList = doc.getElementsByTagName("Group");
        EnumerScala.foreach(groupList, (group: Node) => {
            if (group.getNodeType() == Node.ELEMENT_NODE) {
                val mv = new MapValue();
                val grpElement = group.asInstanceOf[Element];
                val grpName = grpElement.getAttribute("name");
                groupMap.put(grpName, mv);
                val childList = grpElement.getElementsByTagName("Policy").item(0).getChildNodes();

                EnumerScala.foreach(childList, (node: Node) => {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        var allowName = ""
                        var value = ""
                        try {
                            val ele = node.asInstanceOf[Element]
                            allowName = ele.getTagName();
                            value = ele.getTextContent();
                            val allow = value.toBoolean
                            mv.put(allowName, new BooleanValue(allow));
                        } catch {
                            case e: Exception => System.err.println("Error policy  : " + allowName + "=" + value);
                        }
                    }
                })

            }
        })
        return groupMap;
    }

    def editGroupPolicy(file: File, pack: MapPack): Boolean = {
        try {
            val docBuilderFactory = DocumentBuilderFactory.newInstance();
            val docBuilder = docBuilderFactory.newDocumentBuilder();
            val doc = docBuilder.parse(file);
            doc.getDocumentElement().normalize();
            val nodeList = doc.getElementsByTagName(TAG_GROUP);
            val itr = pack.keys();
            while (itr.hasNext()) {
                val group = itr.next();
                findAndSet(pack, doc, nodeList, group)
            }
            XmlUtil.writeXmlFileWithIndent(doc, file, 2);
            return true;
        } catch {
            case e: Exception =>
                Logger.println(e.getMessage());
        }
        return false;
    }

    def addAccountGroup(file: File, name: String, policyMap: MapValue): Boolean = {
        try {
            val docBuilderFactory = DocumentBuilderFactory.newInstance();
            val docBuilder = docBuilderFactory.newDocumentBuilder();
            val doc = docBuilder.parse(file);
            doc.getDocumentElement().normalize();
            val groupElement = doc.createElement(TAG_GROUP);
            groupElement.setAttribute(ATTR_NAME, name);
            doc.getElementsByTagName(TAG_GROUPS).item(0).appendChild(groupElement);
            val policyElement = doc.createElement(TAG_POLICY);
            groupElement.appendChild(policyElement);
            val keys = policyMap.keys();
            while (keys.hasMoreElements()) {
                val policy = keys.nextElement();
                setTextValue(doc, policyElement, policy, String.valueOf(policyMap.getBoolean(policy)));
            }
            XmlUtil.writeXmlFileWithIndent(doc, file, 2);
            return true;
        } catch {
            case e: Exception => Logger.println(e.getMessage());
        }
        return false;
    }

    private def setTextValue(doc: Document, element: Element, tagName: String, text: String) {
        var allowElement: Element = null;
        var nodeList = element.getElementsByTagName(tagName);
        if (nodeList == null || nodeList.getLength() < 1) {
            allowElement = doc.createElement(tagName);
            element.appendChild(allowElement);
        } else {
            allowElement = nodeList.item(0).asInstanceOf[Element]
        }
        allowElement.setTextContent(text);
    }

    private def findAndSet(pack: scouter.lang.pack.MapPack, doc: org.w3c.dom.Document, nodeList: org.w3c.dom.NodeList, group: String): Unit = {
        EnumerScala.foreach(nodeList, (node: Node) => {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                val element = node.asInstanceOf[Element]
                val name = element.getAttribute(ATTR_NAME);
                if (group.equals(name)) {
                    val policyElement = element.getElementsByTagName(TAG_POLICY).item(0).asInstanceOf[Element]
                    val mv = pack.get(group).asInstanceOf[MapValue]
                    EnumerScala.foreach(mv.keySet().iterator(), (policy: String) => {
                        setTextValue(doc, policyElement, policy, CastUtil.cString(mv.get(policy)));
                    })
                    return //종로 
                }
            }
        })
    }
}
