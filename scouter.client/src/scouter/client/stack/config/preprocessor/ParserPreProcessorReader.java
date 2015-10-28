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
package scouter.client.stack.config.preprocessor;

import java.io.IOException;
import java.util.ArrayList;

import org.w3c.dom.Node;

import scouter.client.stack.config.XMLReader;
import scouter.client.stack.config.preprocessor.Processor.TYPE;


public class ParserPreProcessorReader extends XMLReader{

    public ParserPreProcessorReader(String fileName) {
        super(fileName);
    }
    
    public String readTarget() throws Exception{
    	return getAttribute("preProcessor", "target");
    }
    

    public ArrayList<Processor> readProcessors(){
    	ArrayList<Processor> processorList = new ArrayList<Processor>();
    	
        ArrayList<Node> nodeList = null;
        try {
            nodeList = getNodeList("preProcessor/processor");

            if ( nodeList == null || nodeList.size() == 0 )
                return processorList;

            for ( int i = 0; i < nodeList.size(); i++ ) {
            	processorList.add(readProcessor(nodeList.get(i)));
            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }    	
    	return processorList;
    }
    
    private Processor readProcessor(Node node){    	
    	Processor processor= null;
    	Processor.TYPE type;
    	try {
    		type = getType(getAttribute(node, "type"));
    	}catch(Exception ex){
    		throw new RuntimeException(ex);
    	}    	
    	
    	if(type == Processor.TYPE.ADD){
    		processor = new ProcessorAdd();
    	}else if(type == Processor.TYPE.REPLACE){
    		try {
    			processor = new ProcessorReplaceStart(getAttribute(node, "start"));
    		}catch(Exception ex){
        		processor = new ProcessorReplace();
    		}
    	}else if(type == Processor.TYPE.REMOVE){
    		try {
    			processor = new ProcessorRemoveStart(getAttribute(node, "start"));
    		}catch(Exception ex){
    			try {
    				processor = new ProcessorRemoveCount(getAttribute(node, "count"));
    			}catch(Exception e1){
	        		try {
	        			processor = new ProcessorRemoveValue(getAttribute(node, "value"));
	        		}catch(Exception e2){
		        		try {
		        			processor = new ProcessorRemoveFrom(getAttribute(node, "from"));
		        		}catch(Exception e3){
		        			throw new RuntimeException("processor type(remove) is abnormal!");
		        		}
	        		}
    			}
    		}
    	}else if(type == Processor.TYPE.REMOVELINE){
    		processor = new ProcessorRemoveLine();
    	}
    	
    	try {
    		processor.setFilter(getAttribute(node, "filter"));
    	}catch(Exception ex){
    	}    	
    	
    	processor.readConfig(this, node);
    	return processor;
    }
    
    private Processor.TYPE getType(String type) throws IOException{
		if(type == null || type.length() == 0)
			throw new IOException("processor type attribute of preprocessor is not exist!");

		if(type.equals("add")){
			return TYPE.ADD;
		}else if(type.equals("replace")){
			return TYPE.REPLACE;
		}else if(type.equals("remove")){
			return TYPE.REMOVE;
		}else if(type.equals("removeline")){
			return TYPE.REMOVELINE;
		}else{
			throw new IOException("processor type (add/replace/remove) of preprocessor is wrong(" + type + ")!");
		} 
    }
}
