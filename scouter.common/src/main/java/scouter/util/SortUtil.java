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

package scouter.util;


import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

import scouter.lang.step.Step;


public class SortUtil {
	public static int[] sort(int[] arr) {
		Arrays.sort(arr);
		return arr;
	}

	public static int[] sort(Enumeration<Integer> en, int size) {
		int[] arr = new int[size];
		for (int i = 0; i < size; i++) {
			arr[i] = en.nextElement();
		}
		return sort(arr);
	}

	public static String[] sort(String[] arr) {
		Arrays.sort(arr);
		return arr;
	}
	
	public static String[] sort(String[] arr, boolean asc) {
		if (asc) {
			Arrays.sort(arr);
		} else {
			Arrays.sort(arr, Collections.reverseOrder());
		}
		return arr;
	}	

	public static String[] sort_string(Enumeration<String> en, int size) {
		String[] arr = new String[size];
		for (int i = 0; i < size; i++) {
			arr[i] = (String) en.nextElement();
		}
		return sort(arr);
	}

	public static String[] sort_string(Iterator<String> itr, int size) {
		String[] arr = new String[size];
		for (int i = 0; i < size; i++) {
			arr[i] = itr.next();
		}
		return sort(arr);
    }
	
	public static String[] sort_string(Iterator itr, int size, boolean asc) {
		String[] arr = new String[size];
		for (int i = 0; i < size; i++) {
			arr[i] = (String) itr.next();
		}
		return sort(arr, asc);
	}	

	public static Step[] sort(Step[] p) {
		Arrays.sort(p);
		return p;
    }
}