/*
 *  Copyright 2015 Scouter Project.
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
package scouter.server.term;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import scouter.server.term.handler.ProcessMain;
import scouter.util.ShellArg;
import scouter.util.StringUtil;
import scouter.util.SystemUtil;

public class TermMain {

	public static void process(ShellArg ar) throws IOException, Exception {

		AnsiPrint.enable = SystemUtil.IS_WINDOWS == false;

		if (ar.hasKey("-ansi"))
			AnsiPrint.enable = true;
		else if (ar.hasKey("-noansi"))
			AnsiPrint.enable = false;

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		PrintStream out = System.out;
		while (true) {
			try {
				out.print(AnsiPrint.green(getPrompt()));
				out.flush();
				String cmd = reader.readLine();
				process(cmd.trim());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static String server_name = "";

	public static String getPrompt() {
		StringBuffer sb = new StringBuffer();
		sb.append("SCOUTER");
		if (StringUtil.isNotEmpty(server_name)) {
			sb.append(" ").append(server_name);
		}
		sb.append("> ");
		return sb.toString();
	}

	private static String lastCmd = "";

	public static void process(String cmd) {
		cmd = cmd.trim();
		if (cmd.length() == 0)
			return;
		if (".".equals(cmd)) {
			cmd = lastCmd;
		} else {
			lastCmd = cmd;
		}

		ProcessMain.process(cmd);

	}

}
