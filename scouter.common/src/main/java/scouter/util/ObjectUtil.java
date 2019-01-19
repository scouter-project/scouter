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

public class ObjectUtil {

	public static String toString(Object o) {
		return o==null?"":o.toString();
	}

	 public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if ((o1 == null) || (o2 == null)) {
            return false;
        }
        return o1.equals(o2);
	 }

    public static boolean objectEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
