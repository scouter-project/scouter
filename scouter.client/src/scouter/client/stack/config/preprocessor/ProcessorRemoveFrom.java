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

public class ProcessorRemoveFrom extends Processor{
	private String m_from = null;
	private int m_skipCount = 0;

	public ProcessorRemoveFrom(String from){
		setType(Processor.TYPE.REMOVE);
		m_from = from;
	}
	public String process(String line) {
		int currentCount = 0;
		int currentIndex = 0;
		while((currentIndex = line.indexOf(m_from, currentIndex)) >= 0){
			if(currentCount == m_skipCount){
				return line.substring(0, currentIndex);
			}
			currentCount++;
			currentIndex++;
		}
		
		return line;
	}

	public void readConfig(ParserPreProcessorReader reader, Node node) {
		String skipCount = null;
		try {
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
