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
package scouter.client.stack.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scouter.client.stack.utils.ResourceUtils;

public class XMLReader {
	public static final String DEFAULT_XMLCONFIG = "Default";
	private Document m_xmlDoc = null;
	private String m_filename = null;
	
	public XMLReader(String filename){
		try {			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        	dbf.setValidating(false);
        	dbf.setIgnoringComments(true);
        
			DocumentBuilder db = dbf.newDocumentBuilder();
			if(DEFAULT_XMLCONFIG.equals(filename)){
				m_xmlDoc = db.parse(ResourceUtils.getDefaultXMLConfig());
			}else{
				m_xmlDoc = db.parse(new File(filename));
			}
			m_filename = filename;
		} catch(Exception ex){
			m_xmlDoc = null;
			throw new RuntimeException(ex);
		}
	}
	
	public ArrayList<Node> getNodeList(String parameter) throws Exception {
		ArrayList<Node> list = getNodeListByTagName(m_xmlDoc, parameter);
		if(list == null || list.size() < 1)
			throw new IOException( parameter + " node lists in XML file are not exist!");
		
		return list;
	}
	
	public String getValue(Node node) throws Exception{
		if(m_xmlDoc == null || node == null)
			return null;
	
		return node.getFirstChild().getNodeValue();
	}
	
	public String getValue(String parameter) throws Exception{
		if(m_xmlDoc == null)
			return null;
	
		ArrayList<Node> list = getNodeList(parameter);
		Node node = (Node)list.get(0);
		
		return node.getFirstChild().getNodeValue();
	}
	
	public String getAttribute(Node node, String attribute) throws Exception{
		if(m_xmlDoc == null || node == null)
			return null;
				
		String attribute_value = null;
		try {
			attribute_value = node.getAttributes().getNamedItem(attribute).getNodeValue();
		} catch(Exception ex){
			throw new IOException(attribute + " attribute of node" + node.toString() + " is not exists!");
		}
		return attribute_value;
	}	
	
	public String getAttribute(String parameter, String attribute) throws Exception{
		if(m_xmlDoc == null)
			return null;
		
		ArrayList<Node> list = getNodeList(parameter);		
		Node node = (Node)list.get(0);
		
		String attribute_value = null;
		try {
			attribute_value = node.getAttributes().getNamedItem(attribute).getNodeValue();
		} catch(Exception ex){
			throw new IOException(attribute + " attribute of " + parameter + " is not exists!");
		}
		return attribute_value;
	}

	public ArrayList<String> getList(String name) throws Exception{
		if(m_xmlDoc == null)
			return null;
		
		ArrayList<String> list = new ArrayList<String>();
		ArrayList<Node> injarList = getNodeListByTagName(m_xmlDoc, name);
		if(injarList == null || injarList.size() == 0)
			return null;
		
		for(int i =0; i < injarList.size(); i++){
			try {
				list.add(((Node)injarList.get(i)).getFirstChild().getNodeValue());
			} catch (Exception ex){
				throw new IOException( name + " lists in EXL file are not exist!");
			}
		}
		return list;
	}

	public ArrayList<Node> getNodeListByTagName(Document xmlDoc, String nodeName){
		if(xmlDoc == null || nodeName == null || nodeName.length() == 0)
			return null;
		
		String newNodeName = nodeName.replace('/',' ');
		StringTokenizer token = new StringTokenizer(newNodeName);
		NodeList nodeList = xmlDoc.getElementsByTagName(token.nextToken());
		if(nodeList.getLength()==0)
			return null;
		
		ArrayList<Node> list = new ArrayList<Node>();
		if(token.hasMoreTokens()){
			String tagname = token.nextToken();
			int list_cnt = 0;
			for(int i = 0; i < nodeList.getLength(); i++){
				ArrayList<Node> temp = getNodeList(nodeList.item(i), tagname);
				if(temp != null){
					for(int ii=0; ii < temp.size(); ii++){
						list.add(list_cnt++, temp.get(ii));
					}
				}
			}			
		} else {
			for(int i = 0; i < nodeList.getLength(); i++){
				list.add(i, nodeList.item(i));
			}
		}
		
		return list;
	}
	
	public ArrayList<Node> getNodeList(Node node, String tagname){
		NodeList nodeList = node.getChildNodes();
		if(nodeList == null)
			return null;
		
		int list_cnt = 0;
		ArrayList<Node> list = new ArrayList<Node>();
		for(int i = 0; i < nodeList.getLength(); i++){
			Node item = nodeList.item(i);
			if(item.getNodeName().equals(tagname)){
				list.add(list_cnt++, item);
			}
		}
		
		if(list_cnt == 0)
			return null;
		else
			return list;
	}
	
	public String getFilename(){
		return m_filename;
	}
}
