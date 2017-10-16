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
        val accounts = doc.getElementsByTagName(TAG_ACCOUNTS).item(0);
        val accountEle = doc.createElement(TAG_ACCOUNT);
        accountEle.setAttribute(ATTR_ID, account.id);
        accountEle.setAttribute(ATTR_PASS, account.password);
        accountEle.setAttribute(ATTR_GROUP, account.group);
        val emailEle = doc.createElement(TAG_EMAIL);
        emailEle.setTextContent(account.email);
        accountEle.appendChild(emailEle);
        accounts.appendChild(accountEle);
        XmlUtil.writeXmlFileWithIndent(doc, file, 2);
    }

    def editAccount(file: File, account: Account) {
        val docBuilderFactory = DocumentBuilderFactory.newInstance();
        val docBuilder = docBuilderFactory.newDocumentBuilder();
        val doc = docBuilder.parse(file);
        doc.getDocumentElement().normalize();
        val nodeList = doc.getElementsByTagName(TAG_ACCOUNT);

        EnumerScala.foreach(nodeList, (node: Node) => {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                val element = node.asInstanceOf[Element];
                val id = element.getAttribute(ATTR_ID);
                if (account.id.equals(id)) {
                    element.setAttribute(ATTR_PASS, account.password);
                    element.setAttribute(ATTR_GROUP, account.group);
                    element.getElementsByTagName(TAG_EMAIL).item(0).setTextContent(account.email);
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
            return null;
        }
        val objTypeElement = nodeList.item(0).asInstanceOf[Element];
        if (objTypeElement == null) null else objTypeElement.getTextContent();
    }
}