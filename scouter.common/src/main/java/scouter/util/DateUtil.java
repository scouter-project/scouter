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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

public class DateUtil {

	public static final long MILLIS_PER_SECOND = 1000;
	public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
	public static final long MILLIS_PER_FIVE_MINUTE = 5 * 60 * MILLIS_PER_SECOND;
	public static final long MILLIS_PER_TEN_MINUTE = 10 * MILLIS_PER_MINUTE;
	public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
	public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
	public static final int SECONDS_PER_DAY = (int) (MILLIS_PER_DAY / 1000);
	public final static DateTimeHelper helper = DateTimeHelper.getDefault();

	public static String datetime(long time) {
		return helper.datetime(time);
	}

	public static String timestamp(long time) {
		return helper.timestamp(time);
	}

	public static String yyyymmdd(long time) {
		return helper.yyyymmdd(time);
	}

	public static long getDateUnit() {
		return helper.getDateUnit();
	}

	public static long dateUnitToTimeMillis(long dateUnit) {
		return helper.dateUnitToTimeMillis(dateUnit);
	}

	public static long getDateUnit(long time) {
		return helper.getDateUnit(time);
	}

	public static String ymdhms(long time) {
		return helper.yyyymmdd(time) + helper.hhmmss(time);
	}

	public static String hhmmss(long time) {
		return helper.hhmmss(time);
	}

	public static String hhmm(long now) {
		return helper.hhmm(now);
	}

	public static String yyyymmdd() {
		return helper.yyyymmdd(System.currentTimeMillis());
	}

	public static String getLogTime(long time) {
		return helper.logtime(time);
	}

	public static long yyyymmdd(String date) {
		return helper.yyyymmdd(date);
	}

	public static long hhmm(String date) {
		return helper.hhmm(date);
	}

	public static long getTime(String date, String format) {
		if (format.equals("yyyyMMdd"))
			return helper.yyyymmdd(date);
		try {
			SimpleDateFormat sdf = parsers.get(format);
			if (sdf == null) {
				sdf = new SimpleDateFormat(format);
				parsers.put(format, sdf);
			}
			synchronized (sdf) {
				return sdf.parse(date).getTime();
			}
		} catch (ParseException e) {
			return 0;
		}
	}

	private static Hashtable<String, SimpleDateFormat> parsers = new Hashtable();

	public static String format(long stime, String format) {
		if (format.equals("yyyyMMdd"))
			return helper.yyyymmdd(stime);

		SimpleDateFormat sdf = parsers.get(format);
		if (sdf == null) {
			sdf = new SimpleDateFormat(format);
			parsers.put(format, sdf);
		}
		synchronized (sdf) {
			return sdf.format(new Date(stime));
		}
	}

	public static String format(long stime, String format, Locale locale) {
		if (format.equals("yyyyMMdd"))
			return helper.yyyymmdd(stime);

		SimpleDateFormat sdf = parsers.get(format + locale.getCountry());
		if (sdf == null) {
			sdf = new SimpleDateFormat(format, locale);
			parsers.put(format + locale.getCountry(), sdf);
		}
		synchronized (sdf) {
			return sdf.format(new Date(stime));
		}
	}

	public static long parse(String date, String format) {
		if (format.equals("yyyyMMdd"))
			return helper.yyyymmdd(date);

		SimpleDateFormat sdf = parsers.get(format);
		if (sdf == null) {
			sdf = new SimpleDateFormat(format);
			parsers.put(format, sdf);
		}
		synchronized (sdf) {
			try {
				return sdf.parse(date).getTime();
			} catch (ParseException e) {
				return helper.getBaseTime();
			}
		}
	}

	public static boolean isSameDay(Date date, Date date2) {
		return helper.getDateUnit(date.getTime()) == helper.getDateUnit(date2.getTime());
	}

	public static boolean isToday(long time) {
		return helper.getDateUnit(time) == helper.getDateUnit(System.currentTimeMillis());
	}

	public static int getHour(Date date) {
		return helper.getHour(date.getTime());
	}

	public static int getMin(Date date) {
		return helper.getMM(date.getTime());
	}

	public static int getHour(long time) {
		return helper.getHour(time);
	}

	public static int getMin(long time) {
		return helper.getMM(time);
	}

	public static String timestamp() {
		return helper.timestamp(System.currentTimeMillis());
	}

	public static String timestampFileName() {
		return helper.timestampFileName(System.currentTimeMillis());
	}

	public static int getDateMillis(long time) {
		return helper.getDateMillis(time);
	}

	public static long getTimeUnit(long time) {
		return helper.getTimeUnit(time);
	}

	public static long getHourUnit(long time) {
		return helper.getHourUnit(time);
	}

	public static long getTenMinUnit(long time) {
		return helper.getTenMinUnit(time);
	}

	public static long getMinUnit(long time) {
		return helper.getMinUnit(time);
	}

	public static long reverseHourUnit(long unit) {
		return helper.reverseHourUnit(unit);
	}

	public static long now() {
		return System.currentTimeMillis();
	}

	public static void main(String[] args) throws ParseException {

		String date;
		long time;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss.S");
		long cpu = SysJMX.getCurrentThreadCPUnano();
		for (int i = 0; i < 10000; i++) {
			date = sdf.format(new Date(System.currentTimeMillis() + i));
		}
		cpu = SysJMX.getCurrentThreadCPUnano() - cpu;
		System.out.println("SimpleDateFormat.format " + cpu / 1000000L + " ms");
		
		cpu = SysJMX.getCurrentThreadCPUnano();
		for (int i = 0; i < 10000; i++) {
			date = DateUtil.timestamp(System.currentTimeMillis() + i);
		}
		cpu = SysJMX.getCurrentThreadCPUnano() - cpu;
		System.out.println("DateUtil.format " + cpu / 1000000L + " ms");

		sdf = new SimpleDateFormat("yyyyMMdd");
		cpu = SysJMX.getCurrentThreadCPUnano();
		for (int i = 0; i < 10000; i++) {
			time = sdf.parse("20101123").getTime();
		}
		cpu = SysJMX.getCurrentThreadCPUnano() - cpu;
		System.out.println("SimpleDateFormat.parse " + cpu / 1000000L + " ms");

		cpu = SysJMX.getCurrentThreadCPUnano();
		for (int i = 0; i < 10000; i++) {
			time = DateUtil.yyyymmdd("20101123");
		}
		cpu = SysJMX.getCurrentThreadCPUnano() - cpu;
		System.out.println("DateUtil.parse  " + cpu / 1000000L + " ms");
	}
}