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

import org.w3c.dom.Node;

public class ProcessorReplace extends Processor{
	private String m_source = null;
	private String m_target = null;
	private boolean m_isAll = true;
	
	public ProcessorReplace(){
		setType(Processor.TYPE.REPLACE);
	}
	
	public String process(String line) {
		if(m_isAll){
			return line.replaceAll(m_source, m_target);
		}else{
			return line.replaceFirst(m_source, m_target);
		}
	}

	public void readConfig(ParserPreProcessorReader reader, Node node) {
		try {
			m_source = reader.getAttribute(node, "source");
			m_target = reader.getAttribute(node, "target");
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		
		String isAll = null;
		try {
			isAll = reader.getAttribute(node, "all");
		}catch(Exception ex){
		}
		
		if(isAll != null){
			try {
				m_isAll = Boolean.parseBoolean(isAll);
			}catch(Exception ex){
				throw new RuntimeException("all attribute(true/false) of replace type processor is abnormal!(" + isAll + ")");
			}
		}

	}
}
