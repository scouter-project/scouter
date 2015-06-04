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

package scouter.server.db.io.zip;

import java.io.File;

import scouter.server.db.DBCtr;
import scouter.server.db.XLogWR;
import scouter.util.DateUtil;
import scouter.util.LongKeyLinkedMap;
import scouter.util.StringKeyLinkedMap;

public class IOUtil {
	public static int to144(long time) {
		long dateunit = DateUtil.getTimeUnit(time);
		return (int) (dateunit / 1000 / 600) % 144;
	}

	public static int to1440(long time) {
		long dateunit = DateUtil.getTimeUnit(time);
		return (int) (dateunit / 1000 / 60) % 1440;
	}

	private static LongKeyLinkedMap<String> dateTable = new LongKeyLinkedMap<String>().setMax(100);
	private static StringKeyLinkedMap<Long> dateunitTable = new StringKeyLinkedMap< Long>();

	public static String getDate(long time) {
		long dateunit = DateUtil.getDateUnit(time);
		String date = dateTable.get(dateunit);
		if (date != null)
			return date;
		date = DateUtil.yyyymmdd(time);
		dateTable.put(dateunit, date);
		return date;
	}

	public static long getDateMillis(String date) {
		Long millis = dateunitTable.get(date);
		if (millis != null)
			return millis.longValue();
		millis = DateUtil.yyyymmdd(date);
		dateunitTable.put(date, millis);
		return millis;
	}

	public static String openWriteFile(long time, int x) {
		StringBuffer sb = new StringBuffer();
		sb.append(DBCtr.getRootPath());
		sb.append("/").append(getDate(time));
		sb.append(XLogWR.dir());
		sb.append("/data");

		new File(sb.toString()).mkdirs();

		
		sb.append("/xlog." + x);
		return sb.toString();
	}

	public static String openReadFile(long time, int x) {
		StringBuffer sb = new StringBuffer();
		sb.append(DBCtr.getRootPath());
		sb.append("/").append(getDate(time));
		sb.append(XLogWR.dir());
		sb.append("/data");
		sb.append("/xlog." + x);
		return sb.toString();
	}

	public static int to24(long time) {
		long dateunit = DateUtil.getHourUnit(time);
		return (int) (dateunit % 24);
	}
}