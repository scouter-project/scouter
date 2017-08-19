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
package scouter.server.plugin;

import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.server.Logger;
import scouter.server.core.AlertCore;
import scouter.server.db.TextRD;

import java.text.NumberFormat;

/**
 * Utility class for script plugin
 * Created by gunlee on 2017. 8. 18.
 * @since v1.7.3
 */
public class PluginHelper {
	private static final String NO_DATE = "00000000";

	private static PluginHelper instance =new PluginHelper();
	private PluginHelper() {}

	public static PluginHelper getInstance() {
		return instance;
	}

	public void log(Object c) {
		Logger.println(c);
	}

	public void println(Object c) {
		System.out.println(c);
	}

	public NumberFormat getNumberFormatter() {
		return getNumberFormatter(1);
	}

	public NumberFormat getNumberFormatter(int fractionDigits) {
		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(fractionDigits);
		return f;
	}

	public String formatNumber(float f) {
		return formatNumber(f, 1);
	}

	public String formatNumber(float f, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(f);
	}

	public String formatNumber(double v) {
		return formatNumber(v, 1);
	}

	public String formatNumber(double v, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(v);
	}

	public String formatNumber(int v) {
		return formatNumber(v, 1);
	}

	public String formatNumber(int v, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(v);
	}

	public String formatNumber(long v) {
		return formatNumber(v, 1);
	}

	public String formatNumber(long v, int fractionDigits) {
		return getNumberFormatter(fractionDigits).format(v);
	}

	public void alertInfo(int objHash, String objType, String title, String message) {
		alert(AlertLevel.INFO, objHash, objType, title, message);
	}
	public void alertWarn(int objHash, String objType, String title, String message) {
		alert(AlertLevel.WARN, objHash, objType, title, message);
	}
	public void alertError(int objHash, String objType, String title, String message) {
		alert(AlertLevel.ERROR, objHash, objType, title, message);
	}
	public void alertFatal(int objHash, String objType, String title, String message) {
		alert(AlertLevel.FATAL, objHash, objType, title, message);
	}

	public void alert(byte level, int objHash, String objType, String title, String message) {
		AlertPack p = new AlertPack();
		p.time = System.currentTimeMillis();
		p.level = 2;
		p.objHash = objHash;
		p.objType = objType;
		p.title = title;
		p.message = message;
		AlertCore.add(p);
	}

	public String getErrorString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getErrorString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.ERROR, hash);
	}

	public String getApicallString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getApicallString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.APICALL, hash);
	}

	public String getMethodString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getMethodString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.METHOD, hash);
	}

	public String getServiceString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getServiceString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.SERVICE, hash);
	}

	public String getSqlString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getSqlString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.SQL, hash);
	}

	public String getObjectString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getObjectString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.OBJECT, hash);
	}

	public String getRefererString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getRefererString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.REFERER, hash);
	}

	public String getUserAgentString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getUserAgentString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.USER_AGENT, hash);
	}

	public String getUserGroupString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getUserGroupString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.GROUP, hash);
	}

	public String getCityString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getCityString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.CITY, hash);
	}

	public String getLoginString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getLoginString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.LOGIN, hash);
	}

	public String getDescString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getDescString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.DESC, hash);
	}

	public String getWebString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getWebString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.WEB, hash);
	}

	public String getHashMsgString(int hash) {
		return getErrorString(NO_DATE, hash);
	}
	public String getHashMsgString(String yyyymmdd, int hash) {
		return TextRD.getString(yyyymmdd, TextTypes.HASH_MSG, hash);
	}

}

