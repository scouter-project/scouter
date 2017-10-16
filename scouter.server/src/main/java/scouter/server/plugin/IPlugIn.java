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
 */
package scouter.server.plugin;
import scouter.server.Logger;
import scouter.util.FileUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
public class IPlugIn {
	public long __lastModified;
	public String __pluginName;
	public void log(Object c) {
		Logger.println(c);
	}
	public void println(Object c) {
		System.out.println(c);
	}
	public void logTo(String file, String msg) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(file, true));
			pw.write(msg);
		} catch (Exception e) {
			Logger.println("S214", e.toString());
		} finally {
			FileUtil.close(pw);
		}
	}

	public PluginHelper $$ = PluginHelper.getInstance();
}
