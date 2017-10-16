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

import java.io.IOException;

import scala.tools.jline.TerminalFactory;
import scala.tools.jline.console.ConsoleReader;
import scala.tools.jline.console.completer.StringsCompleter;
import scouter.server.ShutdownManager;
import scouter.server.term.handler.Help;
import scouter.server.term.handler.ProcessMain;
import scouter.util.IShutdown;
import scouter.util.ShellArg;
import scouter.util.SystemUtil;

public class TermMain {

	public static void process(ShellArg ar) throws IOException, Exception {

		AnsiPrint.enable = SystemUtil.IS_WINDOWS == false;

		if (ar.hasKey("-ansi"))
			AnsiPrint.enable = true;
		else if (ar.hasKey("-noansi"))
			AnsiPrint.enable = false;

		ShutdownManager.add(new IShutdown() {
			public void shutdown() {
				try {
					TerminalFactory.get().restore();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		ConsoleReader console = new ConsoleReader();
		console.setHistoryEnabled(true);
		// console.addCompleter(new FileNameCompleter());
		console.addCompleter(new StringsCompleter(Help.words()));

		console.setPrompt(AnsiPrint.green("scouter> "));
		while (true) {
			try {
				String cmd = console.readLine();
				ProcessMain.process(cmd.trim());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static void exit() {
		System.exit(0);
	}

}
