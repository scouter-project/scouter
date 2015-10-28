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

public class ProcessorRemoveValue extends Processor{
	private String m_value = null;

	public ProcessorRemoveValue(String value){
		setType(Processor.TYPE.REMOVE);
		m_value = value;
	}
	public String process(String line) {
		return line.replaceAll(m_value, "");
	}

	public void readConfig(ParserPreProcessorReader reader, Node node) {
	}
}
