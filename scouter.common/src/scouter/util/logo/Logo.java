/*
 *  Copyright 2015 LG CNS.
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

package scouter.util.logo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import scouter.Version;
import scouter.util.FileUtil;
import scouter.util.ParamText;

public class Logo {
	public static void print() {
		print(false);
	}
	public static void print(boolean server) {
		InputStream in = null;
		try {
			in = Logo.class.getResourceAsStream("scouter.logo");
			if (in == null)
				return;
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			while (line != null) {
				if(server){
					System.out.println(new ParamText(line).getText(Version.getServerFullVersion()));
				}else{
					System.out.println(new ParamText(line).getText(Version.getAgentFullVersion()));
				}
				line = reader.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			FileUtil.close(in);
		}
	}
	public static void print(PrintWriter w,boolean server) {
		InputStream in = null;
		try {
			in = Logo.class.getResourceAsStream("scouter.logo");
			if (in == null)
				return;
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			while (line != null) {
				if(server){
					w.println(new ParamText(line).getText(Version.getServerFullVersion()));
				}else{
					w.println(new ParamText(line).getText(Version.getAgentFullVersion()));
				}
				line = reader.readLine();
			}
			w.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			FileUtil.close(in);
		}
	}

	public static void main(String[] args) throws IOException {
		print();
	}
}