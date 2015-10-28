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

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scouter.client.stack.config.preprocessor.PreProcessor;


public class ParserConfigReader extends XMLReader {
    public ParserConfigReader(String fileName) {
        super(fileName);
    }

    public ParserConfig read() {
        ParserConfig config = new ParserConfig();

        config.setConfigFilename(getFilename());
        try {
            config.setParserName(getSingleValue("scouter/parser"));
        } catch ( Exception ex ) {
        	throw new RuntimeException(ex);
        }
        
        try {
            config.setStackStartLine(Integer.parseInt(getAttribute("scouter/parser", "stackStartLine")));  
        } catch ( Exception ex ) {
        	System.err.println("stackStartLine attribute in scouter/parser is not exist! (Default: 2)");
        }

        try {
        	config.setDivideStack(getAttribute("scouter/parser", "divideStack"));  
        } catch ( Exception ex ) {
        }
        
        try {
            config.setTimeFormat(getSingleValue("scouter/time"));
            config.setTimeSize(Integer.parseInt(getAttribute("scouter/time", "size")));
            config.setTimePosition(Integer.parseInt(getAttribute("scouter/time", "position")));
            config.setTimeFilter(getAttribute("scouter/time", "filter"));
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }

        try {
            config.setThreadStatus(getAttribute("scouter/workingThread", "status"));
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }

        readDefaultAnalyzer(config);
        readAddedAnalyzer(config);
        readJMX(config);
        
        // readPreprocessor
        String filename = null;
        try {
        	filename = getSingleValue("scouter/preProcessor");
        }catch(Exception ex){
        }
    	if(filename != null && filename.length() > 0){
    		PreProcessor.readPreprocessor(config, filename);
    	}        
        return config;
    }

    private void readDefaultAnalyzer( ParserConfig config ) {
        ArrayList<String> list = null;
        String value;
        
        try {
            list = readList("scouter/workerThread");
            config.setWorkerThread(list);
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }

        try {
	        list = readList("scouter/workingThread");
	        config.setWorkingThread(list);
        }catch(RuntimeException ex){
        	throw ex;
        }
        
        try {
        	value = getAttribute("scouter/service", "type");
        	if("exclude".equalsIgnoreCase(value)){
        		config.setServiceExclude(true);
        	}
        }catch(Exception ex){}
        
        try {
            list = readList("scouter/service");
            config.setService(list);
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }

        try {
            list = readList("scouter/sql");
            config.setSql(list);
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }

        try {
            list = readList("scouter/log");
            config.setLog(list);
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }

        try {
            list = readList("scouter/excludeStack");
            config.setExcludeStack(list);
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }

        try {
            list = readList("scouter/singleStack");
            config.setSingleStack(list);
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
        }
    }

    private void readAddedAnalyzer( ParserConfig config ) {
        ArrayList<Node> nodeList = null;
        try {
            nodeList = getNodeList("scouter/analyze");

            if ( nodeList == null || nodeList.size() == 0 )
                return;

            for ( int i = 0; i < nodeList.size(); i++ ) {
                readAnalyzer(nodeList.get(i), config);
            }
        } catch ( Exception ex ) {
        	System.err.println(ex.getMessage());
       }
    }

    private void readAnalyzer( Node node, ParserConfig config ) throws Exception {
        ArrayList<Node> nodeList = getNodeList(node, "analyzeStack");
        if ( nodeList == null || nodeList.size() == 0 )
            return;

        for ( int i = 0; i < nodeList.size(); i++ ) {
            readAnalyzerEach(nodeList.get(i), config);
        }

    }

    private void readAnalyzerEach( Node node, ParserConfig config ) throws Exception {
        AnalyzerValue value = new AnalyzerValue();

        value.setName(getAttribute(node, "name"));
        value.setExtension(getAttribute(node, "extension"));
        value.setFilter(getAttribute(node, "filter"));
        value.setReader(getAttribute(node, "reader"));

        ArrayList<String> list = new ArrayList<String>();
        ArrayList<Node> nodeList = getNodeList(node, "list");
        String filterValue = null;

        for ( int i = 0; i < nodeList.size(); i++ ) {
            filterValue = getValue(nodeList.get(i));
            if ( filterValue != null && filterValue.length() > 0 )
                list.add(filterValue);
        }

        if ( value.getFilter() == AnalyzerValue.FILTER_ALL ) {
            ArrayList<String> listMain = new ArrayList<String>();
            nodeList = getNodeList(node, "listMain");
            for ( int i = 0; i < nodeList.size(); i++ ) {
                filterValue = getValue(nodeList.get(i));
                if ( filterValue != null && filterValue.length() > 0 )
                    listMain.add(filterValue);
            }
            if ( listMain.size() > 0 ) {
                value.setListMain(listMain);
            }

        }

        value.setList(list);
        if ( value.isValid() ) {
            config.addAnalyzer(value);
        } else {
            throw new RuntimeException("analyzeStack configuration is wrong!");
        }
    }

    private String getSingleValue( String nodeName ) {
        String returnValue = null;
        try {
            returnValue = getValue(nodeName);
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
        return returnValue;
    }

    private ArrayList<String> readList( String nodeName ) {
        ArrayList<Node> nodeList = null;
        try {
            nodeList = getNodeList(nodeName);
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
        if ( nodeList == null || nodeList.size() == 0 )
            return null;

        ArrayList<String> returnList = new ArrayList<String>();
        Node node = null;

        for ( int i = 0; i < nodeList.size(); i++ ) {
            try {
                node = (Node)nodeList.get(i);
                NodeList listList = ((Node)nodeList.get(i)).getChildNodes();
                if ( listList == null | listList.getLength() == 0 )
                    continue;

                for ( int ii = 0; ii < listList.getLength(); ii++ ) {
                    node = listList.item(ii);
                    if ( !node.getNodeName().equals("list") )
                        continue;
                    returnList.add(node.getFirstChild().getNodeValue());
                }
            } catch ( Exception ex ) {
                throw new RuntimeException(ex);
            }
        }
        return returnList;
    }

    private void readJMX( ParserConfig config ) {
        ArrayList<Node> nodeList = null;
        try {
            nodeList = getNodeList("scouter/jmx");

            if ( nodeList == null || nodeList.size() == 0 )
                return;

            int count = 100;
            int interval = 10000;
            String path = ".";

            Node jmxNode = nodeList.get(0);
            ArrayList<Node> list = getNodeList(jmxNode, "count");
            if ( list != null && list.size() > 0 ) {
                count = Integer.parseInt(getValue(list.get(0)));
            }
            list = getNodeList(jmxNode, "interval");
            if ( list != null && list.size() > 0 ) {
                interval = Integer.parseInt(getValue(list.get(0)));
            }
            list = getNodeList(jmxNode, "path");
            if ( list != null && list.size() > 0 ) {
                path = getValue(list.get(0));
            }
            System.out.println("JMX: count " + count + ", interval " + interval + ", path " + path);

            config.setJMXConfig(count, interval, path);

            readJMXserver(jmxNode, config);
        } catch ( Exception ex ) {
            System.err.println(ex.getMessage());
        }
    }

    private void readJMXserver( Node node, ParserConfig config ) throws Exception {
        ArrayList<Node> nodeList = getNodeList(node, "server");
        if ( nodeList == null || nodeList.size() == 0 )
            return;

        Node data;
        String ip;
        int port;
        JmxConfig jconfig = config.getJMXConfig();
        if ( jconfig == null ) {
            throw new Exception("JMX configuration(count,interval,path) is not exists!");
        }
        for ( int i = 0; i < nodeList.size(); i++ ) {
            data = getNodeList(nodeList.get(i), "ip").get(0);
            ip = getValue(data);
            data = getNodeList(nodeList.get(i), "port").get(0);
            port = Integer.parseInt(getValue(data));

            if ( ip == null || port == 0 ) {
                throw new Exception("JMX configuration(ip or port) is not exist!");
            } else {
                System.out.println("JMX Server:" + ip + " " + port);
                jconfig.addServer(ip, port);
            }
        }
    }

}
