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

public class BytesUtil {
	public static byte getByte(byte[] data, int x) {
		if (data == null || data.length <= x)
			return 0;
		else
			return data[x];
	}

	public static int getLength(byte[] data) {
	    return data==null?0:data.length;
    }

	public static byte[] merge(byte[] b1, byte[] b2) {
		byte[] buff = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, buff, 0, b1.length);
		System.arraycopy(b2, 0, buff,  b1.length,b2.length);
		return buff;
	}
}