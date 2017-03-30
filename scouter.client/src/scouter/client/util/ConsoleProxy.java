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

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.*;

import java.io.PrintStream;


public class ConsoleProxy {
	private static ConsoleProxy fDefault = new ConsoleProxy();
	private String fTitle = "Scouter";
	private MessageConsole fMessageConsole = null;

	public static final int MSG_INFORMATION = 1;
	public static final int MSG_ERROR       = 2;
	public static final int MSG_WARNING     = 3;
	public static final int MSG_CATCH       = 4;
	public static final int MSG_DEBUG       = 5;

	public static void warning(String msg) {
		fDefault.write(msg, MSG_WARNING);
	}

	public static void error(String msg) {
		fDefault.write(msg, MSG_ERROR);
	}

	public static void info(String msg) {
		fDefault.write(msg, MSG_INFORMATION);
	}

	public static void println(String msg, int msgKind) {
		fDefault.write(msg, msgKind);
	}

	public static void printlnSafe(final String msg, final int msgKind) {
		try {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					println(msg, msgKind);
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void warningSafe(String message) {
		printlnSafe(message, MSG_WARNING);
	}

	public static void infoSafe(String message) {
		printlnSafe(message, MSG_INFORMATION);
	}

	public static void errorSafe(final String message) {
		printlnSafe(message, MSG_ERROR);
	}
	
	public static void catchSafe(final String message) {
		printlnSafe(message, MSG_CATCH);
	}
	
	public static void debugSafe(final String message) {
		printlnSafe(message, MSG_DEBUG);
	}

	private void write(String msg, int msgKind) {
		if (msg == null)
			return;

//		if (!displayConsoleView()) {
//			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", msg);
//			return;
//		}
		try {
			getNewMessageConsoleStream(msgKind).println(msg);
		} catch (Throwable t) {
		}
	}

	public void clear() {
		IDocument document = getMessageConsole().getDocument();
		if (document != null) {
			document.set("");
		}
	}

	public boolean displayConsoleView() {
		try {
			IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (activeWorkbenchWindow != null) {
				IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
				//PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				//		.showView(IConsoleConstants.ID_CONSOLE_VIEW, null, IWorkbenchPage.VIEW_VISIBLE);
				if (activePage != null)
					activePage.showView(IConsoleConstants.ID_CONSOLE_VIEW, null, IWorkbenchPage.VIEW_VISIBLE);
			}
		} catch (Throwable partEx) {
			return false;
		}
		return true;
	}

	private MessageConsoleStream getNewMessageConsoleStream(int msgKind) {

		String swtColorId = "back";
		switch (msgKind) {
		case MSG_INFORMATION:
			swtColorId = "dark green";
			break;
		case MSG_ERROR:
			swtColorId = "dark magenta";
			break;
		case MSG_WARNING:
			swtColorId = "dark blue";
			break;
		case MSG_CATCH:
			swtColorId = "red";
			break;
		case MSG_DEBUG:
			swtColorId = "blue gray";
			break;
		default:
		}

		MessageConsoleStream msgConsoleStream = getMessageConsole().newMessageStream();
		PrintStream myS = new PrintStream(msgConsoleStream);
		System.setOut(myS);
		System.setErr(myS);

		try {
			Color c = ColorUtil.getInstance().getColor(swtColorId);
			msgConsoleStream.setColor(c);
		} catch (Exception e) {
		}
		return msgConsoleStream;
	}

	private MessageConsole getMessageConsole() {
		if (fMessageConsole == null) {
			fMessageConsole = new MessageConsole(fTitle, null);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { fMessageConsole });
		}
		return fMessageConsole;
	}
}