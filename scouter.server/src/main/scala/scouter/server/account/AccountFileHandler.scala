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
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import scouter.server.util.XmlUtil
import scouter.lang.Account
import scouter.util.StringKeyLinkedMap
import scouter.util.ArrayUtil
import scouter.server.util.EnumerScala
object AccountFileHandler {

    val TAG_ACCOUNTS = "Accounts";
    val TAG_ACCOUNT = "Account";
    val TAG_EMAIL = "Email";
    val ATTR_ID = "id";
    val ATTR_PASS = "pass";
    val ATTR_GROUP = "group";
    val TAG_HP = "hp";

    def parse(file: File): StringKeyLinkedMap[Account] = {
        val accountMap = new StringKeyLinkedMap[Account]();
        val docBuilderFactory = DocumentBuilderFactory.newInstance();
        val docBuilder = docBuilderFactory.newDocumentBuilder();
        val doc = docBuilder.parse(file);
        doc.getDocumentElement().normalize();
        val accountList = doc.getElementsByTagName(TAG_ACCOUNT);

        EnumerScala.foreach(accountList, (account: Node) => {
            if (account.getNodeType() == Node.ELEMENT_NODE) {
                val acObj = new Account();
                val accountElement = account.asInstanceOf[Element];
                acObj.id = accountElement.getAttribute(ATTR_ID);
                acObj.password = accountElement.getAttribute(ATTR_PASS);
                acObj.group = accountElement.getAttribute(ATTR_GROUP);
                acObj.email = extractTextValue(accountElement, TAG_EMAIL);
                acObj.hp = extractTextValue(accountElement, TAG_HP);
                accountMap.put(acObj.id, acObj);
            }
        })

        return accountMap;
    }

    def addAccount(file: File, account: Account) {
        val docBuilderFactory = DocumentBuilderFactory.newInstance();
        val docBuilder = docBuilderFactory.newDocumentBuilder();
        val doc = docBuilder.parse(file);
        doc.getDocumentElement().normalize();
        removeWhitespaceNodes(doc.getDocumentElement)
        val accounts = doc.getElementsByTagName(TAG_ACCOUNTS).item(0);
        val accountEle = doc.createElement(TAG_ACCOUNT);
        accountEle.setAttribute(ATTR_ID, account.id);
        accountEle.setAttribute(ATTR_PASS, account.password);
        accountEle.setAttribute(ATTR_GROUP, account.group);
        val emailEle = doc.createElement(TAG_EMAIL);
        emailEle.setTextContent(account.email);
        accountEle.appendChild(emailEle);
        accounts.appendChild(accountEle);
        val hpEle = doc.createElement(TAG_HP);
        hpEle.setTextContent(account.hp);
        accountEle.appendChild(hpEle);
        accounts.appendChild(accountEle);
        XmlUtil.writeXmlFileWithIndent(doc, file, 2);
    }

    def editAccount(file: File, account: Account) {
        val docBuilderFactory = DocumentBuilderFactory.newInstance();
        val docBuilder = docBuilderFactory.newDocumentBuilder();
        val doc = docBuilder.parse(file);
        doc.getDocumentElement().normalize();
        removeWhitespaceNodes(doc.getDocumentElement)
        val nodeList = doc.getElementsByTagName(TAG_ACCOUNT);

        EnumerScala.foreach(nodeList, (node: Node) => {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                val element = node.asInstanceOf[Element];
                val id = element.getAttribute(ATTR_ID);
                if (account.id.equals(id)) {
                    element.setAttribute(ATTR_PASS, account.password);
                    element.setAttribute(ATTR_GROUP, account.group);
                    val email = element.getElementsByTagName(TAG_EMAIL).item(0);
                    val hp = element.getElementsByTagName(TAG_HP).item(0);
                    if (email == null) {
                        val emailEle = doc.createElement(TAG_EMAIL);
                        emailEle.setTextContent(account.email);
                        element.appendChild(emailEle);
                        nodeList.item(0).appendChild(element);
                    } else {
                        email.setTextContent(account.email);
                    }
                    if (hp == null) {
                        val hpEle = doc.createElement(TAG_HP);
                        hpEle.setTextContent(account.hp);
                        element.appendChild(hpEle);
                        nodeList.item(0).appendChild(element);
                    } else {
                        hp.setTextContent(account.hp);
                    }
                    XmlUtil.writeXmlFileWithIndent(doc, file, 2);
                    return ;
                }
            }
        })

        throw new Exception("Cannot find account id : " + account.id);
    }

    private def extractTextValue(alertElement: Element, tagName: String): String = {
        val nodeList = alertElement.getElementsByTagName(tagName);
        if (ArrayUtil.len(nodeList) == 0) {
            return "";
        }
        val objTypeElement = nodeList.item(0).asInstanceOf[Element];
        if (objTypeElement == null) "" else objTypeElement.getTextContent();
    }

    /* 신규추가 */
    def removeAccount(file: File, account: Account) {
        val docBuilderFactory = DocumentBuilderFactory.newInstance();
        val docBuilder = docBuilderFactory.newDocumentBuilder();
        val doc = docBuilder.parse(file);
        doc.getDocumentElement().normalize();
        removeWhitespaceNodes(doc.getDocumentElement)
        val nodeList = doc.getElementsByTagName(TAG_ACCOUNT);

        EnumerScala.foreach(nodeList, (node: Node) => {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                val element = node.asInstanceOf[Element];
                val id = element.getAttribute(ATTR_ID);
                if (account.id.equals(id)) {
                    element.getParentNode().removeChild(element);
                    XmlUtil.writeXmlFileWithIndent(doc, file, 2);
                    return ;
                }
            }
        })

        throw new Exception("Cannot find account id : " + account.id);
    }
    private def removeWhitespaceNodes(node: Node): Unit = {
        val children = node.getChildNodes
        var i = children.getLength - 1

        while (i >= 0) {
            val child = children.item(i)

            if (child.getNodeType == Node.TEXT_NODE &&
                child.getTextContent.trim.isEmpty) {

                node.removeChild(child)

            } else if (child.getNodeType == Node.ELEMENT_NODE) {
                removeWhitespaceNodes(child)
            }

            i -= 1
        }
    }
}