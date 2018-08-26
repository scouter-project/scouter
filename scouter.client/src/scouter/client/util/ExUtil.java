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
package scouter.client.util;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class ExUtil {
	public static void exec(Runnable r) {
		exec(Display.getCurrent(), r);
	}

	public static void exec(Display display, final Runnable r) {
		if (display == null || r == null) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				try {
					r.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}

	public static void exec(Composite c, Runnable r) {
		if (c == null)
			return;
		try {
			exec(c.getDisplay(), r);
		} catch (Throwable t) {
		}
	}
	
	public static void syncExec(Runnable r) {
		syncExec(Display.getCurrent(), r);
	}

	public static void syncExec(Display display, final Runnable r) {
		if (display == null || r == null) {
			return;
		}
		display.syncExec(new Runnable() {
			public void run() {
				try {
					r.run();
				} catch (Throwable t) {
				}
			}
		});
	}

	public static void syncExec(Composite c, Runnable r) {
		if (c == null)
			return;
		try {
			syncExec(c.getDisplay(), r);
		} catch (Throwable t) {
		}
	}
	
	public static void asyncRun(Runnable r) {
		asyncRun(null, r);
	}
	
	public static void asyncRun(String name, Runnable r) {
		try {
			Thread thread = new Thread(r);
			if (name != null) {
				thread.setName(name);
			}
			thread.setDaemon(true);
			thread.start();
		} catch (Throwable t) {
		}
	}
}
