package scouter.lang.constants;

import scouter.util.StringSet;

public class TagConstants {
	public static final String GROUP_SERVICE = "service";
	public static final String GROUP_ALERT = "alert";
	
	public static final String NAME_TOTAL = "@total";
	public static final String NAME_OBJECT = "object";
	//public static final String NAME_IP = "ip";
	public static final String NAME_SERVICE = "service";
	public static final String NAME_SERVICE_TIME_SUM = "service-time-sum";
	public static final String NAME_SERVICE_KBYTE_SUM = "service-kbyte-sum";
	public static final String NAME_SERVICE_ERROR_SUM = "service-error-sum";
	public static final String NAME_USER_AGENT = "user-agent";
	public static final String NAME_ERROR = "error";
	public static final String NAME_REFERER = "referer";
	public static final String NAME_GROUP = "group";
	
//	public static final String NAME_ELAPSED = "elapsed";
//	public static final String NAME_SQLTIME = "sqltime";
//	public static final String NAME_APITIME = "apitime";
	
	public static final String NAME_SQLTIME_SUM = "sqltime-sum";
	public static final String NAME_APITIME_SUM = "apitime-sum";
	public static final String NAME_SQL_COUNT_SUM = "sqlcount-sum";
	public static final String NAME_API_COUNT_SUM = "apicount-sum";
	
	public static final String NAME_CITY = "city";
	public static final String NAME_NATION = "nation";
//	public static final String NAME_USERID = "userid";
	public static final String NAME_LEVEL = "level";
	public static final String NAME_TITLE = "title";

	public static final String NAME_LOGIN = "login";
	public static final String NAME_DESC = "desc";

	
	
	
	public static StringSet serviceHashGroup = new StringSet();
	static{
		serviceHashGroup.put(NAME_SERVICE);
		serviceHashGroup.put(NAME_SERVICE_TIME_SUM);
		serviceHashGroup.put(NAME_SERVICE_KBYTE_SUM);
		serviceHashGroup.put(NAME_SERVICE_ERROR_SUM);
		
		serviceHashGroup.put(NAME_SQL_COUNT_SUM);
		serviceHashGroup.put(NAME_SQLTIME_SUM);
		serviceHashGroup.put(NAME_API_COUNT_SUM);
		serviceHashGroup.put(NAME_APITIME_SUM);
	}
}
