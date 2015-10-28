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

public class ProcessorReplaceStart extends Processor{
	private String m_start = null;
	private String m_end = null;
	private String m_target = null;
	private int m_skipCount = 0;

	public ProcessorReplaceStart(String start){
		setType(Processor.TYPE.REPLACE);
		m_start = start;
	}
	public String process(String line) {
		int currentCount = 0;
		int currentIndex = 0;
		while((currentIndex = line.indexOf(m_start, currentIndex)) >= 0){
			if(currentCount == m_skipCount){
				int endIndex = line.indexOf(m_end, currentIndex + 1);
				if(endIndex > 0){
					StringBuilder buffer = new StringBuilder(100);
					if(currentIndex == 0){
						buffer.append(m_target).append(line.substring(endIndex+1));
					}else if((endIndex + 1) >= line.length()){
						buffer.append(line.substring(0, currentIndex)).append(m_target);
					}else{
						buffer.append(line.substring(0, currentIndex)).append(m_target);
						buffer.append(line.substring(endIndex + 1));
					}
					return buffer.toString();
				}
				return line;
			}
			currentCount++;
			currentIndex++;
		}
		
		return line;
	}

	public void readConfig(ParserPreProcessorReader reader, Node node) {
		String skipCount = null;
		try {
			m_end = reader.getAttribute(node, "end");
			m_target = reader.getAttribute(node, "target");
			skipCount = reader.getAttribute(node, "skipCount");
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		
		try {
			m_skipCount = Integer.parseInt(skipCount);
		}catch(Exception ex){
			throw new RuntimeException("processor skipCount attribute is a integer greater than -1("+ skipCount + ")");			
		}
		if(m_skipCount < 0){
			throw new RuntimeException("processor skipCount attribute is abnormal(" + skipCount + ")!");	
		}
	}
}
