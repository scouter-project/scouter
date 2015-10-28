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

import java.text.DecimalFormat;

/**
 * @author tkyushin TODO To change the template for this generated type comment
 *         go to Window - Preferences - Java - Code Style - Code Templates
 */
public class RuntimeUtil {

	final static DecimalFormat memroyFormat = new DecimalFormat("#,##0.00 Mb");

	final static double MB_Numeral = 1024d * 1024d;

	public RuntimeUtil() {

	}

	public static double getTotalMemoryInMb() {
		return Runtime.getRuntime().totalMemory() / MB_Numeral;
	}

	public static double getFreeMemoryInMb() {
		return Runtime.getRuntime().freeMemory() / MB_Numeral;
	}

	public static double getUsedMemoryInMb() {
		return getTotalMemoryInMb() - getFreeMemoryInMb();
	}

	public static String getTotalMemoryStringInMb() {
		return memroyFormat.format(getTotalMemoryInMb());
	}

	public static String getFreeMemoryStringInMb() {
		return memroyFormat.format(getFreeMemoryInMb());
	}

	public static String getUsedMemoryStringInMb() {
		return memroyFormat.format(getUsedMemoryInMb());
	}

}
