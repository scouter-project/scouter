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
 *  For this class
 *     many of method names
 *     basic ideas 
 *  are  from org.apache.commons.lang3.ArrayUtils.java
 *  
 */
package scouter.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NodeList;

import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;

public class ArrayUtil {
	
	public static boolean isEmpty(MapValue arr) {
		return arr == null || arr.size() == 0;
	}
	public static boolean isEmpty(ListValue arr) {
		return arr == null || arr.size() == 0;
	}
	public static boolean isEmpty(Object[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(long[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(int[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(short[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(char[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(byte[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(double[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(float[] arr) {
		return arr == null || arr.length == 0;
	}

	public static boolean isEmpty(boolean[] arr) {
		return arr == null || arr.length == 0;
	}

	
	public static int len(List n) {
		return n == null ? 0 : n.size();
	}

	public static int len(Map n) {
		return n == null ? 0 : n.size();
	}
	public static int len(NodeList n) {
		return n == null ? 0 : n.getLength();
	}
	public static int len(Object n) {
		if(n==null)
			return 0;
		if(n.getClass().isArray()){
			return Array.getLength(n);
		}
		if(n instanceof List)
			return len((List)n);
		if(n instanceof Map)
			return len((Map)n);
		if(n instanceof ListValue)
			return len((ListValue)n);
		if(n instanceof MapValue)
			return len((MapValue)n);
		if(n instanceof MapValue)
			return len((MapValue)n);
		if(n instanceof NodeList)
			return len((NodeList)n);
	
		return 0;
					
	}
	
	public static int len(Object[] n) {
		return n == null ? 0 : n.length;
	}
	public static int len(long[] n) {
		return n == null ? 0 : n.length;
	}
	public static int len(double[] n) {
		return n == null ? 0 : n.length;
	}
	public static int len(int[] n) {
		return n == null ? 0 : n.length;
	}
	public static int len(byte[] n) {
		return n == null ? 0 : n.length;
	}
	public static int len(float[] n) {
		return n == null ? 0 : n.length;
	}
	public static int len(ListValue n) {
		return n == null ? 0 : n.size();
	}

	public static int len(MapValue n) {
		return n == null ? 0 : n.size();
	}

	

	public static String toString(Object o) {
		if(o==null)
			return "null";
		if(o.getClass().isArray()==false){
			return o.toString();
		}
		if(o instanceof Object[]){
			return Arrays.toString((Object[])o);
		}
		if(o instanceof int[]){
			return Arrays.toString((int[])o);
		}
		if(o instanceof long[]){
			return Arrays.toString((long[])o);
		}
		if(o instanceof float[]){
			return Arrays.toString((float[])o);
		}
		if(o instanceof double[]){
			return Arrays.toString((double[])o);
		}
		return o.toString();
	}
	public static void main(String[] args) {
		Object o = new String[]{"1","2"};
		System.out.println(toString(o));
	}
}