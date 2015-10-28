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

public class ProcessorAdd extends Processor{
	private String m_value = null;
	private int m_position = 0;
	
	public ProcessorAdd(){
		setType(Processor.TYPE.ADD);
	}

	public String process(String line) {
		if(m_position == -1){
			return line + m_value;
		}else if(m_position == 0){
			return m_value + line;
		}
		
		return new StringBuilder(100).append(line.substring(0, m_position)).append(m_value).append(line.substring(m_position)).toString();
	}

	public void readConfig(ParserPreProcessorReader reader, Node node) {
		try {
			m_value = reader.getAttribute(node, "value");
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}

		String position = null;
		try {
			position = reader.getAttribute(node, "position");
			if("*".equals(position)){
				m_position = -1;
			}else{
				m_position = Integer.parseInt(position);
			}
		}catch(Exception ex){
			throw new RuntimeException("position attribute of add type processor is abnormal!(" + position + ")");
		}
	}
}
