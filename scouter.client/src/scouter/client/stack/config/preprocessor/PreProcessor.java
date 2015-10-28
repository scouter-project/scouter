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

import java.util.ArrayList;

import scouter.client.stack.config.ParserConfig;
import scouter.client.stack.utils.StringUtils;


public class PreProcessor {
	static public void process(ParserConfig config, ArrayList<String> list){
		ArrayList<Processor> processorList = config.getStackPreprocessorList();
		if(processorList == null || processorList.size() == 0)
			return;
		
		int i= 0;
		int ii = 0;
		int start = 0;
		int pSize = processorList.size();
		int sSize = list.size();
		
		Processor processor = null;
		String line = null;
		
		Processor.TARGET target = config.getStackPreprocessorTarget();
		if(target == Processor.TARGET.ALL){
		}else if(target == Processor.TARGET.HEADER){
			sSize = config.getStackStartLine();
		}else if(target == Processor.TARGET.STACK){
			start = config.getStackStartLine();
		}
		
		String filter = null;
		String newline = null;
		for(i=0; i < pSize; i++){
			processor = processorList.get(i);
			if(processor == null){
				continue;
			}
			filter = processor.getFilter();
			for( ii = start; ii < sSize; ii++){
				line = list.get(ii);
				if(filter == null || line.indexOf(filter)>=0){
					newline = processor.process(line);
					if(newline == null){ // remove line
						list.remove(ii);
						ii--;
						sSize--;
					}else if(!line.equals(newline)){ // changed
						list.set(ii, newline);
					}
				}
			}
		}
	}
	
	static public void readPreprocessor(ParserConfig config, String filename){
		String fullPath = StringUtils.getDiretory(config.getConfigFilename()) + "\\" + filename;
		ParserPreProcessorReader reader = new ParserPreProcessorReader(fullPath);
		try {
			config.setStackPreprocessorTarget(reader.readTarget());
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		
		ArrayList<Processor> processorList = reader.readProcessors();
		
		if(processorList != null && processorList.size() > 0){
			config.setStackPreprocessorList(processorList);
		}
	}
}
