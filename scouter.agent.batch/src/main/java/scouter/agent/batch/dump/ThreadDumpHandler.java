/*
 *  Copyright 2016 the original author or authors. 
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
 */
package scouter.agent.batch.dump;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import scouter.agent.batch.trace.TraceContext;
import scouter.agent.batch.proxy.IToolsMain;
import scouter.agent.batch.proxy.LoaderManager;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

public class ThreadDumpHandler {
	private static final String TOOLS_MAIN = "scouter.xtra.tools.ToolsMain";
	
	public static void processDump(File stackFile, FileWriter stackWriter, FileWriter indexWriter, String [] filters, boolean headerExists) throws Throwable{
		if(stackWriter == null){
			return;
		}
		
		List<String> dumpList = threadDump();
		if(dumpList == null || dumpList.size() == 0){
			return;
		}
		
		String stack = filter(dumpList, filters, headerExists);
		if(stack == null || stack.length() == 0){
			return;
		}
		
		TraceContext.getInstance().lastStack = stack;

		indexWriter.write(new StringBuilder(50).append(System.currentTimeMillis()).append(' ').append(stackFile.length()).append(System.getProperty("line.separator")).toString());
		indexWriter.flush();
		stackWriter.write(stack);
		stackWriter.flush();
	}
	
	private static String filter(List<String> dumpList, String [] filters, boolean headerExists){
		int i;
		int size = dumpList.size();
		int startIndex = 0;
		String value;
		String lineSeparator = System.getProperty("line.separator");
		
		StringBuilder stackBuffer = new StringBuilder(4096);
		if(headerExists){
			for(i = startIndex; i < size; i++ ){
				value = dumpList.get(i);
				stackBuffer.append(value).append(lineSeparator);
				if(value.length() == 0){
					startIndex = i + 1;
					break;
				}
			}
			
			int ii;
			boolean bSave = false;
			boolean bScouter = false;
			List<String> stack = new ArrayList<String>();
			for(i = startIndex; i < size; i++ ){
				value = dumpList.get(i);
				
				if(value.length() == 0){
					if(bSave && stack.size() > 1){
						for(ii = 0; ii < stack.size(); ii++){
							stackBuffer.append(stack.get(ii)).append(lineSeparator);
						}
						stackBuffer.append(lineSeparator);
					}
					stack.clear();
					bSave = false;
					bScouter = false;
					continue;
				}
				
				if( stack.size() == 0 && !bScouter){
					if(value.indexOf("Scouter-") >=0){
						bScouter = true;
						continue;
					}
				}
				if(bScouter){
					continue;
				}
				
				stack.add(value);
				if(!bSave){
					if(filters == null){
						bSave = true;
						continue;
					}
					for(ii = 0; ii < filters.length; ii++){
						if(value.indexOf(filters[ii]) >= 0){
							bSave = true;
						}
					}
				}
			}
			if(bSave && stack.size() > 1){
				for(ii = 0; ii < stack.size(); ii++){
					stackBuffer.append(stack.get(ii)).append(lineSeparator);
				}
				stackBuffer.append(lineSeparator);
			}
		}
		return stackBuffer.toString();
	}
	
    private static List<String> threadDump() throws Throwable {
    	List<String> out = null;
        //Java 1.5 or IBM JDK
		if (SystemUtil.IS_JAVA_1_5||SystemUtil.JAVA_VENDOR.startsWith("IBM")) {
			out =  ThreadUtil.getThreadDumpList();
			return out;
		}

		ClassLoader loader = LoaderManager.getToolsLoader();
		if (loader == null) {
			out =  ThreadUtil.getThreadDumpList();	
			return out;
		}
		
		try {
			Class<?> c = Class.forName(TOOLS_MAIN, true, loader);
			IToolsMain toolsMain = (IToolsMain) c.newInstance();
			out = (List<String>) toolsMain.threadDump(0, 100000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}
}
