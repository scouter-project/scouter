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

package scouter.net;

import scouter.io.DataInputX;


public class NetCafe {
	public final static int CAFE_LENGTH = 4;
	public final static byte[] CAFE = "CAFE".getBytes();
	public final static byte[] CAFE_N = "CAFN".getBytes();
	public final static byte[] CAFE_MTU = "CAFM".getBytes();
	public final static byte[] JAVA = "JAVA".getBytes();
	public final static byte[] JAVA_N = "JAVN".getBytes();
	public final static byte[] JAVA_MTU = "JMTU".getBytes();
	//
	public final static int UDP_CAFE = 0x43414645;
	public final static int UDP_CAFE_N = 0x4341464e;
	public final static int UDP_CAFE_MTU = 0x4341464d;
	public final static int UDP_JAVA = 0x4a415641;
	public final static int UDP_JAVA_N = 0x4a41564e;
	public final static int UDP_JAVA_MTU = 0x4a4d5455;
	//
	public static final int TCP_AGENT =0xCAFE1001;
	public static final int TCP_CLIENT = 0xCAFE2001;
	
	public static final int TCP_SHUTDOWN = 0xCAFE1999;
	
	public static void main(String[] args) {
		
		System.out.println("CAFE = 0x" +Integer.toHexString(DataInputX.toInt(CAFE, 0)));
		System.out.println("CAFN = 0x" +Integer.toHexString(DataInputX.toInt(CAFE_N, 0)));
		System.out.println("CAFM = 0x" +Integer.toHexString(DataInputX.toInt(CAFE_MTU, 0)));
		System.out.println("JAVA = 0x" +Integer.toHexString(DataInputX.toInt(JAVA, 0)));
		System.out.println("JAVAN = 0x" +Integer.toHexString(DataInputX.toInt(JAVA_N, 0)));
		System.out.println("JMTU = 0x" +Integer.toHexString(DataInputX.toInt(JAVA_MTU, 0)));
		}

}