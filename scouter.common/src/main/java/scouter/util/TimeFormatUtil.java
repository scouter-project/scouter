/*
 *  Copyright 2016 the original author or authors. 
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

public class TimeFormatUtil {
	static public String elapsedTime(long time) {
		if (time == 0 )
			return "00:00:00";
		int dd = (int)(time / 86400000L);
		int remain = (int)(time % 86400000L);
		int hh = remain / 3600000;
		remain = remain % 3600000;
		int mm = remain / 60000;
		int ss = (remain % 60000)/1000;
		
		StringBuffer sb = new StringBuffer();
		if(dd > 0){
			sb.append(dd).append(' ');
		}
		sb.append(mk2(hh)).append(":");
		sb.append(mk2(mm)).append(":");
		sb.append(mk2(ss));
		return sb.toString();
	}
	
	static private String mk2(int i){
		if(i < 10){
			return "0" + i;
		}
		return Integer.toString(i);
	}
}
