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

public class ProcessorRemoveCount extends Processor{
	private int m_count = 0;
	private int m_position = 0;

	public ProcessorRemoveCount(String count){
		setType(Processor.TYPE.REMOVE);
		try {
			m_count = Integer.parseInt(count);
		}catch(Exception ex){
			throw new RuntimeException("processor count attribute is a integer greater than -1("+count + ")");
		}
	}
	public String process(String line) {
		if(line.length() <= m_position)
			return line;
		
		StringBuilder buffer = new StringBuilder(100);
		if(m_position == 0){
			buffer.append(line.substring(m_count));
		}else{
			buffer.append(line.substring(0, m_position));
			if(line.length() >= (m_position + m_count)){
				buffer.append(line.substring((m_position+m_count-1)));
			}
		}
		return buffer.toString();
	}

	public void readConfig(ParserPreProcessorReader reader, Node node) {
		try {
			m_position = Integer.parseInt(reader.getAttribute(node, "position"));
		}catch(Exception ex){
			throw new RuntimeException("processor position attribute is abnormal!");
		}
		
		if(m_position <0){
			throw new RuntimeException("processor position attribute is abnormal!");
		}
	}
}
