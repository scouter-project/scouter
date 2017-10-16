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


public class AnsiPrint {
	public static boolean enable = true;

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static String red(String s) {
		if (enable == false)
			return s;
		return ANSI_RED + s + ANSI_RESET;
	}

	public static String yellow(String s) {
		if (enable == false)
			return s;
		return ANSI_YELLOW + s + ANSI_RESET;
	}

	public static String green(String s) {
		if (enable == false)
			return s;
		return ANSI_GREEN + s + ANSI_RESET;
	}

	public static String cyan(String s) {
		if (enable == false)
			return s;
		return ANSI_CYAN + s + ANSI_RESET;
	}

	public static String blue(String s) {
		if (enable == false)
			return s;
		return ANSI_BLUE + s + ANSI_RESET;
	}


}
