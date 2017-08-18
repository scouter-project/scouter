package scouter.server.plugin;

import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.server.core.AlertCore;
import scouter.server.db.TextRD;

/**
 * Created by gunlee on 2017. 8. 18.
 */
public class PluginHelper {
	private static final String NO_DATE = "00000000";

	private static PluginHelper instance =new PluginHelper();
	private PluginHelper() {}

	public static PluginHelper getInstance() {
		return instance;
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

